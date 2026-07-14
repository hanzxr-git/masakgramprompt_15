package edu.utem.ftmk.llm;

import Database.ExperimentDAO;
import Database.NutritionResultDAO;
import Database.TranscriptDAO;
import model.LLMModel;
import model.NutritionResult;
import model.PromptTechnique;
import model.Transcript;
import util.JsonParserUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles one batch analysis request.
 *
 * Correct project flow based on the specification:
 * 1. Receive selected model_id.
 * 2. Receive one or more selected prompt technique_id values.
 * 3. Read ALL transcript records from the transcript table.
 * 4. For every transcript + selected technique, create/update experiment row.
 * 5. Read transcript text from transcript_text column or from the transcript file_path.
 * 6. Send transcript + prompt to the selected LLM model.
 * 7. Parse JSON and save nutrition_result + ingredient_result attributes.
 * 8. Report live status and total elapsed time back to the client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TranscriptDAO transcriptDAO;
    private final ExperimentDAO experimentDAO;
    private final NutritionResultDAO nutritionResultDAO;
    private final LLMService llmService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.transcriptDAO = new TranscriptDAO();
        this.experimentDAO = new ExperimentDAO();
        this.nutritionResultDAO = new NutritionResultDAO();
        this.llmService = new LLMService();
    }

    @Override
    public void run() {
        DataInputStream input = null;
        DataOutputStream output = null;

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        long batchStart = System.currentTimeMillis();

        try {
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            int modelId = input.readInt();
            String techniqueSelection = input.readUTF();
            boolean ragEnabled = input.readBoolean();

            send(output, "Request received.");
            send(output, "Model ID selected: " + modelId);
            send(output, "Technique selection: " + techniqueSelection);
            send(output, "RAG enabled: " + ragEnabled);

            LLMModel llmModel = experimentDAO.findModelById(modelId);
            if (llmModel == null) {
                throw new IllegalArgumentException("No llm_model found for model_id " + modelId);
            }

            List<PromptTechnique> selectedTechniques = resolveSelectedTechniques(techniqueSelection);
            if (selectedTechniques.isEmpty()) {
                throw new IllegalArgumentException("No valid prompt techniques selected.");
            }

            List<Transcript> transcripts = transcriptDAO.findAll();
            if (transcripts.isEmpty()) {
                throw new IllegalArgumentException("No transcript records found in transcript table.");
            }

            send(output, "Model selected: " + llmModel.getModelName() + " (" + llmModel.getModelTag() + ")");
            send(output, "Total transcript records found: " + transcripts.size());
            send(output, "Total prompt techniques selected: " + selectedTechniques.size());
            send(output, "Total experiment runs to process: " + (transcripts.size() * selectedTechniques.size()));
            send(output, "--------------------------------------------------");

            for (PromptTechnique technique : selectedTechniques) {
                long techniqueStart = System.currentTimeMillis();
                send(output, "[PENDING] Condition: model=" + llmModel.getModelName()
                        + ", technique=" + technique.getTechniqueName());

                String systemPrompt = loadPromptFile(technique.getSystemPromptFile());
                String userPromptTemplate = loadPromptFile(technique.getUserPromptFile());

                int techniqueSuccess = 0;
                int techniqueFailed = 0;
                int techniqueSkipped = 0;

                for (Transcript transcript : transcripts) {
                    Integer experimentId = null;
                    long transcriptStart = System.currentTimeMillis();

                    try {
                        send(output, "[RUNNING] transcript_id=" + transcript.getTranscriptId()
                                + ", file=" + nullToDash(transcript.getFileName())
                                + ", technique=" + technique.getTechniqueName());

                        experimentId = experimentDAO.getOrCreateExperiment(
                                transcript.getTranscriptId(),
                                modelId,
                                technique.getTechniqueId(),
                                ragEnabled
                        );

                        experimentDAO.updateStatus(experimentId, "running");

                        String transcriptText = transcriptDAO.getTranscriptTextById(transcript.getTranscriptId());
                        if (isBlank(transcriptText)) {
                            skippedCount++;
                            techniqueSkipped++;
                            experimentDAO.updateStatus(experimentId, "failed");
                            send(output, "[SKIPPED] transcript_id=" + transcript.getTranscriptId()
                                    + " because transcript text/file cannot be read: "
                                    + nullToDash(transcript.getFilePath()));
                            continue;
                        }

                        String rawResponse = llmService.analyzeTranscript(
                                llmModel.getModelTag(),
                                transcriptText,
                                systemPrompt,
                                userPromptTemplate
                        );
                        
                        String latestStatus = experimentDAO.getExperimentStatus(experimentId);

                        if ("failed".equalsIgnoreCase(latestStatus)) {
                            System.out.println("Experiment " + experimentId + " was stopped by user. Skipping save.");
                            continue;
                        }

                        NutritionResult nutritionResult = JsonParserUtil.parseNutritionResult(rawResponse, experimentId);
                        int resultId = nutritionResultDAO.save(nutritionResult);

                        experimentDAO.updateStatus(experimentId, "completed");

                        successCount++;
                        techniqueSuccess++;

                        long transcriptElapsed = System.currentTimeMillis() - transcriptStart;
                        send(output, "[COMPLETED] transcript_id=" + transcript.getTranscriptId()
                                + ", experiment_id=" + experimentId
                                + ", result_id=" + resultId
                                + ", recipe=" + nullToDash(nutritionResult.getRecipeName())
                                + ", ingredients=" + nutritionResult.getIngredientResults().size()
                                + ", time=" + formatDuration(transcriptElapsed));

                    } catch (Exception transcriptError) {
                        failedCount++;
                        techniqueFailed++;

                        if (experimentId != null) {
                            try {
                                experimentDAO.updateStatus(experimentId, "failed");
                            } catch (SQLException ignored) {
                            }
                        }

                        send(output, "[FAILED] transcript_id=" + transcript.getTranscriptId()
                                + " - " + transcriptError.getClass().getSimpleName()
                                + ": " + nullToDash(transcriptError.getMessage()));
                        transcriptError.printStackTrace();
                    }
                }

                long techniqueElapsed = System.currentTimeMillis() - techniqueStart;
                send(output, "[CONDITION COMPLETED] technique=" + technique.getTechniqueName()
                        + ", success=" + techniqueSuccess
                        + ", failed=" + techniqueFailed
                        + ", skipped=" + techniqueSkipped
                        + ", time=" + formatDuration(techniqueElapsed));
                send(output, "--------------------------------------------------");
            }

            long totalElapsed = System.currentTimeMillis() - batchStart;
            send(output, "===== BATCH ANALYSIS SUMMARY =====");
            send(output, "Model: " + llmModel.getModelName());
            send(output, "Selected techniques: " + selectedTechniques.size());
            send(output, "Transcripts found: " + transcripts.size());
            send(output, "Successful runs: " + successCount);
            send(output, "Failed runs: " + failedCount);
            send(output, "Skipped runs: " + skippedCount);
            send(output, "Total elapsed time: " + formatDuration(totalElapsed));
            send(output, "Saved to DB: experiment + nutrition_result + ingredient_result");

        } catch (Exception e) {
            send(output, "[FATAL ERROR] " + e.getClass().getSimpleName() + ": " + nullToDash(e.getMessage()));
            e.printStackTrace();
        } finally {
            send(output, "DONE");
            closeQuietly(input);
            closeQuietly(output);

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private List<PromptTechnique> resolveSelectedTechniques(String selection) throws SQLException {
        if (isBlank(selection) || selection.trim().equalsIgnoreCase("all")) {
            return experimentDAO.findAllPromptTechniques();
        }

        Set<Integer> ids = new LinkedHashSet<>();
        String[] parts = selection.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ids.add(Integer.parseInt(trimmed));
        }

        List<PromptTechnique> techniques = new ArrayList<>();
        for (Integer id : ids) {
            PromptTechnique technique = experimentDAO.findPromptTechniqueById(id);
            if (technique == null) {
                throw new IllegalArgumentException("No prompt_technique found for technique_id " + id);
            }
            techniques.add(technique);
        }
        return techniques;
    }

   private String loadPromptFile(String promptPath) {
    if (isBlank(promptPath)) {
        throw new IllegalArgumentException("Prompt path is empty in prompt_technique table.");
    }

    promptPath = promptPath.trim().replace("\\", "/");

    System.out.println("Current working directory: " + System.getProperty("user.dir"));
    System.out.println("Trying to load prompt path: " + promptPath);

    // 1. Try exact relative path, example: prompts/zero_shot_system.txt
    Path exactPath = Paths.get(promptPath);
    String exact = readPathIfExists(exactPath);
    if (exact != null) {
        System.out.println("Prompt loaded from exact path: " + exactPath.toAbsolutePath());
        return exact;
    }

    // 2. Try Maven resources path while running in Eclipse
    Path resourcesPath = Paths.get("src", "main", "resources", promptPath);
    String fromResourcesFolder = readPathIfExists(resourcesPath);
    if (fromResourcesFolder != null) {
        System.out.println("Prompt loaded from resources folder: " + resourcesPath.toAbsolutePath());
        return fromResourcesFolder;
    }

    // 3. Try compiled classpath, usually target/classes/prompts/...
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(promptPath)) {
        if (stream != null) {
            byte[] bytes = stream.readAllBytes();
            System.out.println("Prompt loaded from classpath: " + promptPath);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    } catch (IOException e) {
        throw new RuntimeException("Error reading prompt from classpath: " + promptPath, e);
    }

    throw new IllegalArgumentException(
            "Prompt file not found: " + promptPath
            + ". Expected location: src/main/resources/" + promptPath
            + ". Current working directory: " + System.getProperty("user.dir")
    );
}

    private String readPathIfExists(Path path) {
        try {
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void send(DataOutputStream output, String message) {
        if (output == null) {
            return;
        }

        try {
            output.writeUTF(message == null ? "" : message);
            output.flush();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(DataOutputStream stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String nullToDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        long remainingMillis = millis % 1000;

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        if (seconds > 0) {
            return seconds + "s " + remainingMillis + "ms";
        }
        return millis + "ms";
    }
}
