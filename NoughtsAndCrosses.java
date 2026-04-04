import swiftbot.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class NoughtsAndCrosses {
    static SwiftBotAPI swiftBot;
    static final String LOG_FILE = "noughts_and_crosses_log.txt";
    static Scanner scanner = new Scanner(System.in);
    static Random random = new Random();

    // Hardware Button Flags
    static volatile boolean buttonAPressed = false;
    static volatile boolean buttonXPressed = false;
    static volatile boolean buttonYPressed = false;

    // Colors for underlights
    static final int[] GREEN = {0, 255, 0};
    static final int[] RED = {255, 0, 0};
    static final int[] BLUE = {0, 0, 255};

    public static void main(String[] args) {
        // 1. Initialize SwiftBot
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
        } catch (Exception e) {
            System.out.println("\nI2C disabled or Connection Lost!");
            System.exit(5);
        }

        // --- UI: MAIN MENU ---
        System.out.println("===========================================");
        System.out.println("SWIFTBOT: NOUGHTS & CROSSES");
        System.out.println("===========================================");
        System.out.println("Welcome! Ready to play?");
        System.out.println("-------------------------------------------");
        System.out.println("[ GAME RULES ]");
        System.out.println("1. You are playing against the SwiftBot AI.");
        System.out.println("2. Get three symbols in a row to win.");
        System.out.println("3. The robot will physically move to show its turn.");
        System.out.println("\nPRESS BUTTON 'A' TO START GAME");
        System.out.println("===========================================");
        System.out.println("(Waiting for you to press the button...)");

        // Wait for Button A
        try {
            swiftBot.enableButton(Button.A, () -> {
                buttonAPressed = true;
                swiftBot.disableButton(Button.A);
            });
            while (!buttonAPressed) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- UI: GAME SETUP ---
        System.out.println("\n===============================");
        System.out.println("GAME SETUP");
        System.out.println("===============================");
        System.out.print("Hi there! Please enter your name:\n> ");
        String userName = scanner.nextLine();
        String botName = "SwiftBot";
        System.out.println("\nNice to meet you, " + userName + "!");

        int userScore = 0;
        int botScore = 0;
        int draws = 0; // Added variable to track draws for the UI scoreboard
        int roundNumber = 1;
        boolean playing = true;

        // 3. Main Game Loop
        while (playing) {
            char[][] board = {
                    {' ', ' ', ' '},
                    {' ', ' ', ' '},
                    {' ', ' ', ' '}
            };
            ArrayList<String> movesLog = new ArrayList<>();

            System.out.println("\nWHO GOES FIRST? ROLLING DICE...");

            // Dice Roll
            int userRoll = 0, botRoll = 0;
            while (userRoll == botRoll) {
                userRoll = random.nextInt(6) + 1;
                botRoll = random.nextInt(6) + 1;

                System.out.println("[ YOU ]\t\t[ SWIFTBOT ]");
                System.out.println("   " + userRoll + "   \tVS\t   " + botRoll);

                if (userRoll == botRoll) {
                    System.out.println("Tie! Re-rolling...\n");
                    sleep(1000);
                }
            }

            String player1, player2;
            char p1Piece = 'O', p2Piece = 'X';

            if (userRoll > botRoll) {
                System.out.println(">> YOU WIN THE ROLL!");
                System.out.println(">> You are playing as 'O' (Green Pieces)");
                player1 = userName; player2 = botName;
            } else {
                System.out.println(">> SWIFTBOT WINS THE ROLL!");
                System.out.println(">> You are playing as 'X' (Red Pieces)");
                player1 = botName; player2 = userName;
            }

            System.out.println("\n[ PRESS ENTER TO START ROUND " + roundNumber + " ]");
            scanner.nextLine();

            boolean gameOver = false;
            int currentTurn = 1;
            String winner = null;
            String winType = null;
            int winIndex = -1;

            // Round Loop
            while (!gameOver) {
                System.out.println("\n===========================================");
                System.out.println("ROUND: " + roundNumber + " | " + userName + " vs " + botName);

                String activePlayer = (currentTurn % 2 != 0) ? player1 : player2;
                char activePiece = (currentTurn % 2 != 0) ? p1Piece : p2Piece;

                if(activePlayer.equals(userName)) {
                    System.out.println("IT IS YOUR TURN, " + userName.toUpperCase() + " (" + activePiece + ")");
                } else {
                    System.out.println("SWIFTBOT'S TURN (" + activePiece + ")");
                }
                System.out.println("===========================================");
                printBoard(board);

                // --- USER TURN ---
                if (activePlayer.equals(userName)) {
                    boolean validMove = false;
                    while (!validMove) {
                        try {
                            System.out.print("Where would you like to move? (row, col)\n> ");
                            String input = scanner.nextLine().trim();

                            // Split input by comma based on UI design
                            String[] parts = input.split(",");
                            if (parts.length != 2) {
                                throw new NumberFormatException();
                            }

                            int r = Integer.parseInt(parts[0].trim()) - 1;
                            int c = Integer.parseInt(parts[1].trim()) - 1;

                            if (r < 0 || r > 2 || c < 0 || c > 2) {
                                printErrorMsg();
                            } else if (board[r][c] != ' ') {
                                printErrorMsg();
                            } else {
                                board[r][c] = activePiece;
                                movesLog.add(userName + " (" + activePiece + ") -> [" + (r + 1) + "," + (c + 1) + "]");
                                validMove = true;
                            }
                        } catch (NumberFormatException e) {
                            printErrorMsg();
                        }
                    }
                }
                // --- SWIFTBOT TURN ---
                else {
                    System.out.println("SwiftBot is thinking about its move...");
                    sleep(500);
                    System.out.println("... Awaiting robot decision...");
                    sleep(500);

                    int r, c;
                    do {
                        r = random.nextInt(3);
                        c = random.nextInt(3);
                    } while (board[r][c] != ' ');

                    System.out.println(">> TARGET FOUND!");
                    System.out.println(">> SwiftBot is moving to Row " + (r + 1) + ", Column " + (c + 1));
                    System.out.println("(Watch the robot move!)");

                    // Hardware Movements
                    checkPathClear();
                    moveToSquare(r + 1, c + 1);

                    System.out.println(">> Blinking lights...");
                    blinkLights(GREEN, 3);

                    System.out.println(">> Returning to start...");
                    returnToStart();

                    System.out.println("[ PLEASE WAIT FOR ROBOT TO STOP MOVING ]");

                    board[r][c] = activePiece;
                    movesLog.add(botName + " (" + activePiece + ") -> [" + (r + 1) + "," + (c + 1) + "]");
                }

                // --- CHECK WIN/DRAW ---
                if (checkWin(board, activePiece)) {
                    printBoard(board);
                    winner = activePlayer;

                    if (winner.equals(userName)) userScore++;
                    else botScore++;

                    int[] winColor = (activePiece == 'O') ? GREEN : RED;
                    blinkLights(winColor, 3);
                    traceWinningLine();
                    blinkLights(winColor, 3);
                    gameOver = true;
                } else if (checkDraw(board)) {
                    printBoard(board);
                    winner = "Draw";
                    draws++; // Increment draw counter
                    spin360();
                    blinkLights(BLUE, 3);
                    gameOver = true;
                }
                currentTurn++;
            }

            // Post-Round Logistics
            logGameData(roundNumber, userName, movesLog, winner);

            // --- UI: GAME OVER SCREEN ---
            System.out.println("\n===========================================");
            System.out.println("ROUND " + roundNumber + " COMPLETE!");
            if (winner.equals("Draw")) {
                System.out.println("IT'S A DRAW!");
            } else if (winner.equals(userName)) {
                System.out.println("CONGRATULATIONS, " + userName.toUpperCase() + "! YOU WON!");
            } else {
                System.out.println("SWIFTBOT WINS THIS ROUND!");
            }

            System.out.println("\nTOTAL SCOREBOARD");
            System.out.println("PLAYER      | WINS | LOSS | DRAW");
            // Tabular formatting based on UI Mockup
            System.out.printf("%-11s |  %d   |  %d   |  %d\n", userName, userScore, botScore, draws);
            System.out.printf("%-11s |  %d   |  %d   |  %d\n", botName, botScore, userScore, draws);
            System.out.println("-------------------------------------------");
            System.out.println("[ Game saved to log file ]");
            System.out.println("> PRESS BUTTON 'Y' to Play Another Round");
            System.out.println("> PRESS BUTTON 'X' to Quit Game");
            System.out.println("===========================================\n");

            buttonXPressed = false;
            buttonYPressed = false;

            try {
                swiftBot.enableButton(Button.Y, () -> { buttonYPressed = true; });
                swiftBot.enableButton(Button.X, () -> { buttonXPressed = true; });

                while (!buttonYPressed && !buttonXPressed) {
                    Thread.sleep(100);
                }

                swiftBot.disableAllButtons();

                if (buttonXPressed) {
                    System.out.println("Exiting game. Thanks for playing!");
                    playing = false;
                } else {
                    roundNumber++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
        System.exit(0);
    }

    // ==========================================
    // UI HELPER METHODS
    // ==========================================

    public static void printErrorMsg() {
        System.out.println("\nOops! That move isn't possible.");
        System.out.println("Here is how to fix it:");
        System.out.println("* Use a Row number between 1 and 3.");
        System.out.println("* Use a Column number between 1 and 3.");
        System.out.println("* Make sure the square isn't already taken!");
        System.out.println("* Format your answer as row,col (e.g. 2,3)");
        System.out.println("Let's try that again:\n");
    }

    public static void printBoard(char[][] board) {
        System.out.println("\n         Col 1   Col 2   Col 3");
        for (int r = 0; r < 3; r++) {
            System.out.printf("Row %d      %c   |   %c   |   %c  \n", (r+1), board[r][0], board[r][1], board[r][2]);
            if (r < 2) System.out.println("        -------+-------+-------");
        }
        System.out.println();
    }

    // ==========================================
    // HARDWARE METHODS (Require Your Calibration)
    // ==========================================

    public static void blinkLights(int[] color, int times) {
        try {
            for (int i = 0; i < times; i++) {
                swiftBot.fillUnderlights(color);
                Thread.sleep(500);
                swiftBot.disableUnderlights();
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void spin360() {
        // TODO: Calibrate speeds to achieve a perfect 360 spin!
        // swiftBot.move(100, -100, 1500);
    }

    public static void moveToSquare(int row, int col) {
        // TODO: Implement calculation to drive to a 25x25cm square.
        // swiftBot.move(100, 100, 2000); // Example forward movement
        sleep(1000);
    }

    public static void returnToStart() {
        // TODO: Implement reverse navigation
        sleep(1000);
    }

    public static void traceWinningLine() {
        // TODO: Implement physical line tracing
        sleep(2000);
    }

    // ==========================================
    // LOGIC & UTILITY METHODS
    // ==========================================

    public static boolean checkWin(char[][] board, char piece) {
        // Rows & Cols
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == piece && board[i][1] == piece && board[i][2] == piece) return true;
            if (board[0][i] == piece && board[1][i] == piece && board[2][i] == piece) return true;
        }
        // Diagonals
        if (board[0][0] == piece && board[1][1] == piece && board[2][2] == piece) return true;
        if (board[0][2] == piece && board[1][1] == piece && board[2][0] == piece) return true;
        return false;
    }

    public static boolean checkDraw(char[][] board) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == ' ') return false;
            }
        }
        return true;
    }

    public static void logGameData(int round, String user, ArrayList<String> moves, String winner) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            out.println("--- Round " + round + " ---");
            out.println("Date/Time: " + dtf.format(now));
            out.println("Players: " + user + " vs SwiftBot");
            out.println("Moves:");
            for (String move : moves) out.println("  " + move);
            out.println("Outcome: " + (winner.equals("Draw") ? "Draw" : "Win"));
            if (!winner.equals("Draw")) out.println("Winner: " + winner);
            out.println();
        } catch (Exception e) {
            System.out.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void checkPathClear() {
        boolean pathClear = false;

        while (!pathClear) {
            try {
                // Take a reading from the Ultrasound sensor
                double distance = swiftBot.useUltrasound();

                // If an object is closer than 20cm, pause and warn the user
                if (distance < 20.0) {
                    System.out.println("\n[WARNING] Obstacle detected " + String.format("%.1f", distance) + "cm away!");
                    System.out.println("Waiting for the user to clear the board...");

                    // Flash red lights to indicate it is blocked
                    swiftBot.fillUnderlights(RED);
                    sleep(500);
                    swiftBot.disableUnderlights();
                    sleep(500);
                } else {
                    // Path is clear!
                    pathClear = true;
                }
            } catch (Exception e) {
                // If sensor fails, we don't want to freeze the game
                pathClear = true;
            }
        }
    }
}