package edu.utem.ftmk.llm;

import Database.DBConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP/IP server for nutritional LLM analysis.
 */
public class LLMServer {

    public static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        if (DBConnection.testConnection()) {
            System.out.println("Database Connected!");
        } else {
            System.out.println("WARNING: Database connection failed. Fix DBConnection.java before running analysis.");
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("Waiting for client...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client Connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
