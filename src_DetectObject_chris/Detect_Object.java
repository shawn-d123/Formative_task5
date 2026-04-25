package src_DetectObject_chris;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

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

                String choice = testQRCodeDetection();

                if (choice.equals("Exit")) {
                    writeFinalLogAndExit();
                } else if (choice.equals("Curious SwiftBot")) {
                    runMode("Curious");
                } else if (choice.equals("Scaredy SwiftBot")) {
                    runMode("Scaredy");
                } else if (choice.equals("Dubious SwiftBot")) {
                    runMode("Dubious");
                } else {
                    displayInvalidInputScreen("INPUT ERROR: Unrecognised QR message. Please scan a valid mode QR code.");
                }
            }
        } catch (Exception e) {
            displayError("SwiftBot could not be initialised. Please check the SwiftBot connection and I2C settings.");
            e.printStackTrace();
            return;
        }
    }

    public static String testQRCodeDetection() {
        int attempts = 1;

        while (attempts <= 10) {
            displayInfo("QR SCAN", "Scanning for QR code... Attempt " + attempts + " of 10");

            try {
                BufferedImage img = swiftBot.getQRImage();
                String decodedMessage = swiftBot.decodeQRImage(img);

                if (decodedMessage != null && !decodedMessage.isEmpty()) {
                    displayInfo("MODE SELECTED", "QR code found. Decoded message: " + decodedMessage);
                    return decodedMessage;
                }

                displayInfo("QR SCAN", "No QR code detected on this attempt.");
                attempts++;
            } catch (Exception e) {
                displayInfo("QR SCAN", "Unable to find QR code... trying again.");
                attempts++;
            }
        }

        return "No QR code detected";
    }

    private static void runMode(String modeName) {
        currentMode = modeName;
        currentModeStart = System.currentTimeMillis();
        currentModeEncounters = 0;
        currentModeEncountersWindowStart = currentModeStart;
        currentModeImagePaths = new ArrayList<>();
        terminate = false;

        displayModeSelectedScreen(currentMode.toUpperCase() + " MODE ACTIVE");

        while (!terminate && !sessionFinished) {
            if (currentMode.equalsIgnoreCase("Curious")) {
                curious();
            } else if (currentMode.equalsIgnoreCase("Scaredy")) {
                scaredy();
            } else if (currentMode.equalsIgnoreCase("Dubious")) {
                dubious();
            }
        }

        if (terminate && !sessionFinished) {
            long duration = System.currentTimeMillis() - currentModeStart;
            displayInfo("MODE SUMMARY", "Mode '" + currentMode + "' ended. Encounters: " + currentModeEncounters + " Duration (ms): " + duration);
            writeFinalLogAndExit();
        }
    }

    private static void attachButtonXListener() {
        if (!buttonXListenerAttached) {
            try {
                swiftBot.enableButton(Button.X, () -> {
                    displayTerminationScreen();
                    terminate = true;
                    writeFinalLogAndExit();
                });

                buttonXListenerAttached = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void scaredy() {
        try {
            long lastObjectTime = System.currentTimeMillis();

            while (!terminate) {
                double distance = measureDistance();

                if (distance <= 50) {
                    lastObjectTime = System.currentTimeMillis();

                    setUnderlights(255, 0, 0);
                    takePictureAndRecord();

                    displayObjectDetectedScreen(distance);
                    displayScaredyModeScreen("Object detected -> running away");

                    for (int i = 0; i < 3; i++) {
                        swiftBot.disableUnderlights();
                        Thread.sleep(300);

                        setUnderlights(255, 0, 0);
                        Thread.sleep(300);
                    }

                    swiftBot.move(-80, -80, 1000);
                    swiftBot.move(80, -49, 1100);
                    swiftBot.move(100, 100, 3000);

                    if (checkEncounterThresholdAndMaybePrompt()) {
                        return;
                    }
                } else {
                    setUnderlights(0, 0, 255);
                    swiftBot.move(50, 50, 1000);

                    if (System.currentTimeMillis() - lastObjectTime >= 5000) {
                        swiftBot.move(0, 0, 1000);
                        displayNoObjectScreen("SCAREDY MODE ACTIVE");
                        swiftBot.move(40, -19, 1100);
                        lastObjectTime = System.currentTimeMillis();
                    }
                }

                Thread.sleep(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dubious() {
        Random rand = new Random();
        int randomChoice = rand.nextInt(2);

        String selectedMode;

        if (randomChoice == 1) {
            selectedMode = "Scaredy";
            displayDubiousModeScreen(randomChoice, selectedMode);
            runMode("Scaredy");
        } else {
            selectedMode = "Curious";
            displayDubiousModeScreen(randomChoice, selectedMode);
            runMode("Curious");
        }
    }

    public static double measureDistance() {
        double distance = 0.0;

        try {
            distance = swiftBot.useUltrasound();
            displayDistanceReading(distance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return distance;
    }

    public static void curious() {
        try {
            long lastChangeTime = System.currentTimeMillis();
            double lastDistance = -1;

            while (!terminate) {
                double distance = measureDistance();

                if (distance < 0 || distance > 80) {
                    displayNoObjectScreen("CURIOUS MODE ACTIVE");
                    setUnderlights(0, 0, 255);
                    moveForward(21);
                } else if (distance >= 34) {
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object far -> moving forward", distance);

                    setUnderlights(0, 255, 0);
                    moveForward(distance);

                    swiftBot.move(0, 0, 500);
                    swiftBot.disableUnderlights();

                    takePictureAndRecord();

                    lastChangeTime = System.currentTimeMillis();
                    Thread.sleep(5000);
                } else if (distance > 26 && distance < 34) {
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object at buffer -> holding position", distance);

                    for (int i = 0; i < 3; i++) {
                        setUnderlights(0, 255, 0);
                        Thread.sleep(300);

                        swiftBot.disableUnderlights();
                        Thread.sleep(300);
                    }

                    takePictureAndRecord();

                    lastChangeTime = System.currentTimeMillis();
                    Thread.sleep(5000);
                } else {
                    displayObjectDetectedScreen(distance);
                    displayCuriousModeScreen("Object too close -> moving backward", distance);

                    setUnderlights(0, 255, 0);
                    moveBackward(distance);

                    swiftBot.move(0, 0, 500);
                    swiftBot.disableUnderlights();

                    takePictureAndRecord();

                    lastChangeTime = System.currentTimeMillis();
                    Thread.sleep(5000);
                }

                double newDistance = measureDistance();

                if (lastDistance != -1 &&
                        (System.currentTimeMillis() - lastChangeTime >= 5000 ||
                                Math.abs(newDistance - lastDistance) < 1.0)) {

                    displayCuriousModeScreen("No movement -> pause and change direction", newDistance);

                    Thread.sleep(1000);
                    swiftBot.move(40, -19, 1100);

                    lastChangeTime = System.currentTimeMillis();
                }

                lastDistance = newDistance;

                if (checkEncounterThresholdAndMaybePrompt()) {
                    if (!currentMode.equalsIgnoreCase("Curious")) {
                        return;
                    }
                }

                Thread.sleep(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String takePictureAndRecord() {
        try {
            BufferedImage bwImage = swiftBot.takeGrayscaleStill(ImageSize.SQUARE_480x480);

            if (bwImage == null) {
                displayError("Image is null.");
                return null;
            }

            String imagesDirPath = System.getProperty("user.dir") + File.separator + "images";
            File imagesDir = new File(imagesDirPath);

            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String filename = imagesDirPath + File.separator + "bwImage_" + System.currentTimeMillis() + ".png";

            ImageIO.write(bwImage, "png", new File(filename));

            currentModeImagePaths.add(filename);
            currentModeEncounters++;

            if (currentModeEncounters == 1) {
                currentModeEncountersWindowStart = System.currentTimeMillis();
            }

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
            displayInfo("OBJECT DETECTED", "More than " + ENCOUNTER_THRESHOLD + " objects detected in under 5 minutes while in mode: " + currentMode);

            while (true) {
                displayModeChangePromptScreen();

                String input = scanner.nextLine().trim();
                attachButtonXListener();

                if (input.equals("2")) {
                    terminate = true;
                    return true;
                } else if (input.equals("1")) {
                    while (true) {
                        displayInfo("MODE SELECTED", "Choose new mode: 1 = Curious, 2 = Scaredy, 3 = Dubious");
                        displayInfo("MODE SELECTED", "Enter choice (1/2/3):");

                        String modeChoice = scanner.nextLine().trim();

                        if (modeChoice.equals("1")) {
                            switchToMode("Curious");
                            return true;
                        } else if (modeChoice.equals("2")) {
                            switchToMode("Scaredy");
                            return true;
                        } else if (modeChoice.equals("3")) {
                            switchToMode("Dubious");
                            return true;
                        } else {
                            displayInvalidInputScreen("INPUT ERROR: Invalid choice. Please enter 1, 2 or 3.");
                        }
                    }
                } else {
                    displayInvalidInputScreen("INPUT ERROR: Please choose either 1 or 2, or press Button X on the SwiftBot.");
                }
            }
        }

        return false;
    }

    private static void switchToMode(String newMode) {
        long duration = System.currentTimeMillis() - currentModeStart;

        sessionSummaries.add(new ModeSummary(currentMode, duration, currentModeEncounters, new ArrayList<>(currentModeImagePaths)));

        currentMode = newMode;
        currentModeStart = System.currentTimeMillis();
        currentModeEncounters = 0;
        currentModeImagePaths = new ArrayList<>();
        currentModeEncountersWindowStart = currentModeStart;

        displayModeSelectedScreen(currentMode.toUpperCase() + " MODE ACTIVE");
        runMode(currentMode);
    }

    public static void moveForward(double distance) {
        int time = (int) ((((distance - 30) / 18.50) * 1000));

        try {
            swiftBot.move(100, 100, Math.abs(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void moveBackward(double distance) {
        double x = 30 - distance;
        int time = (int) (((x / 23.78) * 1000));

        try {
            swiftBot.move(-100, -100, Math.abs(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setUnderlights(int r, int g, int b) {
        int[] colour = new int[]{r, g, b};

        Underlight[] underlights = {
                Underlight.BACK_LEFT,
                Underlight.BACK_RIGHT,
                Underlight.MIDDLE_LEFT,
                Underlight.MIDDLE_RIGHT,
                Underlight.FRONT_LEFT,
                Underlight.FRONT_RIGHT
        };

        try {
            for (Underlight underlight : underlights) {
                swiftBot.setUnderlight(underlight, colour);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void writeFinalLogAndExit() {
        if (sessionFinished) {
            return;
        }

        sessionFinished = true;
        terminate = true;

        displayInfo("LOGGING", "Writing to log file...");

        try {
            long now = System.currentTimeMillis();

            if (currentMode != null && !currentMode.isEmpty()) {
                long duration = now - currentModeStart;
                sessionSummaries.add(new ModeSummary(currentMode, duration, currentModeEncounters, new ArrayList<>(currentModeImagePaths)));
            }

            String logsDirPath = System.getProperty("user.dir") + File.separator + "logs";
            File logsDir = new File(logsDirPath);

            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            Calendar date = Calendar.getInstance();
            String timestampForName = String.valueOf(date.getTimeInMillis());
            String logFilePath = logsDirPath + File.separator + "log_" + timestampForName + ".txt";

            BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath));

            bw.write("SwiftBot session log - " + date.getTime().toString());
            bw.newLine();
            bw.newLine();

            for (ModeSummary modeSummary : sessionSummaries) {
                bw.write("Mode: " + modeSummary.modeName);
                bw.newLine();

                bw.write("Duration (ms): " + modeSummary.durationMs);
                bw.newLine();

                bw.write("Object encounters: " + modeSummary.encounters);
                bw.newLine();

                bw.write("Image files:");
                bw.newLine();

                if (modeSummary.imagePaths != null && !modeSummary.imagePaths.isEmpty()) {
                    for (String imagePath : modeSummary.imagePaths) {
                        bw.write("  " + imagePath);
                        bw.newLine();
                    }
                } else {
                    bw.write("  (no images)");
                    bw.newLine();
                }

                bw.newLine();
            }

            bw.flush();
            bw.close();

            displayLogSavedScreen(logFilePath);

            List<String> allImages = new ArrayList<>();

            for (ModeSummary modeSummary : sessionSummaries) {
                if (modeSummary.imagePaths != null) {
                    allImages.addAll(modeSummary.imagePaths);
                }
            }

            backupSessionData(logFilePath, allImages);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void backupSessionData(String logFilePath, List<String> imagePaths) throws java.io.IOException {
        String backupDirPath = System.getProperty("user.dir") + File.separator + "backup_" + System.currentTimeMillis();
        Path backupDir = Paths.get(backupDirPath);

        Files.createDirectories(backupDir);

        displayInfo("BACKUP", "Backup folder created: " + backupDirPath);

        if (logFilePath != null && !logFilePath.isEmpty()) {
            Path sourceLog = Paths.get(logFilePath);

            if (Files.exists(sourceLog)) {
                Path targetLog = backupDir.resolve(sourceLog.getFileName());

                Files.copy(sourceLog, targetLog, StandardCopyOption.REPLACE_EXISTING);
                displayInfo("BACKUP", "Log file backed up: " + targetLog);
            }
        }

        if (imagePaths != null) {
            int count = 0;

            for (String imagePath : imagePaths) {
                Path sourceImage = Paths.get(imagePath);

                if (Files.exists(sourceImage)) {
                    String fileName = count + "_" + sourceImage.getFileName().toString();
                    Path targetImage = backupDir.resolve(fileName);

                    Files.copy(sourceImage, targetImage, StandardCopyOption.REPLACE_EXISTING);
                    displayInfo("BACKUP", "Image backed up: " + targetImage);

                    count++;
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
        System.out.println(CYAN + "=====================================================================================" + RESET);
        System.out.println();
        System.out.println(CYAN + "  __  __  ____  _____  ______      _____  _____          _   _ " + RESET);
        System.out.println(CYAN + " |  \\/  |/ __ \\|  __ \\|  ____|    / ____|/ ____|   /\\   | \\ | |" + RESET);
        System.out.println(CYAN + " | \\  / | |  | | |  | | |__      | (___ | |       /  \\  |  \\| |" + RESET);
        System.out.println(CYAN + " | |\\/| | |  | | |  | |  __|      \\___ \\| |      / /\\ \\ | . ` |" + RESET);
        System.out.println(CYAN + " | |  | | |__| | |__| | |____     ____) | |____ / ____ \\| |\\  |" + RESET);
        System.out.println(CYAN + " |_|  |_|\\____/|_____/|______|   |_____/ \\_____/_/    \\_\\_| \\_|" + RESET);
        System.out.println();
        System.out.println(CYAN + "=====================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Scan QR code to choose operating mode." + RESET);
        System.out.println(WHITE + "Accepted QR values :" + RESET);
        System.out.println(CYAN + "Curious SwiftBot | Scaredy SwiftBot | Dubious SwiftBot | Exit" + RESET);
        System.out.println();
    }

    private static void displayModeSelectedScreen(String modeLabel) {
        System.out.println();
        System.out.println(GREEN + "==================================================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + " __  __  ____  _____  ______      _____ ______ _      ______ _____ _______ ______ _____  " + RESET);
        System.out.println(GREEN + "|  \\/  |/ __ \\|  __ \\|  ____|    / ____|  ____| |    |  ____/ ____|__   __|  ____|  __ \\ " + RESET);
        System.out.println(GREEN + "| \\  / | |  | | |  | | |__      | (___ | |__  | |    | |__ | |       | |  | |__  | |  | |" + RESET);
        System.out.println(GREEN + "| |\\/| | |  | | |  | |  __|      \\___ \\|  __| | |    |  __|| |       | |  |  __| | |  | |" + RESET);
        System.out.println(GREEN + "| |  | | |__| | |__| | |____     ____) | |____| |____| |___| |____   | |  | |____| |__| |" + RESET);
        System.out.println(GREEN + "|_|  |_|\\____/|_____/|______|   |_____/|______|______|______\\_____|  |_|  |______|_____/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "==================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "MODE SELECTED : " + GREEN + modeLabel + RESET);
        System.out.println(WHITE + "SYSTEM STATUS : " + GREEN + "READY" + RESET);
        System.out.println();
    }

    private static void displayCuriousModeScreen(String status, double distance) {
        System.out.println();
        System.out.println(GREEN + "=============================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + "   _____ _    _ _____  _____ ____   _    _  _____ " + RESET);
        System.out.println(GREEN + "  / ____| |  | |  __ \\|_   _/ __ \\ | |  | |/ ____|" + RESET);
        System.out.println(GREEN + " | |    | |  | | |__) | | || |  | || |  | | (___  " + RESET);
        System.out.println(GREEN + " | |    | |  | |  _  /  | || |  | || |  | |\\___ \\ " + RESET);
        System.out.println(GREEN + " | |____| |__| | | \\ \\ _| || |__| || |__| |____) |" + RESET);
        System.out.println(GREEN + "  \\_____|\\____/|_|  \\_\\_____\\____/  \\____/|_____/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "=============================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "CURIOUS MODE ACTIVE" + RESET);
        System.out.println(WHITE + "Status          : " + GREEN + status + RESET);
        System.out.println(WHITE + "Distance        : " + GREEN + String.format("%.2f", distance) + " cm" + RESET);
        System.out.println();
    }

    private static void displayScaredyModeScreen(String status) {
        System.out.println();
        System.out.println(RED + "===============================================================================" + RESET);
        System.out.println();
        System.out.println(RED + "   _____  _____          _______ _______     __" + RESET);
        System.out.println(RED + "  / ____|/ ____|   /\\   |__   __|  __ \\ \\   / /" + RESET);
        System.out.println(RED + " | (___ | |       /  \\     | |  | |__) \\ \\_/ / " + RESET);
        System.out.println(RED + "  \\___ \\| |      / /\\ \\    | |  |  _  / \\   /  " + RESET);
        System.out.println(RED + "  ____) | |____ / ____ \\   | |  | | \\ \\  | |   " + RESET);
        System.out.println(RED + " |_____/ \\_____/_/    \\_\\  |_|  |_|  \\_\\ |_|   " + RESET);
        System.out.println();
        System.out.println(RED + "===============================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "SCAREDY MODE ACTIVE" + RESET);
        System.out.println(WHITE + "Status          : " + RED + status + RESET);
        System.out.println();
    }

    private static void displayDubiousModeScreen(int randomChoice, String selectedMode) {
        System.out.println();
        System.out.println(YELLOW + "====================================================================================" + RESET);
        System.out.println();
        System.out.println(YELLOW + "  _____  _    _ ____ _____ ____  _    _  _____ " + RESET);
        System.out.println(YELLOW + " |  __ \\| |  | |  _ \\_   _/ __ \\| |  | |/ ____|" + RESET);
        System.out.println(YELLOW + " | |  | | |  | | |_) || || |  | | |  | | (___  " + RESET);
        System.out.println(YELLOW + " | |  | | |  | |  _ < | || |  | | |  | |\\___ \\ " + RESET);
        System.out.println(YELLOW + " | |__| | |__| | |_) || || |__| | |__| |____) |" + RESET);
        System.out.println(YELLOW + " |_____/ \\____/|____/_____\\____/ \\____/|_____/ " + RESET);
        System.out.println();
        System.out.println(YELLOW + "====================================================================================" + RESET);
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
        System.out.println(BLUE + "==================================================================================" + RESET);
        System.out.println();
        System.out.println(BLUE + " _   _  ____     ____  ____       _ ______ _____ _______ " + RESET);
        System.out.println(BLUE + "| \\ | |/ __ \\   / __ \\|  _ \\     | |  ____/ ____|__   __|" + RESET);
        System.out.println(BLUE + "|  \\| | |  | | | |  | | |_) |    | | |__ | |       | |   " + RESET);
        System.out.println(BLUE + "| . ` | |  | | | |  | |  _ < _   | |  __|| |       | |   " + RESET);
        System.out.println(BLUE + "| |\\  | |__| | | |__| | |_) | |__| | |___| |____   | |   " + RESET);
        System.out.println(BLUE + "|_| \\_|\\____/   \\____/|____/ \\____/|______\\_____|  |_|   " + RESET);
        System.out.println();
        System.out.println(BLUE + "==================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + modeName + RESET);
        System.out.println(WHITE + "Status          : " + BLUE + "No object in active range. Wandering/searching..." + RESET);
        System.out.println();
    }

    private static void displayModeChangePromptScreen() {
        System.out.println();
        printBorder(YELLOW);
        System.out.println(YELLOW + "MODE CHANGE PROMPT" + RESET);
        System.out.println(WHITE + "Enter 1 to change mode, 2 to terminate program, or press X to terminate." + RESET);
        printBorder(YELLOW);
        System.out.print(WHITE + "Enter choice: " + RESET);
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

    private static class ModeSummary {
        String modeName;
        long durationMs;
        int encounters;
        List<String> imagePaths;

        ModeSummary(String modeName, long durationMs, int encounters, List<String> imagePaths) {
            this.modeName = modeName;
            this.durationMs = durationMs;
            this.encounters = encounters;
            this.imagePaths = imagePaths;
        }
    }
}
