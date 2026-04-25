package src_DetectObject_chris;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Calendar;
import java.util.*;
import javax.imageio.ImageIO;

import com.pi4j.io.exception.IOException;

import swiftbot.*;

public class Detect_Object {

    static SwiftBotAPI swiftBot;
    public static boolean terminate = false;

    private static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;
    private static final int ENCOUNTER_THRESHOLD = 3;
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String WHITE  = "\u001B[37m";
    private static final String BORDER = "========================================================================================================================";

    private static String currentMode = "";
    private static long currentModeStart = 0L;
    private static int currentModeEncounters = 0;
    private static long currentModeEncountersWindowStart = 0L;
    private static List<String> currentModeImagePaths = new ArrayList<>();

    private static final List<ModeSummary> sessionSummaries = new ArrayList<>();
    private static final Scanner scanner = new Scanner(System.in);

    private static boolean buttonXListenerAttached = false;
    private static boolean sessionFinished = false;

    private enum CuriousState { WANDERING, MOVING_FORWARD, MOVING_BACKWARD, HOLDING }

    public static void main(String[] args) {
        displayWelcomeScreen();
        terminate = false;
        sessionFinished = false;
        buttonXListenerAttached = false;
        currentMode = "";
        currentModeStart = 0L;
        currentModeEncounters = 0;
        currentModeEncountersWindowStart = 0L;
        currentModeImagePaths = new ArrayList<>();
        sessionSummaries.clear();

        try {
            swiftBot = SwiftBotAPI.INSTANCE;
            attachButtonXListener();

            while (!sessionFinished) {
                displayModeScanScreen();
//            	System.out.println("1 = Curious");
//            	System.out.println("2 = Scaredy");
//            	System.out.println("3 = Dubious");
//            	System.out.println("0 = Exit");
//            	System.out.print("[SYSTEM] Enter choice: ");
//            	String choice = scanner.nextLine().trim();
//            	if (choice.equals("0")) writeFinalLogAndExit();
//            	else if (choice.equals("1")) runMode("Curious");
//            	else if (choice.equals("2")) runMode("Scaredy");
//            	else if (choice.equals("3")) runMode("Dubious");
//            	else System.out.println("[SYSTEM] Invalid input. Please enter 0, 1, 2 or 3.");
            	
            	
                String choice = testQRCodeDetection(); //setting the QR code detection
                //checking for different courses of actions to take
                if (choice.equals("Exit")) writeFinalLogAndExit();
                else if (choice.equals("Curious SwiftBot")) runMode("Curious");
                else if (choice.equals("Scaredy SwiftBot")) runMode("Scaredy");
                else if (choice.equals("Dubious SwiftBot")) runMode("Dubious");
                else { displayInvalidInputScreen("INPUT ERROR: Unrecognised QR message. Please scan a valid mode QR code."); }
            }
        } catch (Exception e) {
            displayError("I2C disabled!");
            e.printStackTrace();
            return;
        }
    }
    
    public static String testQRCodeDetection() {
    	//attempting the scanning part
		int attempts = 1;
		while(attempts <=10) {
            displayInfo("QR SCAN", "Scanning for QR code... Attempt " + attempts + " of 10");
			try {
				BufferedImage img = swiftBot.getQRImage();
				String decodedMessage = swiftBot.decodeQRImage(img);
				if (!decodedMessage.isEmpty()) {
                    displayInfo("MODE SELECTED", "QR code found. Decoded message: " + decodedMessage);
					return decodedMessage;
				}	
				++attempts;
				return "No QR code detected";
			} catch (Exception e) {
                displayInfo("QR SCAN", "Unable to find QR code...trying again...");
				++attempts;
			}
		}
		return currentMode ;
	}

    private static void runMode(String modeName) {
    	//Defining starters
        currentMode = modeName;
        currentModeStart = System.currentTimeMillis();
        currentModeEncounters = 0;
        currentModeEncountersWindowStart = currentModeStart;
        currentModeImagePaths = new ArrayList<>();
        terminate = false;

        displayModeSelectedScreen(currentMode.toUpperCase() + " MODE ACTIVE");
        //if terminate = true exit
        while (terminate != true && !sessionFinished) {
        	//run mode according to the current mode
	        if (currentMode.equalsIgnoreCase("Curious")) curious();
	        else if (currentMode.equalsIgnoreCase("Scaredy")) scaredy();
	        else if (currentMode.equalsIgnoreCase("Dubious")) dubious();
        }
        if (terminate == true && !sessionFinished) {//if terminate is true put details in ModeSummary and execute writeFinalLogAndExit
            long duration = System.currentTimeMillis() - currentModeStart;
            sessionSummaries.add(new ModeSummary(currentMode, duration, currentModeEncounters, new ArrayList<>(currentModeImagePaths)));
            displayInfo("MODE SUMMARY", "Mode '" + currentMode + "' ended. Encounters: " + currentModeEncounters + " Duration (ms): " + duration);
            writeFinalLogAndExit();
        }
    }

