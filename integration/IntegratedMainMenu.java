package integration;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class IntegratedMainMenu {

    // Colour constants for the command line UI
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String WHITE = "\u001B[37m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readChoice(scanner);

            if (choice == 11) {
                System.out.println();
                System.out.println(RED + "Exiting integrated launcher." + RESET);
                running = false;
                continue;
            }

            String taskName = taskNameFor(choice);
            String entryClass = entryClassFor(choice);
            launchTask(taskName, entryClass);
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println(GREEN + "=======================================================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + "   _____      _____ ___ _____ ___  ___ _____    __  __   _   ___ _  _    __  __ ___ _  _ _   _ " + RESET);
        System.out.println(GREEN + "  / __\\\\ \\    / /_ _| __|_   _| _ )/ _ \\\\_   _|  |  \\/  | /_\\\\ |_ _| \\\\| |  |  \\/  | __| \\\\| | | | |" + RESET);
        System.out.println(GREEN + "  \\\\__ \\\\\\\\ \\/\\\\/ / | || _|  | | | _ \\\\ (_) || |    | |\\\\/| |/ _ \\\\ | || .` |  | |\\\\/| | _|| .` | |_| |" + RESET);
        System.out.println(GREEN + "  |___/ \\\\_/\\\\_/ |___|_|   |_| |___/\\\\___/ |_|    |_|  |_/_/ \\\\_\\\\___|_|\\\\_|  |_|  |_|___|_|\\\\_|\\\\___/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "========================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Welcome to the integrated SwiftBot launcher." + RESET);
        System.out.println(WHITE + "Please choose a task to run from the menu below." + RESET);
        System.out.println();
        System.out.println(WHITE + "1.  Master Mind" + RESET);
        System.out.println(WHITE + "2.  Zigzag" + RESET);
        System.out.println(WHITE + "3.  Snakes and Ladders" + RESET);
        System.out.println(WHITE + "4.  Traffic Light" + RESET);
        System.out.println(WHITE + "5.  SpyBot" + RESET);
        System.out.println(WHITE + "6.  Draw Shape" + RESET);
        System.out.println(WHITE + "7.  Noughts and Crosses" + RESET);
        System.out.println(WHITE + "8.  Search for Light" + RESET);
        System.out.println(WHITE + "9.  Dance" + RESET);
        System.out.println(WHITE + "10. Detect Object" + RESET);
        System.out.println(RED + "11. Exit" + RESET);
        System.out.println();

        System.out.print(WHITE + "Select an option (1-11): " + RESET);
    }

    private static int readChoice(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                if (choice >= 1 && choice <= 11) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
                // keep looping until the user enters a valid number
            }

            System.out.println();
            System.out.println(RED + "Invalid input. Please enter a number from 1 to 11." + RESET);
            System.out.print(WHITE + "Select an option (1-11): " + RESET);
        }
    }

    private static String taskNameFor(int choice) {
        switch (choice) {
            case 1:
                return "Master Mind";
            case 2:
                return "Zigzag";
            case 3:
                return "Snakes and Ladders";
            case 4:
                return "Traffic Light";
            case 5:
                return "SpyBot";
            case 6:
                return "Draw Shape";
            case 7:
                return "Noughts and Crosses";
            case 8:
                return "Search for Light";
            case 9:
                return "Dance";
            case 10:
                return "Detect Object";
            default:
                throw new IllegalArgumentException("Unsupported menu option: " + choice);
        }
    }

    private static String entryClassFor(int choice) {
        switch (choice) {
            case 1:
                return "src_MasterMind_Emanuel.Main";
            case 2:
                return "src_ZigZag_Shamez.ZigZag";
            case 3:
                return "src_SnakeAndLadder_Minhaj.SnakeandLadder";
            case 4:
                return "src_TrafficLights_shawn.TrafficLightMainController";
            case 5:
                return "src_SpyBot_alwyn.SpyBot";
            case 6:
                return "src_DrawShape_Miguel.DrawShape";
            case 7:
                return "src_NoughtsAndCrosses_Prahlad.NoughtsAndCrosses";
            case 8:
                return "src_SearchForLight_Abid.SearchForLight";
            case 9:
                return "src_Dance_Shumail.SwiftBotDance";
            case 10:
                return "src_DetectObject_chris.Detect_Object";
            default:
                throw new IllegalArgumentException("Unsupported menu option: " + choice);
        }
    }

    private static void launchTask(String taskName, String entryClass) {
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Launching: " + GREEN + taskName + RESET);
        System.out.println();
        System.out.println(WHITE + "Please wait..." + RESET);
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);

        ProcessBuilder processBuilder = new ProcessBuilder(
                resolveJavaCommand(),
                "-cp",
                System.getProperty("java.class.path"),
                entryClass
        );

        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            System.out.println();
            System.out.println(GREEN + "========================================================================================" + RESET);
            System.out.println();
            System.out.println(WHITE + taskName + " finished with exit code " + GREEN + exitCode + RESET);
            System.out.println(WHITE + "Returning to main menu..." + RESET);
            System.out.println();
            System.out.println(GREEN + "========================================================================================" + RESET);
        } catch (IOException e) {
            System.out.println();
            System.out.println(RED + "Could not launch task: " + taskName + RESET);
            System.out.println(RED + "I/O error: " + e.getMessage() + RESET);
            System.out.println(WHITE + "Returning to main menu..." + RESET);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println();
            System.out.println(RED + "Task interrupted: " + taskName + RESET);
            System.out.println(WHITE + "Returning to main menu..." + RESET);
        } catch (Throwable unexpected) {
            System.out.println();
            System.out.println(RED + "Unexpected launcher error for task: " + taskName + RESET);
            System.out.println(RED + "Reason: " + unexpected.getClass().getSimpleName() + " - " + unexpected.getMessage() + RESET);
            System.out.println(WHITE + "Returning to main menu..." + RESET);
        }
    }

    private static String resolveJavaCommand() {
        String javaHome = System.getProperty("java.home");
        String basePath = javaHome + File.separator + "bin" + File.separator + "java";

        File javaBinary = new File(basePath);
        if (javaBinary.exists()) {
            return javaBinary.getAbsolutePath();
        }

        File javaBinaryExe = new File(basePath + ".exe");
        if (javaBinaryExe.exists()) {
            return javaBinaryExe.getAbsolutePath();
        }

        return "java";
    }
}