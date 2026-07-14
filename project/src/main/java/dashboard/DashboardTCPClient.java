package dashboard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class DashboardTCPClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private volatile boolean stopRequested = false;
    private volatile Socket activeSocket;

    public String runAllExperiments(String modelTag, List<String> techniques) {
        return runAllExperiments(modelTag, techniques, null);
    }

    public String runAllExperiments(String modelTag, List<String> techniques, Consumer<String> onMessage) {
        stopRequested = false;

        int modelId = mapModelTagToId(modelTag);
        String techniqueIds = mapTechniqueNamesToIds(techniques);
        boolean ragEnabled = false;

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream())
                );
                DataInputStream input = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream())
                )
        ) {
            activeSocket = socket;

            sendMessage(onMessage, "Connected to TCP/IP server.");
            sendMessage(onMessage, "Selected Model ID: " + modelId);
            sendMessage(onMessage, "Selected Technique ID(s): " + techniqueIds);
            sendMessage(onMessage, "Starting analysis...");

            output.writeInt(modelId);
            output.writeUTF(techniqueIds);
            output.writeBoolean(ragEnabled);
            output.flush();

            StringBuilder response = new StringBuilder();

            while (!stopRequested) {
                String line = input.readUTF();

                response.append(line).append("\n");
                sendMessage(onMessage, line);

                if ("DONE".equalsIgnoreCase(line.trim())) {
                    break;
                }
            }

            if (stopRequested) {
                sendMessage(onMessage, "STOPPED BY USER.");
                return "STOPPED BY USER.";
            }

            return response.toString();

        } catch (IOException e) {
            if (stopRequested) {
                sendMessage(onMessage, "STOPPED BY USER.");
                return "STOPPED BY USER.";
            }

            String error = "ERROR: Cannot connect to TCP/IP server.\n" + e.getMessage();
            sendMessage(onMessage, error);
            return error;

        } catch (IllegalArgumentException e) {
            String error = "ERROR: " + e.getMessage();
            sendMessage(onMessage, error);
            return error;

        } finally {
            activeSocket = null;
        }
    }

    public void stopCurrentRun() {
        stopRequested = true;

        try {
            if (activeSocket != null && !activeSocket.isClosed()) {
                activeSocket.close();
            }
        } catch (IOException e) {
            // Stop requested by user, ignore close error.
        }
    }

    private void sendMessage(Consumer<String> onMessage, String message) {
        if (onMessage != null) {
            onMessage.accept(message);
        }
    }

    private int mapModelTagToId(String modelTag) {
        if ("llama3.2:3b".equals(modelTag)) {
            return 1;
        }

        if ("phi4-mini".equals(modelTag)) {
            return 2;
        }

        if ("qwen2.5:3b".equals(modelTag)) {
            return 3;
        }

        if ("aisingapore/Gemma-SEA-LION-v4-4B-VL".equals(modelTag)) {
            return 4;
        }

        if ("medgemma:4b".equals(modelTag)) {
            return 5;
        }

        throw new IllegalArgumentException("Unknown model tag: " + modelTag);
    }

    private String mapTechniqueNamesToIds(List<String> techniques) {
        StringBuilder ids = new StringBuilder();

        for (String technique : techniques) {
            int id = mapTechniqueNameToId(technique);

            if (ids.length() > 0) {
                ids.append(",");
            }

            ids.append(id);
        }

        return ids.toString();
    }

    private int mapTechniqueNameToId(String techniqueName) {
        if ("zero-shot".equals(techniqueName)) {
            return 1;
        }

        if ("few-shot".equals(techniqueName)) {
            return 2;
        }

        if ("chain-of-thought".equals(techniqueName)) {
            return 3;
        }

        if ("structured-output".equals(techniqueName)) {
            return 4;
        }

        throw new IllegalArgumentException("Unknown prompt technique: " + techniqueName);
    }
}