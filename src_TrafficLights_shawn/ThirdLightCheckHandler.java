/*
 * Tracks the number of valid traffic lights encountered during the run and
 * prompts the user to continue or terminate at every third light via CLI input.
 */


import swiftbot.*;
import java.util.Scanner;

/**
 * Handles the every-third-light checkpoint for the SwiftBot Traffic Light
 * System. Maintains its own light counter and, when a multiple
 * of three is reached, stops the SwiftBot, displays the checkpoint screen,
 * and blocks until the user enters a valid continue (C) or terminate (X).
 */
public class ThirdLightCheckHandler {

    // Number of lights between each checkpoint prompt
    private static final int CHECKPOINT_INTERVAL = 3;

    private final SwiftBotAPI swiftBot;

    /* Running count of total valid lights handled since the start. */
    private int totalLightCount = 0;

    /* Scanner held open for the lifetime of the session to read checkpoint input. */
    private final Scanner scanner;

    /**
     * Creates a ThirdLightCheckHandler with the given SwiftBot instance.
     * @param swiftBot the SwiftBot API instance used to stop movement at checkpoints.
     */
    public ThirdLightCheckHandler(SwiftBotAPI swiftBot) {
        this.swiftBot = swiftBot;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Increments the internal light counter and, if the count is a multiple of
     * CHECKPOINT_INTERVAL, stops the SwiftBot and prompts the user to continue
     * or terminate. Pauses until a valid input C or X is entered.
     * @return true if the user chose to terminate, false to continue the run.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */

    public boolean thridLightHandler() throws InterruptedException {

        totalLightCount++;

        System.out.println("\n----------------------------------------");
        System.out.println("Traffic Lights Encountered: " + totalLightCount);
        System.out.println("----------------------------------------\n");

        // Only trigger the checkpoint prompt on every third light
        if (totalLightCount % CHECKPOINT_INTERVAL == 0) {

            // Stop the bot and clear the reset the state, to ensure no double callings
            swiftBot.stopMove();
            swiftBot.disableUnderlights();
            swiftBot.disableAllButtons();

            TrafficLightMainController.displayThirdLightCheckpointScreen();

            // Keep reading input until the user enters C or X
            while (true) {
                String input = scanner.nextLine().trim().toUpperCase(); // prep input

                if (input.equals("C")) {
                    return false; // Continue the run
                }

                if (input.equals("X")) {
                    return true; // Terminate the run
                }

                // Invalid input so show error UI and re-prompt
                TrafficLightMainController.displayThirdLightCheckpointInvalidInputScreen();
            }
        }

        return false;
    }
}