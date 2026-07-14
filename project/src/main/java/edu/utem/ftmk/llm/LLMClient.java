package edu.utem.ftmk.llm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP/IP client interface for batch nutritional analysis.
 *
 * Correct project flow:
 * - User selects ONE model.
 * - User selects ONE OR MORE prompt techniques.
 * - Server processes ALL transcripts stored in the transcript table / transcript files.
 * - Client receives live status until DONE.
 */
public class LLMClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== MASAKGRAMPROMPT BATCH LLM CLIENT =====");
        System.out.println("This client does NOT ask for Transcript ID.");
        System.out.println("The server will process ALL transcripts found in the database/file paths.");
        System.out.println();

        System.out.print("Model ID: ");
        int modelId = readInt(scanner);

        System.out.print("Technique ID(s) e.g. 1 or 1,2,3 or all: ");
        String techniqueIds = scanner.nextLine().trim();
        if (techniqueIds.isEmpty()) {
            techniqueIds = "all";
        }

        System.out.print("RAG enabled? (y/n): ");
        boolean ragEnabled = scanner.nextLine().trim().equalsIgnoreCase("y");

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            output.writeInt(modelId);
            output.writeUTF(techniqueIds);
            output.writeBoolean(ragEnabled);
            output.flush();

            System.out.println();
            System.out.println("===== LIVE SERVER STATUS =====");

            while (true) {
                String response = input.readUTF();

                if ("DONE".equals(response)) {
                    break;
                }

                System.out.println(response);
            }

            System.out.println("===== CLIENT FINISHED =====");

        } catch (EOFException e) {
            System.out.println("Client error: server closed the connection before sending DONE.");
            System.out.println("Check the LLMServer console for the full error.");
        } catch (IOException e) {
            System.out.println("Client error type: " + e.getClass().getName());
            System.out.println("Client error message: " + e.getMessage());
            System.out.println("Make sure LLMServer is running first on port " + SERVER_PORT + ".");
        }
    }

    private static int readInt(Scanner scanner) {
        while (true) {
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }
}
