import swiftbot.*;

/**
 * Main.java
 * This is the entry point of the Mastermind program.
 * It initialises the SwiftBot and asks the player to select a game mode.
 */
public class Main {

    // The SwiftBot object used to control the robot throughout the program
    static SwiftBotAPI swiftBot;

    // Flag to track whether we are running on the Raspberry Pi or a laptop
    // true = running on Pi with real robot, false = running in test mode on laptop
    static boolean runningOnPi = false;

    public static void main(String[] args) throws InterruptedException {

        // Attempt to connect to the SwiftBot using SwiftBotAPI.INSTANCE
        // If this fails it means we are not on a Raspberry Pi
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
            runningOnPi = true;
            System.out.println("Running on Raspberry Pi - robot controls enabled.");
        } catch (ExceptionInInitializerError e) {
            // SwiftBot could not be initialised - switch to test mode
            runningOnPi = false;
            System.out.println("Not running on Raspberry Pi - entering test mode.");
        }

        // Display welcome message to the user
        System.out.println("==========================================");
        System.out.println("       Welcome to SwiftBot Mastermind!    ");
        System.out.println("==========================================");
        System.out.println();

        // Variable to store which mode the player selected
        // 'D' = Default mode, 'C' = Customised mode
        char selectedMode = ' ';

        if (runningOnPi) {
            // On the Pi, use the physical buttons to select mode
            System.out.println("Press A -> Default Mode");
            System.out.println("Press B -> Customised Mode");

            // Use an array instead of a regular char variable because
            // variables used inside lambdas must be effectively final
            final char[] mode = {' '};

            // Enable button A - sets mode to Default when pressed
            swiftBot.enableButton(Button.A, () -> {
                mode[0] = 'D';
                System.out.println("Default Mode selected!");
                // Disable both buttons so they cannot be pressed again
                swiftBot.disableButton(Button.A);
                swiftBot.disableButton(Button.B);
            });

            // Enable button B - sets mode to Customised when pressed
            swiftBot.enableButton(Button.B, () -> {
                mode[0] = 'C';
                System.out.println("Customised Mode selected!");
                // Disable both buttons so they cannot be pressed again
                swiftBot.disableButton(Button.A);
                swiftBot.disableButton(Button.B);
            });

            // Wait in a loop until a button has been pressed
            // Thread.sleep(50) pauses for 50 milliseconds to avoid overloading the CPU
            while (mode[0] == ' ') {
                Thread.sleep(50);
            }
            selectedMode = mode[0];

        } else {
            // In test mode, use keyboard input instead of buttons
            System.out.println("TEST MODE: Type D for Default, C for Customised, then Enter:");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String input = scanner.next().toUpperCase();

            // Validate the input and assign the selected mode
            if (input.equals("D")) {
                selectedMode = 'D';
                System.out.println("Default Mode selected!");
            } else if (input.equals("C")) {
                selectedMode = 'C';
                System.out.println("Customised Mode selected!");
            } else {
                // If invalid input, default to Default mode
                System.out.println("Invalid input. Defaulting to Default Mode.");
                selectedMode = 'D';
            }
        }

        // Create a GameManager object and start the game
        // Pass the swiftBot, selected mode and runningOnPi flag
        GameManager game = new GameManager(swiftBot, selectedMode, runningOnPi);
        game.startGame();
    }
}