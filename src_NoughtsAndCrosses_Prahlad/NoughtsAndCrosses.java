package src_NoughtsAndCrosses_Prahlad;

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
    static final Scanner scanner = new Scanner(System.in);
    static final Random random = new Random();

    // --- Color Codes ---
    static final String RESET = "\u001B[0m";
    static final String CYAN = "\u001B[96m";    // Bright Cyan
    static final String YELLOW = "\u001B[93m";  // Bright Yellow
    static final String GREEN = "\u001B[92m";   // Bright Green
    static final String RED = "\u001B[91m";     // Bright Red
    static final String BLUE = "\u001B[94m";    // Bright Blue
    static final String MAGENTA = "\u001B[95m"; // Bright Magenta
    static final String GRAY = "\u001B[90m";    // Dark Gray for board grid
    static final String BOLD = "\u001B[1m";

    // --- LED Colors ---
    static final int[] GREEN_LED = {0, 255, 0};
    static final int[] RED_LED = {255, 0, 0};
    static final int[] BLUE_LED = {0, 0, 255};

    // --- Game Tracking Variables ---
    static int userScore = 0;
    static int botScore = 0;
    static int draws = 0;
    static int totalRounds = 0;

    public static void main(String[] args) {
        try {
            // 1. Initialize SwiftBot
            swiftBot = SwiftBotAPI.INSTANCE;
            
            printHeader();
            printRules();
            
            // 2. Wait for Start Button
            waitForButtonA();
            
            // 3. Setup Game
            printSectionHeader("GAME SETUP");
            System.out.print(CYAN + "Hi there! Please enter your name:\n> " + RESET);
            String userName = scanner.nextLine().trim();
            if (userName.isEmpty()) userName = "Player 1";
            String botName = "SwiftBot";
            
            System.out.println(GREEN + BOLD + "\nNice to meet you, " + userName + "!" + RESET);
            
            boolean continueProgram = true;
            
            // 4. Main Game Loop
            while (continueProgram) {
                totalRounds++;
                char[][] board = {
                        {' ', ' ', ' '},
                        {' ', ' ', ' '},
                        {' ', ' ', ' '}
                };
                ArrayList<String> movesLog = new ArrayList<>();
                
                System.out.println("\n" + CYAN + BOLD + "WHO GOES FIRST? ROLLING DICE..." + RESET);
                
                // Dice Roll
                int userRoll = 0, botRoll = 0;
                while (userRoll == botRoll) {
                    userRoll = random.nextInt(6) + 1;
                    botRoll = random.nextInt(6) + 1;
                    
                    System.out.println(CYAN + "   [ YOU ]\t\t[ SWIFTBOT ]" + RESET);
                    System.out.println(YELLOW + "      " + userRoll + "   \tVS\t      " + botRoll + RESET);
                    
                    if (userRoll == botRoll) {
                        System.out.println(MAGENTA + "Tie! Re-rolling...\n" + RESET);
                        Thread.sleep(1000);
                    }
                }
                
                boolean userGoesFirst = userRoll > botRoll;
                char p1Piece = 'O', p2Piece = 'X';
                
                if (userGoesFirst) {
                    System.out.println(GREEN + BOLD + ">> YOU WIN THE ROLL!" + RESET);
                    System.out.println(GREEN + ">> You are playing as 'O' (Green Pieces)" + RESET);
                } else {
                    System.out.println(RED + BOLD + ">> SWIFTBOT WINS THE ROLL!" + RESET);
                    System.out.println(RED + ">> You are playing as 'X' (Red Pieces)" + RESET);
                }
                
                System.out.print("\n" + YELLOW + "[ PRESS ENTER TO START ROUND " + totalRounds + " ]" + RESET);
                scanner.nextLine();
                
                boolean gameOver = false;
                int currentTurn = 1;
                String roundWinner = "Draw";
                
                // 5. Round Loop
                while (!gameOver) {
                    printSectionHeader("ROUND: " + totalRounds + " | Turn: " + currentTurn);
                    
                    boolean isUserTurn = (currentTurn % 2 != 0) == userGoesFirst;
                    char activePiece = isUserTurn ? p1Piece : p2Piece;
                    String activePlayerName = isUserTurn ? userName : botName;
                    
                    if (isUserTurn) {
                        System.out.println(CYAN + BOLD + "IT IS YOUR TURN, " + userName.toUpperCase() + " (" + activePiece + ")" + RESET);
                    } else {
                        System.out.println(MAGENTA + BOLD + "SWIFTBOT'S TURN (" + activePiece + ")" + RESET);
                    }
                    
                    printBoard(board);
                    
                    // Take Turns
                    if (isUserTurn) {
                        int[] move = getUserMove(board);
                        board[move[0]][move[1]] = activePiece;
                        movesLog.add(userName + " (" + activePiece + ") -> [" + (move[0]+1) + "," + (move[1]+1) + "]");
                    } else {
                        System.out.println(YELLOW + "SwiftBot is thinking about its move..." + RESET);
                        Thread.sleep(500);
                        System.out.println(YELLOW + "... Awaiting robot decision..." + RESET);
                        Thread.sleep(500);
                        
                        int[] move = getBotMove(board);
                        
                        System.out.println(GREEN + BOLD + ">> TARGET FOUND!" + RESET);
                        System.out.println(CYAN + ">> SwiftBot is moving to Row " + (move[0]+1) + ", Column " + (move[1]+1) + RESET);
                        System.out.println(MAGENTA + "(Watch the robot move!)" + RESET);
                        
                        checkPathClear();
                        moveToSquare(move[0], move[1]);
                        
                        System.out.println(CYAN + ">> Blinking lights..." + RESET);
                        blinkLights(GREEN_LED, 3);
                        
                        System.out.println(CYAN + ">> Returning to start..." + RESET);
                        returnToStart();
                        System.out.println(YELLOW + "[ PLEASE WAIT FOR ROBOT TO STOP MOVING ]" + RESET);
                        
                        board[move[0]][move[1]] = activePiece;
                        movesLog.add(botName + " (" + activePiece + ") -> [" + (move[0]+1) + "," + (move[1]+1) + "]");
                    }
                    
                    // Check End Conditions
                    if (checkWin(board, activePiece)) {
                        printBoard(board);
                        roundWinner = activePlayerName;
                        if (isUserTurn) userScore++; else botScore++;
                        
                        int[] winColor = (activePiece == 'O') ? GREEN_LED : RED_LED;
                        blinkLights(winColor, 3);
                        traceWinningLine();
                        gameOver = true;
                    } else if (checkDraw(board)) {
                        printBoard(board);
                        roundWinner = "Draw";
                        draws++;
                        spin360();
                        blinkLights(BLUE_LED, 3);
                        gameOver = true;
                    }
                    currentTurn++;
                }
                
                // 6. End of Round Processing
                writeToLogFile(totalRounds, userName, movesLog, roundWinner);
                
                printSectionHeader("ROUND " + totalRounds + " COMPLETE!");
                if (roundWinner.equals("Draw")) {
                    System.out.println(YELLOW + BOLD + "IT'S A DRAW!" + RESET);
                } else if (roundWinner.equals(userName)) {
                    System.out.println(GREEN + BOLD + "CONGRATULATIONS, " + userName.toUpperCase() + "! YOU WON!" + RESET);
                } else {
                    System.out.println(RED + BOLD + "SWIFTBOT WINS THIS ROUND!" + RESET);
                }
                
                printScoreboard(userName, botName);
                
                // 7. Ask to Continue
                continueProgram = askContinue();
            }
            
            // 8. Display Final Summary
            displayFinalSummary();
            swiftBot.move(0,0,100); // Shutdown motors
            return;

        } catch (Exception e) {
            System.out.println(RED + BOLD + "\nERROR: I2C disabled or Connection Lost! (Must run on Raspberry Pi)" + RESET);
            e.printStackTrace();
            return;
        }
    }

    // =========================================================================
    // UI & PRINT METHODS
    // =========================================================================

    public static void printHeader() {
        System.out.println(CYAN + BOLD + "===========================================" + RESET);
        System.out.println(CYAN + BOLD + "        SWIFTBOT: NOUGHTS & CROSSES" + RESET);
        System.out.println(CYAN + BOLD + "===========================================" + RESET);
        System.out.println(GREEN + BOLD + "Welcome! Ready to play?" + RESET);
    }

    public static void printRules() {
        System.out.println(MAGENTA + "-------------------------------------------" + RESET);
        System.out.println(YELLOW + BOLD + "[ GAME RULES ]" + RESET);
        System.out.println(CYAN + "1. You are playing against the SwiftBot AI.");
        System.out.println("2. Get three symbols in a row to win.");
        System.out.println("3. The robot will physically move to show its turn." + RESET);
        System.out.println(MAGENTA + "-------------------------------------------" + RESET);
    }

    public static void printSectionHeader(String title) {
        System.out.println(MAGENTA + "===========================================" + RESET);
        System.out.println(MAGENTA + BOLD + " " + title + RESET);
        System.out.println(MAGENTA + "===========================================" + RESET);
    }

    public static void printBoard(char[][] board) {
        System.out.println("\n         " + CYAN + "Col 1   Col 2   Col 3" + RESET);
        for (int r = 0; r < 3; r++) {
            System.out.printf(CYAN + "Row %d " + RESET + "     %c   " + GRAY + "|" + RESET + "   %c   " + GRAY + "|" + RESET + "   %c  \n", 
                              (r+1), board[r][0], board[r][1], board[r][2]);
            if (r < 2) System.out.println(GRAY + "        -------+-------+-------" + RESET);
        }
        System.out.println();
    }

    public static void printScoreboard(String uName, String bName) {
        System.out.println("\n" + CYAN + BOLD + "TOTAL SCOREBOARD" + RESET);
        System.out.println(CYAN + "PLAYER      | WINS | LOSS | DRAW" + RESET);
        System.out.printf("%-11s |  %d   |  %d   |  %d\n", uName, userScore, botScore, draws);
        System.out.printf("%-11s |  %d   |  %d   |  %d\n", bName, botScore, userScore, draws);
        System.out.println(MAGENTA + "-------------------------------------------" + RESET);
    }

    public static void displayFinalSummary() {
        System.out.println();
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(CYAN + BOLD + "           Final Summary" + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(GREEN + "Total rounds played: " + YELLOW + totalRounds + RESET);
        System.out.println(GREEN + "Log file saved as:   " + YELLOW + LOG_FILE + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(GREEN + BOLD + "Thanks for playing Noughts & Crosses!" + RESET);
        System.out.println(GREEN + BOLD + "Program terminated successfully." + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
    }

    public static void printErrorMsg() {
        System.out.println(RED + "\nOops! That move isn't possible." + RESET);
        System.out.println(YELLOW + "Here is how to fix it:");
        System.out.println("* Use a Row number between 1 and 3.");
        System.out.println("* Use a Column number between 1 and 3.");
        System.out.println("* Make sure the square isn't already taken!");
        System.out.println("* Format your answer as row,col (e.g. 2,3)" + RESET);
        System.out.println(CYAN + "Let's try that again:\n" + RESET);
    }

    // =========================================================================
    // GAME LOGIC METHODS
    // =========================================================================

    public static int[] getUserMove(char[][] board) {
        while (true) {
            try {
                System.out.print(CYAN + "Where would you like to move? (row, col)\n> " + RESET);
                String input = scanner.nextLine().trim();
                String[] parts = input.split(",");
                
                if (parts.length != 2) throw new NumberFormatException();
                
                int r = Integer.parseInt(parts[0].trim()) - 1;
                int c = Integer.parseInt(parts[1].trim()) - 1;
                
                if (r >= 0 && r < 3 && c >= 0 && c < 3 && board[r][c] == ' ') {
                    return new int[]{r, c};
                } else {
                    printErrorMsg();
                }
            } catch (Exception e) {
                printErrorMsg();
            }
        }
    }

    public static int[] getBotMove(char[][] board) {
        int r, c;
        do {
            r = random.nextInt(3);
            c = random.nextInt(3);
        } while (board[r][c] != ' ');
        return new int[]{r, c};
    }

    public static boolean checkWin(char[][] board, char piece) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == piece && board[i][1] == piece && board[i][2] == piece) return true;
            if (board[0][i] == piece && board[1][i] == piece && board[2][i] == piece) return true;
        }
        return (board[0][0] == piece && board[1][1] == piece && board[2][2] == piece) || 
               (board[0][2] == piece && board[1][1] == piece && board[2][0] == piece);
    }

    public static boolean checkDraw(char[][] board) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == ' ') return false;
            }
        }
        return true;
    }

    public static void writeToLogFile(int round, String user, ArrayList<String> moves, String winner) {
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
            System.out.println(GREEN + "[ Game saved to log file ]" + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Failed to write to log file: " + e.getMessage() + RESET);
        }
    }

    // =========================================================================
    // HARDWARE & SENSOR METHODS
    // =========================================================================

    public static void waitForButtonA() throws InterruptedException {
        System.out.println(CYAN + BOLD + "\nPRESS BUTTON 'A' TO START GAME" + RESET);
        System.out.println(YELLOW + "(Waiting for you to press the button...)" + RESET);
        
        final boolean[] buttonPressed = {false};
        swiftBot.disableAllButtons();
        
        swiftBot.enableButton(Button.A, () -> {
            if (!buttonPressed[0]) {
                buttonPressed[0] = true;
            }
        });
        
        while (!buttonPressed[0]) { Thread.sleep(100); }
        swiftBot.disableAllButtons();
    }

    public static boolean askContinue() throws InterruptedException {
        System.out.println(CYAN + "> PRESS BUTTON 'Y' to Play Another Round" + RESET);
        System.out.println(YELLOW + "> PRESS BUTTON 'X' to Quit Game" + RESET);
        System.out.println(MAGENTA + "===========================================\n" + RESET);
        
        final boolean[] decision = {false};
        final boolean[] buttonPressed = {false};
        
        swiftBot.disableAllButtons();
        
        swiftBot.enableButton(Button.Y, () -> {
            if (!buttonPressed[0]) {
                decision[0] = true;
                buttonPressed[0] = true;
            }
        });
        
        swiftBot.enableButton(Button.X, () -> {
            if (!buttonPressed[0]) {
                decision[0] = false;
                buttonPressed[0] = true;
            }
        });
        
        while (!buttonPressed[0]) { Thread.sleep(100); }
        swiftBot.disableAllButtons();
        
        if (!decision[0]) {
            System.out.println(YELLOW + "Exiting game..." + RESET);
        }
        return decision[0];
    }

    public static void checkPathClear() {
        boolean pathClear = false;
        while (!pathClear) {
            try {
                double distance = swiftBot.useUltrasound();
                if (distance < 20.0) {
                    System.out.println(RED + "\n[WARNING] Obstacle detected " + String.format("%.1f", distance) + "cm away!" + RESET);
                    System.out.println(YELLOW + "Waiting for the user to clear the board..." + RESET);
                    
                    swiftBot.fillUnderlights(RED_LED);
                    Thread.sleep(500);
                    swiftBot.disableUnderlights();
                    Thread.sleep(500);
                } else {
                    pathClear = true;
                }
            } catch (Exception e) {
                pathClear = true; // Skip if sensor errors out
            }
        }
    }

    public static void blinkLights(int[] color, int times) {
        try {
            for (int i = 0; i < times; i++) {
                swiftBot.fillUnderlights(color);
                Thread.sleep(500);
                swiftBot.disableUnderlights();
                Thread.sleep(500);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void spin360() {
        
        try { swiftBot.move(100, -100, 1500); } catch (Exception e) {}
    }

    public static void moveToSquare(int row, int col) {
       
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    public static void returnToStart() {
        
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    public static void traceWinningLine() {
        
        try { Thread.sleep(2000); } catch (Exception e) {}
    }
}