    private static void attachButtonXListener() {
    	//The code will exit when the requirements are met
        if (!buttonXListenerAttached) {
            try {
                swiftBot.enableButton(Button.X, () -> {
                    displayTerminationScreen();
                    terminate = true;
                    writeFinalLogAndExit();//Ultimatum for exit 
                });
                buttonXListenerAttached = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void scaredy() {
        try {
            long lastObjectTime = System.currentTimeMillis();//registering the current time

            while (!terminate) {//Run the while loop until terminate doesn't become true
                double distance = measureDistance();//Check the distance

                if (distance <= 50) {//If distance < 50 cm
                    lastObjectTime = System.currentTimeMillis();//Register the time for last object found
                    setUnderlights(255,0,0);//Set underlights red
                    takePictureAndRecord();//take picture of the object
                    displayObjectDetectedScreen(distance);
                    displayScaredyModeScreen("Object detected -> running away");

                    for (int i=0; i<3; i++) {//blink red 3 times
                        swiftBot.disableUnderlights();
                        Thread.sleep(300);
                        setUnderlights(255,0,0);
                        Thread.sleep(300);
                    }

                    swiftBot.move(-80, -80, 1000);//move back (80 cm)
                    swiftBot.move(80, -49, 1100);//rotate 180 degree
                    swiftBot.move(100, 100, 3000);//run at full speed for 3 second

                    if (checkEncounterThresholdAndMaybePrompt()) return;//Check if the encounter threshold has been met
                    				//( Met more than 3 objects in less than 5 min )

                } else {
                    setUnderlights(0,0,255);//set underlight to blue (wandering around)
                    swiftBot.move(50, 50, 1000);
                    if (System.currentTimeMillis() - lastObjectTime >= 5000) {//If no object found for 5 second changing direction
                        swiftBot.move(0,0,1000);//stop for 1 second
                        displayNoObjectScreen("SCAREDY MODE ACTIVE");
                        swiftBot.move(40, -19, 1100);//changing into a slightly different direction
                        lastObjectTime = System.currentTimeMillis();//modifying the lastObjectTime 
                    }
                }
                Thread.sleep(200);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void dubious() {
        Random rand = new Random();
		int r = rand.nextInt(2);//randomly choose between 0 and 1
        String selectedMode;
		if (r == 1) {//if r is 1 then scaredy
            selectedMode = "Scaredy";
            displayDubiousModeScreen(r, selectedMode);
			runMode("Scaredy");
		}
        else {
            selectedMode = "Curious";
            displayDubiousModeScreen(r, selectedMode);
        	runMode("Curious");
        }
    }

    public static double measureDistance() {
        double distance = 0.0;
        try {
            distance = swiftBot.useUltrasound();//Use ultrasound to measure distance
            displayDistanceReading(distance);
        } catch (Exception e) { e.printStackTrace(); }
        return distance;
    }

    public static void curious() {
        try {
            long lastChangeTime = System.currentTimeMillis();
            double lastDistance = -1;

            while (!terminate) {
                double distance = measureDistance();

                // ===== NO OBJECT: WANDERING =====
                if (distance < 0 || distance > 80) {//If distance is less than 0 or distance is greater than 80 Wander around randomly
                    displayNoObjectScreen("CURIOUS MODE ACTIVE");
                    setUnderlights(0, 0, 255);
                    moveForward(21);
                }

                // ===== OBJECT FAR (>=34cm): MOVE FORWARD =====
                else if (distance >= 34) { //If distance is greater than or equal to 34 cm move forward 30 cm before the object.
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object far -> moving forward", distance);
                    setUnderlights(0, 255, 0);//Set underlights green
                    moveForward(distance); //Cover the sufficient distance
                    swiftBot.move(0, 0, 500);//Wait for 500 mini seconds
                    swiftBot.disableUnderlights();//Disable underlights
                    takePictureAndRecord();//Take picture

                    lastChangeTime = System.currentTimeMillis();//Record current time in lastChangeTime
                    Thread.sleep(5000);//Wait for 5 seconds
                }

                // ===== OBJECT AT BUFFER (≈30cm): HOLD =====
                else if (distance > 26 && distance < 34) {
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object at buffer -> holding position", distance);

                    for (int i = 0; i < 3; i++) {//blink underlights 3 times
                        setUnderlights(0, 255, 0);
                        Thread.sleep(300);
                        swiftBot.disableUnderlights();
                        Thread.sleep(300);
                    }

                    takePictureAndRecord();//Click a picture
                    lastChangeTime = System.currentTimeMillis();//Record current time in lastChangeTime
                    Thread.sleep(5000);//Wait for 5 seconds
                }

                // ===== OBJECT TOO CLOSE (≤26cm): MOVE BACKWARD =====
                else { // distance <= 26
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object too close -> moving backward", distance);
                    setUnderlights(0, 255, 0);
                    moveBackward(distance); // Cover the sufficient distance  
                    swiftBot.move(0, 0, 500);//Wait for 500 mini seconds
                    swiftBot.disableUnderlights();//Disable underlights
                    takePictureAndRecord();//Take picture

                    lastChangeTime = System.currentTimeMillis();//Record the current time in lastChangeTime 
                    Thread.sleep(5000);//Wait for 5 seconds
                }

                // ===== RE-MEASURE DISTANCE =====
                double newDistance = measureDistance();//Measure the new distance after 5 seconds

                // ===== NO MOVEMENT OR TIMEOUT =====
                if (lastDistance != -1 &&
                    (System.currentTimeMillis() - lastChangeTime >= 5000 ||
                     Math.abs(newDistance - lastDistance) < 1.0)) {//If object moves for 5 seconds and does not encounter anything it shall change direction

                    displayCuriousModeScreen("No movement -> pause & change direction", newDistance);
                    Thread.sleep(1000);//Wait for a second
                    swiftBot.move(40, -19, 1100);//change direction
                    lastChangeTime = System.currentTimeMillis();//update time
                }

                // ===== UPDATE LAST DISTANCE =====
                lastDistance = newDistance;//Updating last distance

                // ===== ENCOUNTER CHECK =====
                if (checkEncounterThresholdAndMaybePrompt()) {//Checl if more than 3 objects in less than 5 min then stop
                    if (!currentMode.equalsIgnoreCase("Curious")) return;
                }

                Thread.sleep(200);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String takePictureAndRecord() {
        try {
            BufferedImage bwImage = swiftBot.takeGrayscaleStill(ImageSize.SQUARE_480x480);//take the picture
            if (bwImage == null) {
                displayError("Image is null");
                return null;
            }

            String imagesDirPath = System.getProperty("user.dir") + File.separator + "images";//Creating a string for current path and the folder images in it
            File imagesDir = new File(imagesDirPath);//Putting the file path
            if (!imagesDir.exists()) imagesDir.mkdirs();//if the images dir does not exist create it

            String filename = imagesDirPath + File.separator + "bwImage_" + System.currentTimeMillis() + ".png";//save the image in accordance with the time in millis
            ImageIO.write(bwImage, "png", new File(filename));//Write the image
            currentModeImagePaths.add(filename);//store the filename we added
            currentModeEncounters++;//Add number of encounters
            if (currentModeEncounters == 1) currentModeEncountersWindowStart = System.currentTimeMillis();//register the time when the first encounter was made
            displayInfo("OBJECT DETECTED", "Object image captured: " + filename);
            displayInfo("IMAGE DIRECTORY", "Directory path: " + imagesDirPath);
            Thread.sleep(1000);
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean checkEncounterThresholdAndMaybePrompt() {
    	swiftBot.disableUnderlights();
        long now = System.currentTimeMillis();
        long windowElapsed = now - currentModeEncountersWindowStart;

        if (currentModeEncounters > ENCOUNTER_THRESHOLD && windowElapsed <= FIVE_MINUTES_MS) {
        	//if in less then 5 min we encounter more than 3 (enocounter threshold) objects then do the following
            displayInfo("OBJECT DETECTED", "More than " + ENCOUNTER_THRESHOLD + " objects detected in under 5 minutes while in mode: " + currentMode);

            while (true) {
                displayModeChangePromptScreen();
                String input = scanner.nextLine();
                attachButtonXListener();
                if (input.equals("2")) { //if 2 is pressed put terminate = true
                	terminate = true;
                	return true; 
                }
                else if (input.equals("1")) {//if 1 is pressed perform switchToMode
                    while (true) {
                        displayInfo("MODE SELECTED", "Choose new mode: 1 = Curious, 2 = Scaredy, 3 = Dubious");
                        displayInfo("MODE SELECTED", "Enter choice (1/2/3):");
                        String modeChoice = scanner.nextLine().trim();//needs fixing (to qr code scanning)
                        if (modeChoice.equals("1")) { switchToMode("Curious"); return true; }
                        else if (modeChoice.equals("2")) { switchToMode("Scaredy"); return true; }
                        else if (modeChoice.equals("3")) { switchToMode("Dubious"); return true; }
                        else displayInvalidInputScreen("INPUT ERROR: Invalid choice. Please enter 1, 2 or 3.");
                    }
                }
                else { displayInvalidInputScreen("INPUT ERROR: Please choose either 1 or 2 (or press Button X on SwiftBot)."); continue;}
            }
        }
        return false;
    }

    private static void switchToMode(String newMode) {
    	//Mainly used for information gathering
        long duration = System.currentTimeMillis() - currentModeStart;
        sessionSummaries.add(new ModeSummary(currentMode, duration, currentModeEncounters, new ArrayList<>(currentModeImagePaths)));//store the data
        //modify details
        currentMode = newMode;
        currentModeStart = System.currentTimeMillis();
        currentModeEncounters = 0;
        currentModeImagePaths = new ArrayList<>();
        currentModeEncountersWindowStart = currentModeStart;
        displayModeSelectedScreen(currentMode.toUpperCase() + " MODE ACTIVE");
        runMode(currentMode);//Use runMode
    }

    public static void moveForward(double distance) {
    	int time = (int) ((((distance - 30) / 18.50) * 1000));
        try { swiftBot.move(100, 100, Math.abs(time)); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void moveBackward(double distance) {
    	double x = 30 - distance;
    	int time = (int) (((x/23.78)*1000));
        try { swiftBot.move(-100, -100, Math.abs(time)); } catch (Exception e) { e.printStackTrace(); }
    }

    private static void setUnderlights(int r, int g, int b) {
        int[] color = new int[]{r,g,b};
        //creating a list for all the underlights
        Underlight[] underlights = { Underlight.BACK_LEFT, Underlight.BACK_RIGHT,
            Underlight.MIDDLE_LEFT, Underlight.MIDDLE_RIGHT,
            Underlight.FRONT_LEFT, Underlight.FRONT_RIGHT };
        try {
            for (Underlight u : underlights) {//go through all underlights 1 by 1
                swiftBot.setUnderlight(u, color);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static synchronized void writeFinalLogAndExit() {
        if (sessionFinished) {
            return;
        }

        sessionFinished = true;
        terminate = true;
        displayInfo("LOGGING", "Writing to log file...");
        try {
            long now = System.currentTimeMillis();//the current time in milli second
            if (currentMode != null && !currentMode.isEmpty()) {//if currentMode contains something
                long duration = now - currentModeStart;//find the duration and add it to sessionSummaries
                sessionSummaries.add(new ModeSummary(currentMode, duration, currentModeEncounters, new ArrayList<>(currentModeImagePaths)));
            }
            
            String logsDirPath = System.getProperty("user.dir") + File.separator + "logs";//The logs folder put the dir in a string
            File logsDir = new File(logsDirPath);//Make a new file
            if (!logsDir.exists()) logsDir.mkdirs();//if logDir does not exist then make the dir

            Calendar date = Calendar.getInstance();
            String timestampForName = String.valueOf(date.getTimeInMillis());//find the time stamp
            String logFilePath = logsDirPath + File.separator + "log_" + timestampForName + ".txt";//use the time stamp for naming

            BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath));
            bw.write("SwiftBot session log - " + date.getTime().toString());
            bw.newLine(); bw.newLine();

            for (ModeSummary ms : sessionSummaries) {//write all the details in session summaries
                bw.write("Mode: " + ms.modeName);
                bw.newLine();
                bw.write("Duration (ms): " + ms.durationMs);
                bw.newLine();
                bw.write("Object encounters: " + ms.encounters);
                bw.newLine();
                bw.write("Image files:");
                bw.newLine();
                if (ms.imagePaths != null && !ms.imagePaths.isEmpty()) {
                    for (String p : ms.imagePaths) {
                        bw.write("  " + p); bw.newLine();
                    }
                } else { bw.write("  (no images)"); bw.newLine(); }
                bw.newLine();
            }

            bw.flush(); bw.close();
            displayLogSavedScreen(logFilePath);

            // Backup log and images
            List<String> allImages = new ArrayList<>();//in this store all image paths
            for (ModeSummary ms : sessionSummaries) {
            	allImages.addAll(ms.imagePaths);
            }
            backupSessionData(logFilePath, allImages);//backup the details

			

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void backupSessionData(String logFilePath, List<String> imagePaths) throws IOException, java.io.IOException {

        // Create backup directory
        String backupDirPath = System.getProperty("user.dir") + File.separator + "backup_" + System.currentTimeMillis();
        Path backupDir = Paths.get(backupDirPath);
        Files.createDirectories(backupDir);

        displayInfo("BACKUP", "Backup folder created: " + backupDirPath);

        // Backup log file
        if (logFilePath != null && !logFilePath.isEmpty()) {
            Path sourceLog = Paths.get(logFilePath);

            if (Files.exists(sourceLog)) {
                Path targetLog = backupDir.resolve(sourceLog.getFileName());
                Files.copy(sourceLog, targetLog, StandardCopyOption.REPLACE_EXISTING);
                displayInfo("BACKUP", "Log file backed up: " + targetLog);
            }
        }

        // Backup images
        if (imagePaths != null) {
            int count = 0;

            for (String imgPath : imagePaths) {
                Path sourceImg = Paths.get(imgPath);

                if (Files.exists(sourceImg)) {
                    String fileName = count++ + "_" + sourceImg.getFileName().toString();
                    Path targetImg = backupDir.resolve(fileName);

                    Files.copy(sourceImg, targetImg, StandardCopyOption.REPLACE_EXISTING);
                    displayInfo("BACKUP", "Image backed up: " + targetImg);
                }
            }
        }

        displayInfo("BACKUP", "Backup completed successfully.");
    }

    private static void printBorder(String colour) {
        System.out.println(colour + BORDER + RESET);
    }

    private static void displayWelcomeScreen() {
        System.out.println();
        printBorder(CYAN);
        System.out.println(CYAN + "  _____  ______ _______ ______ _____ _______    ____  ____       _ ______ _____ _______ " + RESET);
        System.out.println(CYAN + " |  __ \\|  ____|__   __|  ____/ ____|__   __|  / __ \\|  _ \\     | |  ____/ ____|__   __|" + RESET);
        System.out.println(CYAN + " | |  | | |__     | |  | |__ | |       | |    | |  | | |_) |    | | |__ | |       | |   " + RESET);
        System.out.println(CYAN + " | |  | |  __|    | |  |  __|| |       | |    | |  | |  _ < _   | |  __|| |       | |   " + RESET);
        System.out.println(CYAN + " | |__| | |____   | |  | |___| |____   | |    | |__| | |_) | |__| | |___| |____   | |   " + RESET);
        System.out.println(CYAN + " |_____/|______|  |_|  |______\\_____|  |_|     \\____/|____/ \\____/|______\\_____|  |_|   " + RESET);
        printBorder(CYAN);
        System.out.println(WHITE + "CS1813" + RESET);
        System.out.println(WHITE + "Assignment - Detect Object" + RESET);
        System.out.println(WHITE + "Made By: Chris Das (2550446)" + RESET);
        System.out.println();
    }

    private static void displayModeScanScreen() {
        System.out.println();
        System.out.println(CYAN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(CYAN + "  __  __  ____  _____  ______      _____  _____          _   _ " + RESET);
        System.out.println(CYAN + " |  \\/  |/ __ \\|  __ \\|  ____|    / ____|/ ____|   /\\   | \\ | |" + RESET);
        System.out.println(CYAN + " | \\  / | |  | | |  | | |__      | (___ | |       /  \\  |  \\| |" + RESET);
        System.out.println(CYAN + " | |\\/| | |  | | |  | |  __|      \\___ \\| |      / /\\ \\ | . ` |" + RESET);
        System.out.println(CYAN + " | |  | | |__| | |__| | |____     ____) | |____ / ____ \\| |\\  |" + RESET);
        System.out.println(CYAN + " |_|  |_|\\____/|_____/|______|   |_____/ \\_____/_/    \\_\\_| \\_|" + RESET);
        System.out.println();
        System.out.println(CYAN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Scan QR code to choose operating mode." + RESET);
        System.out.println(WHITE + "Accepted QR values : " + CYAN + "Curious SwiftBot | Scaredy SwiftBot | Dubious SwiftBot | Exit" + RESET);
        System.out.println();
    }

    private static void displayModeSelectedScreen(String modeLabel) {
        System.out.println();
        System.out.println(GREEN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + " __  __  ____  _____  ______      _____ ______ _      ______ _____ _______ ______ _____  " + RESET);
        System.out.println(GREEN + "|  \\/  |/ __ \\|  __ \\|  ____|    / ____|  ____| |    |  ____/ ____|__   __|  ____|  __ \\ " + RESET);
        System.out.println(GREEN + "| \\  / | |  | | |  | | |__      | (___ | |__  | |    | |__ | |       | |  | |__  | |  | |" + RESET);
        System.out.println(GREEN + "| |\\/| | |  | | |  | |  __|      \\___ \\|  __| | |    |  __|| |       | |  |  __| | |  | |" + RESET);
        System.out.println(GREEN + "| |  | | |__| | |__| | |____     ____) | |____| |____| |___| |____   | |  | |____| |__| |" + RESET);
        System.out.println(GREEN + "|_|  |_|\\____/|_____/|______|   |_____/|______|______|______\\_____|  |_|  |______|_____/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "MODE SELECTED : " + GREEN + modeLabel + RESET);
        System.out.println(WHITE + "SYSTEM STATUS : " + GREEN + "READY" + RESET);
        System.out.println();
    }

    private static void displayCuriousModeScreen(String status, double distance) {
        System.out.println();
        System.out.println(GREEN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + "   _____ _    _ _____  _____ ____   _    _  _____ " + RESET);
        System.out.println(GREEN + "  / ____| |  | |  __ \\|_   _/ __ \\ | |  | |/ ____|" + RESET);
        System.out.println(GREEN + " | |    | |  | | |__) | | || |  | || |  | | (___  " + RESET);
        System.out.println(GREEN + " | |    | |  | |  _  /  | || |  | || |  | |\\___ \\ " + RESET);
        System.out.println(GREEN + " | |____| |__| | | \\ \\ _| || |__| || |__| |____) |" + RESET);
        System.out.println(GREEN + "  \\_____|\\____/|_|  \\_\\_____\\____/  \\____/|_____/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "CURIOUS MODE ACTIVE" + RESET);
        System.out.println(WHITE + "Status          : " + GREEN + status + RESET);
        System.out.println(WHITE + "Distance        : " + GREEN + String.format("%.2f", distance) + " cm" + RESET);
        System.out.println();
    }

    private static void displayScaredyModeScreen(String status) {
        System.out.println();
        System.out.println(RED + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(RED + "   _____  _____          _______ _______     __" + RESET);
        System.out.println(RED + "  / ____|/ ____|   /\\   |__   __|  __ \\ \\   / /" + RESET);
        System.out.println(RED + " | (___ | |       /  \\     | |  | |__) \\ \\_/ / " + RESET);
        System.out.println(RED + "  \\___ \\| |      / /\\ \\    | |  |  _  / \\   /  " + RESET);
        System.out.println(RED + "  ____) | |____ / ____ \\   | |  | | \\ \\  | |   " + RESET);
        System.out.println(RED + " |_____/ \\_____/_/    \\_\\  |_|  |_|  \\_\\ |_|   " + RESET);
        System.out.println();
        System.out.println(RED + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "SCAREDY MODE ACTIVE" + RESET);
        System.out.println(WHITE + "Status          : " + RED + status + RESET);
        System.out.println();
    }

    private static void displayDubiousModeScreen(int randomChoice, String selectedMode) {
        System.out.println();
        System.out.println(YELLOW + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(YELLOW + "  _____  _    _ ____ _____ ____  _    _  _____ " + RESET);
        System.out.println(YELLOW + " |  __ \\| |  | |  _ \\_   _/ __ \\| |  | |/ ____|" + RESET);
        System.out.println(YELLOW + " | |  | | |  | | |_) || || |  | | |  | | (___  " + RESET);
        System.out.println(YELLOW + " | |  | | |  | |  _ < | || |  | | |  | |\\___ \\ " + RESET);
        System.out.println(YELLOW + " | |__| | |__| | |_) || || |__| | |__| |____) |" + RESET);
        System.out.println(YELLOW + " |_____/ \\____/|____/_____\\____/ \\____/|_____/ " + RESET);
        System.out.println();
        System.out.println(YELLOW + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "DUBIOUS MODE ACTIVE" + RESET);
        System.out.println(WHITE + "Random choice    : " + YELLOW + randomChoice + RESET);
        System.out.println(WHITE + "MODE SELECTED    : " + YELLOW + selectedMode + RESET);
        System.out.println();
    }

    private static void displayObjectDetectedScreen(double distance) {
        System.out.println();
        System.out.println(RED + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(RED + "  ____  ____       _ ______ _____ _______   _____  ______ _______ ______ _____ _______ ______ _____  " + RESET);
        System.out.println(RED + " / __ \\|  _ \\     | |  ____/ ____|__   __| |  __ \\|  ____|__   __|  ____/ ____|__   __|  ____|  __ \\ " + RESET);
        System.out.println(RED + "| |  | | |_) |    | | |__ | |       | |    | |  | | |__     | |  | |__ | |       | |  | |__  | |  | |" + RESET);
        System.out.println(RED + "| |  | |  _ < _   | |  __|| |       | |    | |  | |  __|    | |  |  __|| |       | |  |  __| | |  | |" + RESET);
        System.out.println(RED + "| |__| | |_) | |__| | |___| |____   | |    | |__| | |____   | |  | |___| |____   | |  | |____| |__| |" + RESET);
        System.out.println(RED + " \\____/|____/ \\____/|______\\_____|  |_|    |_____/|______|  |_|  |______\\_____|  |_|  |______|_____/ " + RESET);
        System.out.println();
        System.out.println(RED + "===================================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "OBJECT DETECTED" + RESET);
        System.out.println(WHITE + "Distance        : " + RED + String.format("%.2f", distance) + " cm" + RESET);
        System.out.println();
    }

    private static void displayNoObjectScreen(String modeName) {
        System.out.println();
        System.out.println(BLUE + "===================================================================================================================================" + "============" + RESET);
        System.out.println();
        System.out.println(BLUE + "  _   _  ____     ____  ____       _ ______ _____  _____ _______    _____ ______          _____   _____ _    _ _____ _   _  _____ " + RESET);
        System.out.println(BLUE + " | \\ | |/ __ \\   / __ \\|  _ \\     | |  ____/ ____|/ ____|__   __|  / ____|  ____|   /\\   |  __ \\ / ____| |  | |_   _| \\ | |/ ____|" + RESET);
        System.out.println(BLUE + " |  \\| | |  | | | |  | | |_) |    | | |__ | |    | |       | |    | (___ | |__     /  \\  | |__) | |    | |__| | | | |  \\| | |  __ " + RESET);
        System.out.println(BLUE + " | . ` | |  | | | |  | |  _ < _   | |  __|| |    | |       | |     \\___ \\|  __|   / /\\ \\ |  _  /| |    |  __  | | | | . ` | | |_ |" + RESET);
        System.out.println(BLUE + " | |\\  | |__| | | |__| | |_) | |__| | |___| |____| |____   | |     ____) | |____ / ____ \\| | \\ \\| |____| |  | |_| |_| |\\  | |__| |" + RESET);
        System.out.println(BLUE + " |_| \\_|\\____/   \\____/|____/ \\____/|______\\_____|\\_____|  |_|    |_____/|______/_/    \\_\\_|  \\_\\\\_____|_|  |_|_____|_| \\_|\\_____|" + RESET);
        System.out.println();
        System.out.println(BLUE + "===================================================================================================================================" + "============" + RESET);
        System.out.println();
        System.out.println(WHITE + modeName + RESET);
        System.out.println(WHITE + "Status          : " + BLUE + "No object in active range. Wandering/searching..." + RESET);
        System.out.println();
    }

    private static void displayModeChangePromptScreen() {
        System.out.println();
        printBorder(YELLOW);
        System.out.println(YELLOW + "MODE CHANGE PROMPT" + RESET);
        System.out.println(WHITE + "Enter 1 to change mode, 2 to terminate program (or press X to terminate): " + RESET);
        printBorder(YELLOW);
    }

    private static void displayInvalidInputScreen(String message) {
        System.out.println();
        printBorder(RED);
        System.out.println(RED + " _____ _   _ _____  _    _ _______    ______ _____  _____   ____  _____  " + RESET);
        System.out.println(RED + "|_   _| \\ | |  __ \\| |  | |__   __|  |  ____|  __ \\|  __ \\ / __ \\|  __ \\ " + RESET);
        System.out.println(RED + "  | | |  \\| | |__) | |  | |  | |     | |__  | |__) | |__) | |  | | |__) |" + RESET);
        System.out.println(RED + "  | | | . ` |  ___/| |  | |  | |     |  __| |  _  /|  _  /| |  | |  _  / " + RESET);
        System.out.println(RED + " _| |_| |\\  | |    | |__| |  | |     | |____| | \\ \\| | \\ \\| |__| | | \\ \\ " + RESET);
        System.out.println(RED + "|_____|_| \\_|_|     \\____/   |_|     |______|_|  \\_\\_|  \\_\\\\____/|_|  \\_\\" + RESET);
        printBorder(RED);
        System.out.println(WHITE + "INPUT ERROR" + RESET);
        System.out.println(WHITE + message + RESET);
        System.out.println();
    }

    private static void displayLogSavedScreen(String logFilePath) {
        System.out.println();
        printBorder(GREEN);
        System.out.println(GREEN + " _      ____   _____   ______ _____ _      ______    _____         ______      ________ _____  " + RESET);
        System.out.println(GREEN + "| |    / __ \\ / ____| |  ____|_   _| |    |  ____|  / ____|  /\\   / /  _ \\    |  ____|  __ \\ " + RESET);
        System.out.println(GREEN + "| |   | |  | | |  __  | |__    | | | |    | |__    | (___   /  \\ / /| |_) |   | |__  | |  | |" + RESET);
        System.out.println(GREEN + "| |   | |  | | | |_ | |  __|   | | | |    |  __|    \\___ \\ / /\\ \\ / |  _ <    |  __| | |  | |" + RESET);
        System.out.println(GREEN + "| |___| |__| | |__| | | |     _| |_| |____| |____   ____) / ____ \\  | |_) |   | |____| |__| |" + RESET);
        System.out.println(GREEN + "|______\\____/ \\_____| |_|    |_____|______|______| |_____/_/    \\_\\ |____/    |______|_____/ " + RESET);
        printBorder(GREEN);
        System.out.println(WHITE + "LOG FILE SAVED" + RESET);
        System.out.println(WHITE + "Path: " + GREEN + logFilePath + RESET);
        System.out.println(WHITE + "RETURNING TO MENU" + RESET);
        System.out.println();
    }

    private static void displayTerminationScreen() {
        System.out.println();
        printBorder(RED);
        System.out.println(RED + " _______ ______ _____  __  __ _____ _   _          _______ _____ ____  _   _ " + RESET);
        System.out.println(RED + "|__   __|  ____|  __ \\|  \\/  |_   _| \\ | |   /\\   |__   __|_   _/ __ \\| \\ | |" + RESET);
        System.out.println(RED + "   | |  | |__  | |__) | \\  / | | | |  \\| |  /  \\     | |    | || |  | |  \\| |" + RESET);
        System.out.println(RED + "   | |  |  __| |  _  /| |\\/| | | | | . ` | / /\\ \\    | |    | || |  | | . ` |" + RESET);
        System.out.println(RED + "   | |  | |____| | \\ \\| |  | |_| |_| |\\  |/ ____ \\   | |   _| || |__| | |\\  |" + RESET);
        System.out.println(RED + "   |_|  |______|_|  \\_\\_|  |_|_____|_| \\_/_/    \\_\\  |_|  |_____\\____/|_| \\_|" + RESET);
        printBorder(RED);
        System.out.println(WHITE + "Button X pressed. Termination requested." + RESET);
        System.out.println();
    }

    private static void displayDistanceReading(double distance) {
        System.out.println(CYAN + "[DISTANCE]" + RESET + WHITE + " Distance to object: " + CYAN + String.format("%.2f", distance) + " cm" + RESET);
    }

    private static void displayInfo(String label, String message) {
        System.out.println(YELLOW + "[" + label + "] " + RESET + WHITE + message + RESET);
    }

    private static void displayError(String message) {
        System.out.println();
        printBorder(RED);
        System.out.println(RED + "ERROR" + RESET);
        System.out.println(WHITE + message + RESET);
        printBorder(RED);
        System.out.println();
    }


    private static class ModeSummary {//write all details of ModeSummaary
        String modeName; long durationMs; int encounters; List<String> imagePaths;
        ModeSummary(String m, long d, int e, List<String> imgs) { modeName=m; durationMs=d; encounters=e; imagePaths=imgs; }
    }
}
