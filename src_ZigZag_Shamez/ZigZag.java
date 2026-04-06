package src_ZigZag_Shamez;

import swiftbot.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZigzagTask {
    
    static SwiftBotAPI swiftBot;
    
    // color codes 
    static final String RESET = "\u001B[0m";
    static final String CYAN = "\u001B[36m";
    static final String YELLOW = "\u001B[33m";
    static final String GREEN = "\u001B[32m";
    static final String RED = "\u001B[31m";
    static final String BLUE = "\u001B[34m";
    static final String MAGENTA = "\u001B[35m";
    static final String BOLD = "\u001B[1m";
    
    // Constants validation
    static final int MIN_SEGMENT_LENGTH = 15;
    static final int MAX_SEGMENT_LENGTH = 85;
    static final int MAX_SEGMENTS = 12;
    
    // Constants for speed calculation
    static final double MIN_SPEED_PERCENT = 0.20;
    static final double MAX_SPEED_PERCENT = 0.80;
    static final int MAX_SPEED = 100;
    
    // Calibration 
    static final double CM_PER_SECOND_AT_50_SPEED = 15.0;
    
    // LED colors
    static int[] GREEN_LED = new int[] {0, 255, 0};
    static int[] BLUE_LED = new int[] {0, 0, 255};
    
    // Journey tracking variables
    static int totalJourneys = 0;
    static double longestDistance = 0;
    static String longestJourneyDetails = "";
    static double shortestDistance = Double.MAX_VALUE;
    static String shortestJourneyDetails = "";
    
    // Turn sequence tracker 
    static String forwardTurnSequence = "";
    
    public static void main(String[] args) {
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
            printHeader();
            
            boolean continueProgram = true;
            
            while (continueProgram) {
                // 1: Scan and validate QR code
                int[] parameters = scanAndValidateQRCode();
                if (parameters == null) {
                    continue;
                }
                
                int segmentLength = parameters[0];
                int numSegments = parameters[1];
                
                // 2: Generate random wheel speed
                int wheelSpeed = generateRandomSpeed();
                
                // 3: Get turning direction from user
                String direction = getTurningDirection();
                
                // 4: Display preview and get confirmation
                double velocity = calculateVelocity(wheelSpeed);
                int movementTime = calculateMovementTime(segmentLength, velocity);
                double estimatedTime = (movementTime * numSegments) / 1000.0;
                
                displayPreview(segmentLength, numSegments, direction, wheelSpeed, velocity, estimatedTime);
                
                if (!getUserConfirmation()) {
                    System.out.println(YELLOW + "Movement cancelled. Returning to QR scan." + RESET);
                    System.out.println();
                    continue;
                }
                
                // 5: Countdown before movement
                countdown();
                
                // 6: Execute zigzag movement and record turn sequence
                long startTime = System.currentTimeMillis();
                forwardTurnSequence = executeZigzagMovement(segmentLength, numSegments, direction, wheelSpeed, movementTime);
                
                // 7: Retrace path
                System.out.println();
                printSectionHeader("Path Retracing");
                System.out.println(CYAN + "Retracing zigzag path..." + RESET);
                System.out.println(YELLOW + "Please do not obstruct the robot." + RESET);
                System.out.println();
                
                retracePath(segmentLength, numSegments, direction, wheelSpeed, movementTime, forwardTurnSequence);
                long endTime = System.currentTimeMillis();
                
                // 8: Calculate and log journey details
                double executionTime = (endTime - startTime) / 1000.0;
                double totalDistance = segmentLength * numSegments;
                double straightLineDistance = calculateStraightLineDistance(segmentLength, numSegments);
                
                // Update journey tracking
                totalJourneys++;
                updateJourneyTracking(segmentLength, numSegments, straightLineDistance);
                
                // Write to log file
                String logFile = writeToLogFile(segmentLength, numSegments, wheelSpeed, velocity, totalDistance, 
                              executionTime, straightLineDistance);
                
                // 9: Display completion message
                displayCompletionMessage(logFile);
                
                // 10: Ask user to continue or terminate
                continueProgram = askContinue();
            }
            
            // Display final summary before terminating
            displayFinalSummary();
            
        } catch (Exception e) {
            System.out.println(RED + "\nERROR: An unexpected error occurred!" + RESET);
            e.printStackTrace();
            System.exit(5);
        }
    }
    
    /*
       program header
     */
    public static void printHeader() {
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(CYAN + BOLD + "   SwiftBot Zigzag Control Program" + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println();
    }
    
    /*
       section header
     */
    public static void printSectionHeader(String title) {
        System.out.println(MAGENTA + "----------------------------------------" + RESET);
        System.out.println(MAGENTA + "     " + title + RESET);
        System.out.println(MAGENTA + "----------------------------------------" + RESET);
    }
    
    /*
       Scans and validates QR code input
     */
    public static int[] scanAndValidateQRCode() {
        System.out.println(CYAN + "Please scan the QR code to begin." + RESET);
        System.out.println(YELLOW + "Waiting for QR code input..." + RESET);
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        long timeout = 20000; // 20 seconds timeout
        
        try {
            while (System.currentTimeMillis() - startTime < timeout) {
                BufferedImage img = swiftBot.getQRImage();
                String qrContent = swiftBot.decodeQRImage(img);
                
                if (qrContent != null && !qrContent.isEmpty()) {
                    qrContent = qrContent.trim();
                    
                    if (!qrContent.contains("-")) {
                        displayQRError("Invalid format. Expected: Value1-Value2 (e.g., 40-6)");
                        return null;
                    }
                    
                    String[] parts = qrContent.split("-");
                    
                    if (parts.length != 2) {
                        displayQRError("Invalid format. Expected: Value1-Value2 (e.g., 40-6)");
                        return null;
                    }
                    
                    try {
                        int segmentLength = Integer.parseInt(parts[0].trim());
                        int numSegments = Integer.parseInt(parts[1].trim());
                        
                        if (segmentLength < MIN_SEGMENT_LENGTH || segmentLength > MAX_SEGMENT_LENGTH) {
                            displayQRError("Segment length must be between " + MIN_SEGMENT_LENGTH + 
                                         " and " + MAX_SEGMENT_LENGTH + " cm.");
                            return null;
                        }
                        
                        if (numSegments % 2 != 0) {
                            displayQRError("Number of segments must be even.");
                            return null;
                        }
                        
                        if (numSegments > MAX_SEGMENTS || numSegments <= 0) {
                            displayQRError("Number of segments must be between 2 and " + MAX_SEGMENTS + ".");
                            return null;
                        }
                        
                        displayQRSuccess(segmentLength, numSegments);
                        return new int[] {segmentLength, numSegments};
                        
                    } catch (NumberFormatException e) {
                        displayQRError("Values must be numbers. Example: 40-6");
                        return null;
                    }
                }
                
                Thread.sleep(500);
            }
            
            System.out.println(RED + "QR code scan timeout. Please try again." + RESET);
            System.out.println();
            return null;
            
        } catch (Exception e) {
            System.out.println(RED + "ERROR: Unable to scan QR code." + RESET);
            e.printStackTrace();
            return null;
        }
    }
    
    /*
       Displays QR code error message
     */
    public static void displayQRError(String message) {
        printSectionHeader("QR Code Error");
        System.out.println(RED + "The scanned QR code is invalid." + RESET);
        System.out.println(RED + "Reason: " + message + RESET);
        System.out.println();
        System.out.println(YELLOW + "Please ensure the format is: Value1-Value2" + RESET);
        System.out.println(YELLOW + "Example: 40-6" + RESET);
        System.out.println();
        System.out.println(CYAN + "Press ENTER to rescan the QR code." + RESET);
        printSectionHeader("");
        System.out.println();
        
        try {
            new Scanner(System.in).nextLine();
        } catch (Exception e) {
            // Continue
        }
    }
    
    /*
       Displays QR code acceptance message
     */
    public static void displayQRSuccess(int segmentLength, int numSegments) {
        printSectionHeader("QR Code Accepted");
        System.out.println(GREEN + "Zigzag Segment Length: " + segmentLength + " cm" + RESET);
        System.out.println(GREEN + "Number of Segments: " + numSegments + RESET);
        System.out.println(GREEN + "Input validation successful." + RESET);
        printSectionHeader("");
        System.out.println();
    }
    
    /*
       Generates random wheel speed within safe operating range
     */
    public static int generateRandomSpeed() {
        Random random = new Random();
        int minSpeed = (int)(MIN_SPEED_PERCENT * MAX_SPEED);
        int maxSpeed = (int)(MAX_SPEED_PERCENT * MAX_SPEED);
        return minSpeed + random.nextInt(maxSpeed - minSpeed + 1);
    }
    
    /*
       Gets turning direction from user
     */
    public static String getTurningDirection() {
        Scanner scanner = new Scanner(System.in);
        printSectionHeader("Initial Orientation");
        System.out.println(CYAN + "The SwiftBot is facing straight forward." + RESET);
        System.out.println(CYAN + "Which direction should it lean for the zigzag pattern?" + RESET);
        System.out.println(YELLOW + "[L] Lean Left" + RESET);
        System.out.println(YELLOW + "[R] Lean Right" + RESET);
        System.out.print(CYAN + "Enter choice: " + RESET);
        
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            
            if (input.equals("L")) {
                System.out.println(GREEN + "Direction selected: Lean Left" + RESET);
                printSectionHeader("");
                System.out.println();
                return "Left";
            } else if (input.equals("R")) {
                System.out.println(GREEN + "Direction selected: Lean Right" + RESET);
                printSectionHeader("");
                System.out.println();
                return "Right";
            } else {
                System.out.print(RED + "Invalid input. Please enter L or R: " + RESET);
            }
        }
    }
    
    /*
       Calculates velocity based on wheel speed
     */
    public static double calculateVelocity(int wheelSpeed) {
        return (wheelSpeed / 50.0) * CM_PER_SECOND_AT_50_SPEED;
    }
    
    /*
       Calculates movement time for a segment
     */
    public static int calculateMovementTime(int distance, double velocity) {
        double timeInSeconds = distance / velocity;
        int timeInMillis = (int)(timeInSeconds * 1000);
        
        // Ensure minimum movement time
        if (timeInMillis < 1000) {
            timeInMillis = 1000;
        }
        
        return timeInMillis;
    }
    
    /*
       Displays preview summary
     */
    public static void displayPreview(int segmentLength, int numSegments, String direction, 
                                     int wheelSpeed, double velocity, double estimatedTime) {
        printSectionHeader("Zigzag Execution Preview");
        System.out.println(CYAN + "Segment Length     : " + YELLOW + segmentLength + " cm" + RESET);
        System.out.println(CYAN + "Number of Segments : " + YELLOW + numSegments + RESET);
        System.out.println(CYAN + "Initial Lean       : " + YELLOW + direction + RESET);
        System.out.println(CYAN + "Wheel Speed        : " + YELLOW + wheelSpeed + "%" + RESET);
        System.out.printf(CYAN + "Velocity           : " + YELLOW + "%.2f cm/s\n" + RESET, velocity);
        System.out.printf(CYAN + "Estimated Time     : " + YELLOW + "%.1f seconds\n" + RESET, estimatedTime);
        printSectionHeader("");
        System.out.println();
    }
    
    /*
       Gets user confirmation to start movement
     */
    public static boolean getUserConfirmation() {
        Scanner scanner = new Scanner(System.in);
        System.out.println(CYAN + "Do you want to start the movement?" + RESET);
        System.out.print(YELLOW + "[Y] Yes  [N] No: " + RESET);
        
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            
            if (input.equals("Y")) {
                return true;
            } else if (input.equals("N")) {
                return false;
            } else {
                System.out.print(RED + "Invalid input. Please enter Y or N: " + RESET);
            }
        }
    }
    
    /*
       Displays countdown before movement
     */
    public static void countdown() throws InterruptedException {
        System.out.println();
        printSectionHeader("Preparing to Start");
        System.out.println(YELLOW + "Starting in:" + RESET);
        
        for (int i = 3; i > 0; i--) {
            System.out.println(GREEN + BOLD + i + "..." + RESET);
            Thread.sleep(1000);
        }
        
        System.out.println(GREEN + BOLD + "Movement starting now." + RESET);
        printSectionHeader("");
        System.out.println();
    }
    
    /*
       Executes the zigzag movement pattern and stores turn sequence
     */
    public static String executeZigzagMovement(int segmentLength, int numSegments, 
                                            String direction, int wheelSpeed, int movementTime) 
                                            throws InterruptedException {
        printSectionHeader("Zigzag Movement in Progress");
        System.out.println(YELLOW + "Please keep a safe distance from the robot." + RESET);
        System.out.println();
        
        int moveSpeed = Math.max(wheelSpeed, 60);
        
        // Store the sequence of turns for retracing
        StringBuilder turnSequence = new StringBuilder();
        
        // Track which wheel should move for the turn
        String currentDirection = direction;
        
        // 1: Initial slight turn in chosen direction
        System.out.println(CYAN + "Turning slightly " + direction.toLowerCase() + " to start zigzag..." + RESET);
        initialSlightTurn(direction, moveSpeed);
        Thread.sleep(500);
        
        // 2: Execute zigzag segments
        boolean isGreen = true;
        
        for (int i = 1; i <= numSegments; i++) {
            System.out.println(GREEN + "Executing zigzag segment " + i + " of " + numSegments + RESET);
            System.out.println(YELLOW + "LED Status: " + (isGreen ? "GREEN" : "BLUE") + RESET);
            
            // Set LED color
            if (isGreen) {
                swiftBot.fillUnderlights(GREEN_LED);
            } else {
                swiftBot.fillUnderlights(BLUE_LED);
            }
            
            // Move forward for segment
            swiftBot.move(moveSpeed, moveSpeed, movementTime);
            Thread.sleep(500);
            
            // Turn 90 degrees INWARD using single wheel
            if (i < numSegments) {
                System.out.println(CYAN + "Turning 90° inward..." + RESET);
                turnInward90Degrees(currentDirection, moveSpeed);
                Thread.sleep(500);
                
                // Store this turn in the sequence
                turnSequence.append(currentDirection.equals("Left") ? "L" : "R");
                
                // Switch direction for next segment
                currentDirection = currentDirection.equals("Left") ? "Right" : "Left";
            }
            
            // Toggle LED color
            isGreen = !isGreen;
        }
        
        swiftBot.disableUnderlights();
        System.out.println();
        System.out.println(GREEN + BOLD + "Zigzag movement completed!" + RESET);
        System.out.println(CYAN + "Turn sequence: " + turnSequence.toString() + RESET);
        printSectionHeader("");
        
        return turnSequence.toString();
    }
    
    /*
       Initial slight turn to start the zigzag pattern
     */
    public static void initialSlightTurn(String direction, int speed) throws InterruptedException {
        int slightTurnTime = 400;
        
        if (direction.equals("Left")) {
            // Turn slightly left: left wheel backward, right wheel forward
            swiftBot.move(-speed, speed, slightTurnTime);
        } else {
            // Turn slightly right: left wheel forward, right wheel backward
            swiftBot.move(speed, -speed, slightTurnTime);
        }
    }
    
    /*
       1.Turn 90 degrees INWARD using ONE wheel moving, other stationary
       2.If current direction is RIGHT, turn LEFT (move right wheel, left stays still)
       3.If current direction is LEFT, turn RIGHT (move left wheel, right stays still)
     */
    public static void turnInward90Degrees(String currentDirection, int speed) throws InterruptedException {
        int turn90Time = 1500; 
        
        if (currentDirection.equals("Right")) {
            // RIGHT wheel moves forward, LEFT wheel stationary
            System.out.println(YELLOW + "  Right wheel moving, left stationary (turning left)..." + RESET);
            swiftBot.move(0, speed, turn90Time);
        } else {
            // LEFT wheel moves forward, RIGHT wheel stationary
            System.out.println(YELLOW + "  Left wheel moving, right stationary (turning right)..." + RESET);
            swiftBot.move(speed, 0, turn90Time);
        }
    }
    
    /*
       Retraces the zigzag path back to the starting position.
       Steps:
         1. Turn 180 degrees to face back (1000ms)
         2. Drive each segment in reverse order
         3. Reverse the turn sequence AND flip L/R so the opposite
            wheel is used, which produces the correct mirrored angle
     */
    public static void retracePath(int segmentLength, int numSegments, String originalDirection,
                                   int wheelSpeed, int movementTime, String forwardTurnSequence)
                                   throws InterruptedException {

        int moveSpeed = Math.max(wheelSpeed, 60);

        // 1: Turn 180 degrees to face back toward start
        System.out.println(CYAN + "Turning 180° to retrace path..." + RESET);
        int turn180Time = 1000;
        swiftBot.move(-moveSpeed, moveSpeed, turn180Time);
        Thread.sleep(1000);

        // 2: Reverse the sequence then flip each direction
        // Reverse: last turn becomes first (retrace in reverse order)
        // Flip: after 180, opposite wheel produces the correct mirrored angle
        String reversedSequence = new StringBuilder(forwardTurnSequence).reverse().toString();
        StringBuilder retraceSequence = new StringBuilder();
        for (char turn : reversedSequence.toCharArray()) {
            retraceSequence.append(turn == 'L' ? 'R' : 'L');
        }

        System.out.println(CYAN + "Forward turn sequence : " + forwardTurnSequence        + RESET);
        System.out.println(CYAN + "Retrace turn sequence : " + retraceSequence.toString()  + RESET);
        System.out.println();

        // 3: Drive segments in reverse applying the retrace sequence
        boolean isGreen = (numSegments % 2 == 0);
        int turnIndex = 0;

        for (int i = numSegments; i > 0; i--) {
            System.out.println(GREEN + "Retracing segment " + i + " of " + numSegments + RESET);
            System.out.println(YELLOW + "LED Status: " + (isGreen ? "GREEN" : "BLUE") + RESET);

            if (isGreen) {
                swiftBot.fillUnderlights(GREEN_LED);
            } else {
                swiftBot.fillUnderlights(BLUE_LED);
            }

            swiftBot.move(moveSpeed, moveSpeed, movementTime);
            Thread.sleep(500);

            if (i > 1 && turnIndex < retraceSequence.length()) {
                char nextTurn = retraceSequence.charAt(turnIndex);
                String turnDirection = (nextTurn == 'L') ? "Left" : "Right";

                System.out.println(CYAN + "Retracing turn " + (turnIndex + 1)
                        + ": " + turnDirection + " (90° inward)" + RESET);

                turnInward90Degrees(turnDirection, moveSpeed);
                Thread.sleep(500);

                turnIndex++;
            }

            isGreen = !isGreen;
        }

        swiftBot.disableUnderlights();
        System.out.println();
        System.out.println(GREEN + BOLD + "Path retracing completed!" + RESET);
        System.out.println(GREEN + BOLD + "SwiftBot returned to starting position." + RESET);
        printSectionHeader("");
    }
    
    /*
       Calculates straight line distance from start to end of zigzag
     */
    public static double calculateStraightLineDistance(int segmentLength, int numSegments) {
        int horizontalSegments = numSegments / 2;
        double horizontalDistance = horizontalSegments * segmentLength;
        double verticalDistance = horizontalSegments * segmentLength;
        return Math.sqrt(Math.pow(horizontalDistance, 2) + Math.pow(verticalDistance, 2));
    }
    
    /**
     * Writes journey details to log file
     */
    public static String writeToLogFile(int segmentLength, int numSegments, int wheelSpeed, 
                                     double velocity, double totalDistance, double executionTime, 
                                     double straightLineDistance) {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "logs/zigzag_log_" + timestamp + ".txt";
            
            FileWriter writer = new FileWriter(filename, true);
            PrintWriter printWriter = new PrintWriter(writer);
            
            printWriter.println("=== Zigzag Journey Log ===");
            printWriter.println("Date/Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            printWriter.println("Journey Number: " + totalJourneys);
            printWriter.println();
            printWriter.println("Input Parameters:");
            printWriter.println("  Segment Length: " + segmentLength + " cm");
            printWriter.println("  Number of Segments: " + numSegments);
            printWriter.println();
            printWriter.println("Movement Details:");
            printWriter.println("  Wheel Speed: " + wheelSpeed + "%");
            printWriter.printf("  Velocity: %.2f cm/s\n", velocity);
            printWriter.printf("  Total Zigzag Distance: %.2f cm\n", totalDistance);
            printWriter.printf("  Execution Time: %.2f seconds\n", executionTime);
            printWriter.printf("  Straight-Line Distance: %.2f cm\n", straightLineDistance);
            printWriter.println();
            printWriter.println("==========================================");
            printWriter.println();
            
            printWriter.close();
            return filename;
            
        } catch (IOException e) {
            System.out.println(RED + "ERROR: Could not write to log file." + RESET);
            e.printStackTrace();
            return "Error creating log file";
        }
    }
    
    /*
       Updates journey tracking
     */
    public static void updateJourneyTracking(int segmentLength, int numSegments, double straightLineDistance) {
        String journeyDetails = "Segment Length: " + segmentLength + " cm, Segments: " + numSegments;
        
        if (straightLineDistance > longestDistance) {
            longestDistance = straightLineDistance;
            longestJourneyDetails = journeyDetails;
        }
        
        if (straightLineDistance < shortestDistance) {
            shortestDistance = straightLineDistance;
            shortestJourneyDetails = journeyDetails;
        }
    }
    
    /*
       Displays execution completion message
     */
    public static void displayCompletionMessage(String logFile) {
        System.out.println();
        printSectionHeader("Execution Complete");
        System.out.println(GREEN + BOLD + "Zigzag task completed successfully." + RESET);
        System.out.println(CYAN + "Log file saved at: " + YELLOW + logFile + RESET);
        printSectionHeader("");
        System.out.println();
    }
    
    /*
       Asks user if they want to continue or terminate
     */
    public static boolean askContinue() throws InterruptedException {
        printSectionHeader("Program Options");
        System.out.println(CYAN + "Would you like to run another zigzag task?" + RESET);
        System.out.println(YELLOW + "[Y] Press Y button" + RESET);
        System.out.println(YELLOW + "[X] Press X button" + RESET);
        printSectionHeader("");
        System.out.println();
        
        final boolean[] decision = {false};
        final boolean[] buttonPressed = {false};
        
        swiftBot.disableAllButtons();
        
        swiftBot.enableButton(Button.Y, () -> {
            if (!buttonPressed[0]) {
                buttonPressed[0] = true;
                decision[0] = true;
                System.out.println(GREEN + "Continuing program..." + RESET);
                System.out.println();
                swiftBot.disableAllButtons();
            }
        });
        
        swiftBot.enableButton(Button.X, () -> {
            if (!buttonPressed[0]) {
                buttonPressed[0] = true;
                decision[0] = false;
                System.out.println(RED + "Terminating program..." + RESET);
                System.out.println();
                swiftBot.disableAllButtons();
            }
        });
        
        long timeout = System.currentTimeMillis() + 30000;
        while (!buttonPressed[0] && System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }
        
        swiftBot.disableAllButtons();
        
        if (!buttonPressed[0]) {
            System.out.println(YELLOW + "No input received. Terminating program..." + RESET);
            return false;
        }
        
        return decision[0];
    }
    
    /*
       Displays final summary
     */
    public static void displayFinalSummary() {
        System.out.println();
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(CYAN + BOLD + "          Final Summary" + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println(GREEN + "Total zigzag journeys completed: " + YELLOW + totalJourneys + RESET);
        System.out.println();
        
        if (totalJourneys > 0) {
            System.out.println(MAGENTA + "Journey with longest straight-line distance:" + RESET);
            System.out.println(CYAN + "  " + longestJourneyDetails + RESET);
            System.out.printf(CYAN + "  Distance: " + YELLOW + "%.2f cm\n" + RESET, longestDistance);
            System.out.println();
            
            System.out.println(MAGENTA + "Journey with shortest straight-line distance:" + RESET);
            System.out.println(CYAN + "  " + shortestJourneyDetails + RESET);
            System.out.printf(CYAN + "  Distance: " + YELLOW + "%.2f cm\n" + RESET, shortestDistance);
            System.out.println();
        }
        
        System.out.println(GREEN + "Log files saved in: " + YELLOW + "logs/ directory" + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
        System.out.println();
        System.out.println(GREEN + BOLD + "Thank you for using SwiftBot Zigzag!" + RESET);
        System.out.println(GREEN + BOLD + "Program terminated successfully." + RESET);
        System.out.println(CYAN + BOLD + "========================================" + RESET);
    }
}
