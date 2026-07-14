package edu.utem.ftmk.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

public class LLMService {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    public static final String LLAMA = "llama3.2:3b";
    public static final String PHI = "phi4-mini:latest";
    public static final String QWEN = "qwen2.5:3b";
    public static final String SEALION = "aisingapore/Gemma-SEA-LION-v4-4B-VL";
    public static final String MEDGEMMA = "medgemma:4b";

    public ChatModel buildModel(String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .modelName(modelName)
                .timeout(Duration.ofHours(1))
                .build();
    }

    public String prompt(String modelName, String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt text cannot be null or blank.");
        }

        ChatModel model = buildModel(modelName);
        return model.chat(userPrompt);
    }

    public String analyzeTranscript(
            String modelName,
            String transcriptText,
            String systemPrompt,
            String userPromptTemplate
    ) {
        if (transcriptText == null || transcriptText.trim().isEmpty()) {
            throw new IllegalArgumentException("Transcript text cannot be null or blank.");
        }

        StringBuilder finalPrompt = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            finalPrompt.append(systemPrompt.trim()).append("\n\n");
        }

        if (userPromptTemplate != null && !userPromptTemplate.trim().isEmpty()) {
            finalPrompt.append(userPromptTemplate.replace("{{TRANSCRIPT}}", transcriptText));
        } else {
            finalPrompt.append("Analyze this cooking transcript and return valid JSON only.\n\n");
            finalPrompt.append("Transcript:\n");
            finalPrompt.append(transcriptText);
        }

        return prompt(modelName, finalPrompt.toString());
    }
}