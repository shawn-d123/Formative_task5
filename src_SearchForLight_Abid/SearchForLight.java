package src_SearchForLight_Abid;

import swiftbot.SwiftBotAPI;
import swiftbot.ImageSize;
import swiftbot.Button;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Random;
import java.util.Arrays;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

public class SearchForLight {
    // Core Robot Components
    static SwiftBotAPI swiftBot;
    static Scanner scanner = new Scanner(System.in);
    
    // Movement & Speed Configurations
    static int searchSpeed = 30; 
    
    // ADJUSTED TIMINGS FOR SAFETY
    static final int MOVE_TIME_MS = 500;  // Shorter bursts = more frequent sensor checks
    static final int TURN_TIME_MS = 1000; // Longer turn = better angle to clear obstacles
    
    // Mission Tracking & Logging Variables
    static double thresholdLightIntensity = 0;
    static double highestLightIntensity = 0;
    static int lightDetectionCount = 0;
    static List<Double> detectedLightIntensities = new ArrayList<>();
    static List<String> movementLog = new ArrayList<>();
    static int objectsDetected = 0;
    static List<Long> objectDetectionTimes = new ArrayList<>();
    static double totalDistanceTravelled = 0.0;
    
    static final String LOG_DIR = ""; 

    public static void main(String[] args) {
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
        } catch (Exception e) {
            System.out.println("CRITICAL ERROR: Failed to initialize SwiftBot.");
            System.exit(1);
        }

        System.out.println("=========================================");
        System.out.println("      TASK 8: SEARCH FOR LIGHT           ");
        System.out.println("=========================================");
        
        boolean validSpeed = false;
        while (!validSpeed) {
            System.out.print("Enter preferred search speed (10 to 50): ");
            try {
                searchSpeed = Integer.parseInt(scanner.nextLine());
                if (searchSpeed >= 10 && searchSpeed <= 50) {
                    validSpeed = true;
                } else {
                    System.out.println(">>> Error: Speed must be strictly between 10 and 50.");
                }
            } catch (NumberFormatException e) {
                System.out.println(">>> Error: Please enter a valid whole number.");
            }
        }
        System.out.println(">>> Search speed successfully locked at: " + searchSpeed);
        System.out.println("=========================================");

        System.out.println("WAITING... Press Button 'A' on the SwiftBot to begin the mission.");
        
