package com.gamedev.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class GameClient {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private Scanner scanner = new Scanner(System.in);
    
    // Game state
    private Map<Integer, Player> players = new HashMap<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private int myPlayerId = -1;
    
    public static class Player {
        public int id, x, y, health, score;
        public boolean alive;
        
        public Player(int id, int x, int y, int health, int score, boolean alive) {
            this.id = id; this.x = x; this.y = y; 
            this.health = health; this.score = score; this.alive = alive;
        }
    }
    
    public static class Enemy {
        public int id, x, y, health;
        
        public Enemy(int id, int x, int y, int health) {
            this.id = id; this.x = x; this.y = y; this.health = health;
        }
    }
    
    public static class Projectile {
        public int id, x, y, playerId;
        
        public Projectile(int id, int x, int y, int playerId) {
            this.id = id; this.x = x; this.y = y; this.playerId = playerId;
        }
    }

    public void start() {
        try (Socket socket = new Socket(HOST, PORT)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            // Start message listener thread
            Thread listener = new Thread(this::messageListener);
            listener.setDaemon(true);
            listener.start();

            displayWelcome();
            gameLoop();

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
    
    private void displayWelcome() {
        clearScreen();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    VAMPIRE SURVIVORS CLONE                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Controls:                                                   ║");
        System.out.println("║  WASD - Move your character                                  ║");
        System.out.println("║  IJKL - Shoot projectiles (Up/Down/Left/Right)              ║");
        System.out.println("║  Q - Quit game                                               ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Survive against waves of enemies!                          ║");
        System.out.println("║  Kill enemies to gain points!                               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("\nConnecting to game server...");
    }

    private void messageListener() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("\nDisconnected from server.");
                connected = false;
            }
        }
    }
    
    private void processServerMessage(String message) {
        if (message.startsWith("WELCOME:")) {
            String welcomeMsg = message.substring(8);
            System.out.println("\n" + welcomeMsg);
            // Extract player ID
            if (welcomeMsg.contains("Player ")) {
                String[] parts = welcomeMsg.split("Player ");
                if (parts.length > 1) {
                    myPlayerId = Integer.parseInt(parts[1].trim());
                }
            }
        } else if (message.startsWith("INFO:")) {
            System.out.println(message.substring(5));
        } else if (message.startsWith("GAMESTATE:")) {
            parseGameState(message.substring(10));
            displayGame();
        }
    }
    
    private void parseGameState(String gameState) {
        players.clear();
        enemies.clear();
        projectiles.clear();
        
        if (gameState.isEmpty()) return;
        
        String[] entities = gameState.split("\\|");
        for (String entity : entities) {
            if (entity.isEmpty()) continue;
            
            String[] parts = entity.split(":");
            if (parts.length < 2) continue;
            
            switch (parts[0]) {
                case "PLAYER":
                    if (parts.length >= 7) {
                        int id = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int health = Integer.parseInt(parts[4]);
                        int score = Integer.parseInt(parts[5]);
                        boolean alive = Boolean.parseBoolean(parts[6]);
                        players.put(id, new Player(id, x, y, health, score, alive));
                    }
                    break;
                case "ENEMY":
                    if (parts.length >= 5) {
                        int id = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int health = Integer.parseInt(parts[4]);
                        enemies.add(new Enemy(id, x, y, health));
                    }
                    break;
                case "PROJECTILE":
                    if (parts.length >= 5) {
                        int id = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int playerId = Integer.parseInt(parts[4]);
                        projectiles.add(new Projectile(id, x, y, playerId));
                    }
                    break;
            }
        }
    }
    
    private void displayGame() {
        clearScreen();
        
        // Game area dimensions
        final int WIDTH = 60;
        final int HEIGHT = 25;
        final int OFFSET_X = 50;
        final int OFFSET_Y = 50;
        
        char[][] grid = new char[HEIGHT][WIDTH];
        
        // Initialize grid with spaces
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                grid[i][j] = ' ';
            }
        }
        
        // Place entities on grid
        for (Player player : players.values()) {
            int screenX = (player.x + OFFSET_X) * WIDTH / 100;
            int screenY = (player.y + OFFSET_Y) * HEIGHT / 100;
            if (screenX >= 0 && screenX < WIDTH && screenY >= 0 && screenY < HEIGHT) {
                if (player.alive) {
                    grid[screenY][screenX] = (player.id == myPlayerId) ? '@' : (char)('0' + player.id);
                } else {
                    grid[screenY][screenX] = 'X';
                }
            }
        }
        
        for (Enemy enemy : enemies) {
            int screenX = (enemy.x + OFFSET_X) * WIDTH / 100;
            int screenY = (enemy.y + OFFSET_Y) * HEIGHT / 100;
            if (screenX >= 0 && screenX < WIDTH && screenY >= 0 && screenY < HEIGHT) {
                grid[screenY][screenX] = 'E';
            }
        }
        
        for (Projectile projectile : projectiles) {
            int screenX = (projectile.x + OFFSET_X) * WIDTH / 100;
            int screenY = (projectile.y + OFFSET_Y) * HEIGHT / 100;
            if (screenX >= 0 && screenX < WIDTH && screenY >= 0 && screenY < HEIGHT) {
                grid[screenY][screenX] = '*';
            }
        }
        
        // Display grid
        System.out.println("╔" + "═".repeat(WIDTH) + "╗");
        for (int i = 0; i < HEIGHT; i++) {
            System.out.print("║");
            for (int j = 0; j < WIDTH; j++) {
                System.out.print(grid[i][j]);
            }
            System.out.println("║");
        }
        System.out.println("╚" + "═".repeat(WIDTH) + "╝");
        
        // Display player stats
        Player myPlayer = players.get(myPlayerId);
        if (myPlayer != null) {
            System.out.printf("Player %d | Health: %d | Score: %d | Status: %s%n",
                myPlayer.id, myPlayer.health, myPlayer.score, 
                myPlayer.alive ? "ALIVE" : "DEAD");
        }
        
        System.out.printf("Enemies: %d | Projectiles: %d | Players: %d%n", 
            enemies.size(), projectiles.size(), players.size());
        
        System.out.println("\nLegend: @ = You, 1-4 = Other Players, E = Enemy, * = Projectile, X = Dead");
        System.out.print("Command (WASD to move, IJKL to shoot, Q to quit): ");
    }

    private void gameLoop() {
        while (connected) {
            String command = scanner.nextLine().trim().toUpperCase();
            
            if (command.isEmpty()) continue;
            
            if (command.equals("Q") || command.equals("QUIT")) {
                break;
            }
            
            // Send command to server
            out.println(command);
        }
    }
    
    private void clearScreen() {
        // Clear screen for Windows
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Exception e) {
            // Fallback: print newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        new GameClient().start();
    }
}
