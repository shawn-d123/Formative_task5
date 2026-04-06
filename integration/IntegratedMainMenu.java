package integration;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class IntegratedMainMenu {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readChoice(scanner);

            if (choice == 11) {
                System.out.println("Exiting integrated launcher.");
                running = false;
                continue;
            }

            String taskName = taskNameFor(choice);
            String entryClass = entryClassFor(choice);
            launchTask(taskName, entryClass);
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

    private static int readChoice(Scanner scanner) {
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
        System.out.println("Launching: " + taskName);
        System.out.println("----------------------------------------");

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
            System.out.println("----------------------------------------");
            System.out.println(taskName + " finished with exit code " + exitCode + ". Returning to main menu.");
        } catch (IOException e) {
            System.out.println("----------------------------------------");
            System.out.println("Could not launch task: " + taskName);
            System.out.println("I/O error: " + e.getMessage());
            System.out.println("Returning to main menu.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("----------------------------------------");
            System.out.println("Task interrupted: " + taskName);
            System.out.println("Returning to main menu.");
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