        swiftBot.enableButton(Button.A, () -> {
            System.out.println("\n>>> Button A Pressed! Initializing Light Search Protocol...");
            startLightSearch();
        });

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }
    }

    public static void startLightSearch() {
        long startTime = System.currentTimeMillis();
        swiftBot.disableButton(Button.A); 
        
        System.out.println(">>> Capturing initial environment light level to set baseline...");
        
        BufferedImage initialImg = takePictureSafely();
        thresholdLightIntensity = getAverageIntensity(initialImg, 0, initialImg.getWidth());
        System.out.printf(">>> Baseline Threshold Light Intensity locked at: %.2f\n", thresholdLightIntensity);

        boolean running = true;
        Random random = new Random();

        while (running) {
            System.out.println("\n-----------------------------------------");
            cleanOldDetections();
            
            if (objectDetectionTimes.size() >= 5) {
                System.out.println("!!! CRITICAL ALERT: 5 objects detected in under 5 minutes !!!");
                System.out.print("Please enter the command 'TERMINATE' to safely stop the robot: ");
                String input = scanner.nextLine();
                if ("TERMINATE".equals(input)) {
                    System.out.println(">>> Termination command accepted. Halting robot.");
                    running = false;
                    break;
                } else {
                    System.out.println(">>> Invalid command. Robot resuming operation...");
                    objectDetectionTimes.clear(); 
                }
            }

            // 1. CAPTURE & ANALYZE LIGHT
            BufferedImage img = takePictureSafely();
            int width = img.getWidth();
            
            double avgLeft = getAverageIntensity(img, 0, width / 3);
            double avgCenter = getAverageIntensity(img, width / 3, (width / 3) * 2);
            double avgRight = getAverageIntensity(img, (width / 3) * 2, width);
            
            double maxCurrentIntensity = Math.max(avgLeft, Math.max(avgCenter, avgRight));
            
            if (maxCurrentIntensity > highestLightIntensity) {
                highestLightIntensity = maxCurrentIntensity; 
            }

            System.out.printf("[VISION] Light -> Left: %.2f | Center: %.2f | Right: %.2f\n", avgLeft, avgCenter, avgRight);

            // Get Base Directions
            String primaryDir = getDirection(avgLeft, avgCenter, avgRight, 1);
            String secondaryDir = getDirection(avgLeft, avgCenter, avgRight, 2);

            // Check Sensors
            double distanceToObj = swiftBot.useUltrasound();
            // BUFFER SET TO 70CM FOR MAXIMUM SAFETY
            boolean obstacleDetected = (distanceToObj > 0 && distanceToObj < 70.0);
            boolean lightDetected = (maxCurrentIntensity > thresholdLightIntensity + 5);

            String targetDir = "CENTER"; 

            // 2. THE STRICT DECISION TREE
            if (obstacleDetected) {
                objectsDetected++;
                objectDetectionTimes.add(System.currentTimeMillis());
                System.out.printf("!!! OBSTACLE DETECTED at %.2f cm !!! Blinking RED.\n", distanceToObj);
                blinkRed();
                
                try {
                    File outputfile = new File(LOG_DIR + "obstacle_" + objectsDetected + ".jpg");
                    ImageIO.write(img, "jpg", outputfile);
                    System.out.println(">>> Obstacle image successfully saved.");
                } catch (IOException e) {
                    System.out.println(">>> Warning: Failed to save obstacle image.");
                }

                if (primaryDir.equals("CENTER")) {
                    targetDir = secondaryDir; 
                } else {
                    targetDir = primaryDir; 
                }
                
                System.out.println(">>> Evading obstacle! Safe route locked: " + targetDir);

            } else if (!lightDetected) {
                String[] dirs = {"LEFT", "RIGHT", "CENTER"}; 
                targetDir = dirs[random.nextInt(3)];
                System.out.println(">>> No significant light source found. Wandering: " + targetDir);
                
            } else {
                lightDetectionCount++;
                detectedLightIntensities.add(maxCurrentIntensity);
                targetDir = primaryDir;
                System.out.println(">>> Tracking target light! Route: " + targetDir);
            }

            // 3. EXECUTE MOVEMENT
            swiftBot.fillUnderlights(new int[]{0, 255, 0}); 
            executeMovement(targetDir);
            swiftBot.disableUnderlights();
        }

        // 4. SHUTDOWN
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        writeLogFile(durationMs);
        
        try { Thread.sleep(500); } catch (Exception e) {} 
        System.exit(0);
    }

    public static BufferedImage takePictureSafely() {
        try {
            for (Method m : swiftBot.getClass().getMethods()) {
                if (m.getReturnType() == BufferedImage.class && m.getName().toLowerCase().contains("take")) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == boolean.class) args[i] = false; 
                        else if (paramTypes[i] == ImageSize.class) args[i] = ImageSize.SQUARE_144x144;
                    }
                    return (BufferedImage) m.invoke(swiftBot, args);
                }
            }
        } catch (Exception e) {
            System.out.println("Camera auto-detect error.");
        }
        return new BufferedImage(144, 144, BufferedImage.TYPE_BYTE_GRAY);
    }

    public static double getAverageIntensity(BufferedImage img, int startX, int endX) {
        long sum = 0;
        int count = 0;
        for (int x = startX; x < endX; x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                sum += (0.299 * r + 0.587 * g + 0.114 * b);
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0;
    }

    public static String getDirection(double left, double center, double right, int rank) {
        class DirAvg implements Comparable<DirAvg> {
            String dir; double avg;
            DirAvg(String d, double a) { dir = d; avg = a; }
            public int compareTo(DirAvg o) { return Double.compare(o.avg, this.avg); }
        }
        DirAvg[] arr = { new DirAvg("LEFT", left), new DirAvg("CENTER", center), new DirAvg("RIGHT", right) };
        Arrays.sort(arr);
        
        if (rank == 2 && arr[1].avg == arr[2].avg) {
            return (new Random().nextBoolean()) ? arr[1].dir : arr[2].dir;
        }
        return arr[rank - 1].dir;
    }

    // UPDATED: SAFE NAVIGATION LOGIC
    public static void executeMovement(String dir) {
        double distanceEstimate = searchSpeed * (MOVE_TIME_MS / 1000.0); 
        
        try {
            if (dir.equals("LEFT")) {
                System.out.println("[MOTOR] Executing Left Pivot Turn...");
                swiftBot.move(0, searchSpeed, TURN_TIME_MS);
                movementLog.add("Left Turn Evasion");
                return; // STOP AND RE-SCAN (Prevents driving into wall)
                
            } else if (dir.equals("RIGHT")) {
                System.out.println("[MOTOR] Executing Right Pivot Turn...");
                swiftBot.move(searchSpeed, 0, TURN_TIME_MS);
                movementLog.add("Right Turn Evasion");
                return; // STOP AND RE-SCAN (Prevents driving into wall)
            }
            
            // This only runs if the path is CLEAR (targetDir was CENTER)
            System.out.println("[MOTOR] Path clear. Driving Forward...");
            swiftBot.move(searchSpeed, searchSpeed, MOVE_TIME_MS);
            movementLog.add("Straight " + distanceEstimate + " cm");
            totalDistanceTravelled += distanceEstimate;
            
        } catch (Exception e) {
            System.out.println(">>> CRITICAL ERROR: Motor failed!");
        }
    }

    public static void blinkRed() {
        try {
            for (int i = 0; i < 3; i++) {
                swiftBot.fillUnderlights(new int[]{255, 0, 0});
                Thread.sleep(200);
                swiftBot.disableUnderlights();
                Thread.sleep(200);
            }
        } catch (InterruptedException e) { }
    }

    public static void cleanOldDetections() {
        long fiveMinsAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        objectDetectionTimes.removeIf(time -> time < fiveMinsAgo);
    }

    public static void writeLogFile(long durationMs) {
        File logFile = new File(LOG_DIR + "LightSearchLog.txt");
        try (FileWriter writer = new FileWriter(logFile)) {
            writer.write("=========================================\n");
            writer.write("       SWIFTBOT LIGHT SEARCH LOG         \n");
            writer.write("=========================================\n\n");
            writer.write("Duration of Execution: " + (durationMs / 1000) + " seconds\n");
            writer.write("Threshold Light Intensity: " + thresholdLightIntensity + "\n");
            writer.write("Brightest Light Detected: " + highestLightIntensity + "\n");
            writer.write("Number of times light detected: " + lightDetectionCount + "\n");
            writer.write("Total Distance Travelled: ~" + totalDistanceTravelled + " cm\n");
            writer.write("Total Obstacles Detected: " + objectsDetected + "\n");
            writer.write("\n--- DETAILED MOVEMENT HISTORY ---\n");
            for (String move : movementLog) {
                writer.write("> " + move + "\n");
            }
            System.out.println("\n>>> SUCCESS: Log file securely generated at: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println(">>> WARNING: Log generation failed.");
        }
    }
}
