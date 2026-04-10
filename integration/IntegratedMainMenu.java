package integration;

import java.util.Scanner;

import src_Dance_Shumail.SwiftBotDance;
import src_DetectObject_chris.Detect_Object;
import src_DrawShape_Miguel.DrawShape;
import src_MasterMind_Emanuel.Main;
import src_NoughtsAndCrosses_Prahlad.NoughtsAndCrosses;
import src_SearchForLight_Abid.SearchForLight;
import src_SnakeAndLadder_Minhaj.SnakeandLadder;
import src_SpyBot_alwyn.SpyBot;
import src_TrafficLights_shawn.TrafficLightMainController;
import src_ZigZag_Shamez.ZigZag;

public class IntegratedMainMenu {

    // Colour constants for the command line UI
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String WHITE = "\u001B[37m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean isRunning = true;

        while (isRunning) {
            displayMenu();
            int choice = readChoice(scanner);

            if (choice == 11) {
                System.out.println();
                System.out.println(RED + "Terminating The SwiftBot launcher..." + RESET);
                isRunning = false;
                continue;
            }

            callChosenTask(choice);
        }

        scanner.close();
    }

    private static void displayMenu() {
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
        System.out.println(WHITE + "11. Exit" + RESET);
        System.out.println();

        System.out.print(WHITE + "Enter an option 1 to 11: " + RESET);
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

    private static void callChosenTask(int choice) {
        String taskName = "";

        try {
            switch (choice) {
                case 1:
                    taskName = "Master Mind";
                    displayLaunchBanner(taskName);
                    Main.main(new String[]{});
                    break;
                case 2:
                    taskName = "Zigzag";
                    displayLaunchBanner(taskName);
                    ZigZag.main(new String[]{});
                    break;
                case 3:
                    taskName = "Snakes and Ladders";
                    displayLaunchBanner(taskName);
                    SnakeandLadder.main(new String[]{});
                    break;
                case 4:
                    taskName = "Traffic Light";
                    displayLaunchBanner(taskName);
                    TrafficLightMainController.main(new String[]{});
                    break;
                case 5:
                    taskName = "SpyBot";
                    displayLaunchBanner(taskName);
                    SpyBot.main(new String[]{});
                    break;
                case 6:
                    taskName = "Draw Shape";
                    displayLaunchBanner(taskName);
                    DrawShape.main(new String[]{});
                    break;
                case 7:
                    taskName = "Noughts and Crosses";
                    displayLaunchBanner(taskName);
                    NoughtsAndCrosses.main(new String[]{});
                    break;
                case 8:
                    taskName = "Search for Light";
                    displayLaunchBanner(taskName);
                    SearchForLight.main(new String[]{});
                    break;
                case 9:
                    taskName = "Dance";
                    displayLaunchBanner(taskName);
                    SwiftBotDance.main(new String[]{});
                    break;
                case 10:
                    taskName = "Detect Object";
                    displayLaunchBanner(taskName);
                    Detect_Object.main(new String[]{});
                    break;
                default:
                    throw new IllegalArgumentException("Invalid menu option: " + choice + ". Please choose a valid option.");
            }
        } catch (Throwable unexpected) {
            System.out.println();
            System.out.println(RED + "Task error for: " + taskName + RESET);
        } finally {
            System.out.println();
            System.out.println(WHITE + "Returning to main menu..." + RESET);
            System.out.println();
        }
    }

    private static void displayLaunchBanner(String taskName) {
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Launching: " + GREEN + taskName + RESET);
        System.out.println();
        System.out.println(WHITE + "Please wait..." + RESET);
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);
    }
}
