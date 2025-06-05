package com.gamedev.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class GameClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            // Thread to listen for server messages
            Thread listener = new Thread(() -> {
                String serverMsg;
                try {
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            listener.setDaemon(true);
            listener.start();

            System.out.println("Connected! Use commands: UP, DOWN, LEFT, RIGHT");
            System.out.println("Type EXIT to quit.");
            System.out.println("--- Simple Console Game ---");
            System.out.println("You are a player in a shared 2D grid. Move with UP, DOWN, LEFT, RIGHT.");
            System.out.println("Your position will be shown by the server after each move.");
            while (true) {
                String command = scanner.nextLine().trim().toUpperCase();
                if (command.equals("EXIT")) break;
                if (command.matches("UP|DOWN|LEFT|RIGHT")) {
                    out.println(command);
                } else {
                    System.out.println("Invalid command. Use: UP, DOWN, LEFT, RIGHT, or EXIT");
                }
            }
        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
}
