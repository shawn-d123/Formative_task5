package integration;

import java.util.Scanner;

/**
 * Unified launcher menu for all SwiftBot task entries.
 */
public class IntegratedMainMenu {

    @FunctionalInterface
    private interface TaskRunner {
        void run() throws Exception;
    }

    private static final String[] TASK_NAMES = {
            "Master Mind",
            "Zigzag",
            "Snakes and Ladders",
            "Traffic Light",
            "SpyBot",
            "Draw Shape",
            "Noughts and Crosses",
            "Search for Light",
            "Dance",
            "Detect Object"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readValidMenuChoice(scanner);

            if (choice == 11) {
                System.out.println("Exiting integrated launcher.");
                running = false;
                continue;
            }

            String taskName = TASK_NAMES[choice - 1];
            TaskRunner taskRunner = getTaskRunner(choice);
            runTaskSafely(taskName, taskRunner);
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println(" SwiftBot Unified Task Launcher");
        System.out.println("========================================");
        System.out.println("1. Master Mind");
        System.out.println("2. Zigzag");
        System.out.println("3. Snakes and Ladders");
        System.out.println("4. Traffic Light");
        System.out.println("5. SpyBot");
        System.out.println("6. Draw Shape");
        System.out.println("7. Noughts and Crosses");
        System.out.println("8. Search for Light");
        System.out.println("9. Dance");
        System.out.println("10. Detect Object");
        System.out.println("11. Exit");
        System.out.print("Select an option (1-11): ");
    }

    private static int readValidMenuChoice(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 11) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.print("Invalid input. Please enter a number from 1 to 11: ");
        }
    }

    private static TaskRunner getTaskRunner(int choice) {
        switch (choice) {
            case 1:
                return TaskLaunchers::launchMasterMind;
            case 2:
                return TaskLaunchers::launchZigzag;
            case 3:
                return TaskLaunchers::launchSnakesAndLadders;
            case 4:
                return TaskLaunchers::launchTrafficLight;
            case 5:
                return TaskLaunchers::launchSpyBot;
            case 6:
                return TaskLaunchers::launchDrawShape;
            case 7:
                return TaskLaunchers::launchNoughtsAndCrosses;
            case 8:
                return TaskLaunchers::launchSearchForLight;
            case 9:
                return TaskLaunchers::launchDance;
            case 10:
                return TaskLaunchers::launchDetectObject;
            default:
                throw new IllegalArgumentException("Unsupported menu option: " + choice);
        }
    }

    private static void runTaskSafely(String taskName, TaskRunner taskRunner) {
        System.out.println();
        System.out.println("Launching: " + taskName);
        System.out.println("----------------------------------------");

        try {
            taskRunner.run();
            System.out.println("----------------------------------------");
            System.out.println(taskName + " finished. Returning to main menu.");
        } catch (Throwable error) {
            String message = error.getMessage() == null ? "(no message provided)" : error.getMessage();
            System.out.println("----------------------------------------");
            System.out.println("Could not launch task: " + taskName);
            System.out.println("Reason: " + error.getClass().getSimpleName() + " - " + message);
            System.out.println("Returning to main menu.");
        }
    }
}
