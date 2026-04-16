package integration;

import java.util.Scanner;

// imports all the tasks
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


// this class is the main menu for the SwiftBot launcher, it allows the user to select which task they want to run and then calls the main method of that task
public class IntegratedMainMenu {

    // Colour constants for the CLI menu
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String WHITE = "\u001B[37m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean isRunning = true;

        while (isRunning) {
            displayMainMenu();
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

    // menu screen to select a task
    private static void displayMainMenu() {
        System.out.println();
        System.out.println(GREEN + "================================================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + " ______        _____ _____ _____ ____   ___ _____    __  __    _    ___ _   _    __  __ _____ _   _ _   _ " + RESET);
        System.out.println(GREEN + "/ ___\\ \\      / /_ _|  ___|_   _| __ ) / _ \\_   _|  |  \\/  |  / \\  |_ _| \\ | |  |  \\/  | ____| \\ | | | | |" + RESET);
        System.out.println(GREEN + "\\___ \\\\ \\ /\\ / / | || |_    | | |  _ \\| | | || |    | |\\/| | / _ \\  | ||  \\| |  | |\\/| |  _| |  \\| | | | |" + RESET);
        System.out.println(GREEN + " ___) |\\ V  V /  | ||  _|   | | | |_) | |_| || |    | |  | |/ ___ \\ | || |\\  |  | |  | | |___| |\\  | |_| |" + RESET);
        System.out.println(GREEN + "|____/  \\_/\\_/  |___|_|     |_| |____/ \\___/ |_|    |_|  |_/_/   \\_\\___|_| \\_|  |_|  |_|_____|_| \\_|\\___/ " + RESET);
        System.out.println();
        System.out.println(GREEN + "================================================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Please enter a task to run from the menu below." + RESET);
        System.out.println();
        System.out.println(WHITE + "[1]  Master Mind" + RESET);
        System.out.println(WHITE + "[2]  Zigzag" + RESET);
        System.out.println(WHITE + "[3]  Snakes and Ladders" + RESET);
        System.out.println(WHITE + "[4]  Traffic Light" + RESET);
        System.out.println(WHITE + "[5]  SpyBot" + RESET);
        System.out.println(WHITE + "[6]  Draw Shape" + RESET);
        System.out.println(WHITE + "[7]  Noughts and Crosses" + RESET);
        System.out.println(WHITE + "[8]  Search for Light" + RESET);
        System.out.println(WHITE + "[9]  Dance" + RESET);
        System.out.println(WHITE + "[10] Detect Object" + RESET);
        System.out.println(WHITE + "[11] Exit" + RESET);
        System.out.println();

        System.out.print(WHITE + "Enter an option 1 to 11: " + RESET);
    }

    // reads the user's choice from the menu and validates it
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
            System.out.println(RED + "Invalid input chosen, Please enter a number from 1 to 11." + RESET);
            System.out.print(WHITE + "Enter an option 1 to 11 : " + RESET);
        }
    }

    // calls the selected task
    private static void callChosenTask(int choice) {
        String taskName = "";

        try {
            switch (choice) {
                case 1:
                    taskName = "Master Mind";
                    displayChosenTaskLaunchScreen(taskName);
                    Main.main(new String[]{});
                    break;
                case 2:
                    taskName = "Zigzag";
                    displayChosenTaskLaunchScreen(taskName);
                    ZigZag.main(new String[]{});
                    break;
                case 3:
                    taskName = "Snakes and Ladders";
                    displayChosenTaskLaunchScreen(taskName);
                    SnakeandLadder.main(new String[]{});
                    break;
                case 4:
                    taskName = "Traffic Light";
                    displayChosenTaskLaunchScreen(taskName);
                    TrafficLightMainController.main(new String[]{});
                    break;
                case 5:
                    taskName = "SpyBot";
                    displayChosenTaskLaunchScreen(taskName);
                    SpyBot.main(new String[]{});
                    break;
                case 6:
                    taskName = "Draw Shape";
                    displayChosenTaskLaunchScreen(taskName);
                    DrawShape.main(new String[]{});
                    break;
                case 7:
                    taskName = "Noughts and Crosses";
                    displayChosenTaskLaunchScreen(taskName);
                    NoughtsAndCrosses.main(new String[]{});
                    break;
                case 8:
                    taskName = "Search for Light";
                    displayChosenTaskLaunchScreen(taskName);
                    SearchForLight.main(new String[]{});
                    break;
                case 9:
                    taskName = "Dance";
                    displayChosenTaskLaunchScreen(taskName);
                    SwiftBotDance.main(new String[]{});
                    break;
                case 10:
                    taskName = "Detect Object";
                    displayChosenTaskLaunchScreen(taskName);
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

    // Displays the launch screen for the chosen task
    private static void displayChosenTaskLaunchScreen(String taskName) {
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);
        System.out.println();
        System.out.println(GREEN + "  _        _   _   _ _   _  ____ _   _ ___ _   _  ____    _____  _    ____  _  __" + RESET);
        System.out.println(GREEN + " | |      / \\ | | | | \\ | |/ ___| | | |_ _| \\ | |/ ___|  |_   _|/ \\  / ___|| |/ /" + RESET);
        System.out.println(GREEN + " | |     / _ \\| | | |  \\| | |   | |_| || ||  \\| | |  _     | | / _ \\ \\___ \\| ' / " + RESET);
        System.out.println(GREEN + " | |___ / ___ \\ |_| | |\\  | |___|  _  || || |\\  | |_| |    | |/ ___ \\ ___) | . \\ " + RESET);
        System.out.println(GREEN + " |_____/_/   \\_\\___/|_| \\_|\\____|_| |_|___|_| \\_|\\____|    |_/_/   \\_\\____/|_|\\_\\" + RESET);
        System.out.println();
        System.out.println(GREEN + "========================================================================================" + RESET);
        System.out.println();
        System.out.println(WHITE + "Launching: " + GREEN + taskName + RESET);
        System.out.println();
    }
}
