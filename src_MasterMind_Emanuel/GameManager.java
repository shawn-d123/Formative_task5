import swiftbot.*;

// GameManager.java
// This class manages the entire game loop for Mastermind.
// It controls the flow of each round, processes guesses,
// generates feedback and tracks the overall game state.
public class GameManager {

    // The SwiftBot object used to control the robot
    private SwiftBotAPI swiftBot;

    // The selected game mode - 'D' for Default, 'C' for Customised
    private char mode;

    // Whether we are running on the Raspberry Pi or in test mode
    private boolean runningOnPi;

    // The secret code the player needs to guess e.g. "RGBY"
    private String secretCode;

    // How many colours are in the code (default is 4, customised is 3-6)
    private int codeLength;

    // How many guesses the player gets (default is 6)
    private int maxGuesses;

    // Manages the player and computer scores across multiple games
    private ScoreManager scoreManager;

    // Handles writing game details to a log file
    private Logger logger;

    // Generates the random secret code at the start of each game
    private CodeGenerator codeGenerator;

    // Handles taking photos and detecting colour cards
    private ColourDetector colourDetector;

    // Constructor for GameManager
    // Initialises all helper objects and sets default game settings
    // swiftBot - the SwiftBot API object
    // mode - the selected game mode ('D' or 'C')
    // runningOnPi - true if running on Raspberry Pi, false if test mode
    public GameManager(SwiftBotAPI swiftBot, char mode, boolean runningOnPi) {
        this.swiftBot = swiftBot;
        this.mode = mode;
        this.runningOnPi = runningOnPi;

        // Set default values for code length and max guesses
        this.codeLength = 4;
        this.maxGuesses = 6;

        // Initialise all helper class objects
        this.scoreManager = new ScoreManager();
        this.logger = new Logger();
        this.codeGenerator = new CodeGenerator();
        this.colourDetector = new ColourDetector(swiftBot, runningOnPi);
    }

    // Starts the game - called from Main.java
    // If customised mode is selected, setup is performed first
    // Then the game loop runs until the player chooses to quit
    public void startGame() {

        // If customised mode, ask the player for their preferred settings
        if (mode == 'C') {
            setupCustomMode();
        }

        // Variables to control the game loop
        boolean keepPlaying = true;
        int roundNumber = 1;

        // Main game loop - keeps running until the player presses X to quit
        while (keepPlaying) {

            System.out.println();
            System.out.println("==========================================");
            System.out.println("              ROUND " + roundNumber);
            System.out.println("==========================================");

            // Play one complete game and get the result
            boolean playerWon = playOneGame(roundNumber);

            // Update the score based on who won this round
            if (playerWon) {
                scoreManager.addPlayerWin();
            } else {
                scoreManager.addComputerWin();
            }

            // Display the updated score to the player
            System.out.println();
            System.out.println("Current Score -> " + scoreManager.getScoreDisplay());

            // Ask the player if they want to play again
            keepPlaying = askPlayAgain();
            roundNumber++;
        }

        // Save the log file and display its location before the program ends
        logger.saveLog();
        System.out.println("Thanks for playing! Goodbye.");
    }

    // Plays one complete game of Mastermind
    // Generates a secret code, gets the player's guesses and provides feedback
    // roundNumber - the current round number for logging purposes
    // Returns true if the player guessed correctly, false if they ran out of guesses
    private boolean playOneGame(int roundNumber) {

        // Generate a new secret code for this round
        secretCode = codeGenerator.generateCode(codeLength);

        System.out.println("I have generated a secret code of " + codeLength + " colours.");
        System.out.println("You have " + maxGuesses + " guesses. Good luck!");
        System.out.println();
        System.out.println("Colours available: R (Red), G (Green), B (Blue),");
        System.out.println("                   Y (Yellow), O (Orange), P (Pink)");
        System.out.println();

        // Track how many guesses have been used and whether the player has won
        int guessesUsed = 0;
        boolean playerWon = false;

        // Keep looping until the player wins or runs out of guesses
        while (guessesUsed < maxGuesses && !playerWon) {

            int guessesLeft = maxGuesses - guessesUsed;
            System.out.println("Guess " + (guessesUsed + 1) + " of " + maxGuesses
                + " (" + guessesLeft + " remaining):");

            // Get the player's guess either from the camera or keyboard
            String playerGuess = getPlayerGuess();
            System.out.println("Your guess: " + playerGuess);

            // Check the guess against the secret code and get + and - feedback
            String feedback = checkGuess(playerGuess, secretCode);
            System.out.println("Feedback: " + feedback);
            System.out.println();

            // Log the details of this guess to the log file
            logger.logGuess(roundNumber, secretCode, playerGuess,
                feedback, guessesUsed + 1, guessesLeft - 1);

            guessesUsed++;

            // Check if the player has guessed all colours correctly
            if (feedback.equals(getFullPlusString())) {
                playerWon = true;
                System.out.println("Congratulations! You cracked the code!");
            }
        }

        // If the player did not win, reveal the secret code
        if (!playerWon) {
            System.out.println("Unlucky! You ran out of guesses.");
            System.out.println("The secret code was: " + secretCode);
        }

        return playerWon;
    }

    // Builds a string of + symbols equal to the code length
    // Used to check if the player has guessed the code correctly
    // For example with codeLength 4 this returns "++++"
    private String getFullPlusString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            sb.append("+");
        }
        return sb.toString();
    }

    // Gets the player's guess either from the camera (on Pi) or keyboard (test mode)
    // Returns the player's guess as a string e.g. "RGBY"
    private String getPlayerGuess() {

        if (runningOnPi) {
            // Use the camera to scan colour cards one at a time
            return colourDetector.scanColourCards(codeLength);
        } else {
            // In test mode allow the player to type their guess
            System.out.print("TEST MODE - Type your guess (e.g. RGBY): ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            return scanner.next().toUpperCase();
        }
    }

    // Checks the player's guess against the secret code
    // Returns feedback using + and - symbols
    // + means the colour is correct and in the right position
    // - means the colour is in the code but in the wrong position
    // + symbols are always displayed before - symbols
    private String checkGuess(String guess, String code) {

        // Counters for exact matches (+) and colour matches in wrong position (-)
        int plusCount = 0;
        int minusCount = 0;

        // Track which positions have already been matched
        // This prevents the same colour being counted twice
        boolean[] codeMatched = new boolean[code.length()];
        boolean[] guessMatched = new boolean[guess.length()];

        // First pass - find exact matches (right colour, right position)
        for (int i = 0; i < code.length(); i++) {
            if (guess.charAt(i) == code.charAt(i)) {
                plusCount++;
                // Mark these positions as matched so they are not counted again
                codeMatched[i] = true;
                guessMatched[i] = true;
            }
        }

        // Second pass - find colour matches in the wrong position
        for (int i = 0; i < guess.length(); i++) {
            // Skip positions that were already matched in the first pass
            if (guessMatched[i]) continue;

            for (int j = 0; j < code.length(); j++) {
                // Skip positions in the code that were already matched
                if (codeMatched[j]) continue;

                if (guess.charAt(i) == code.charAt(j)) {
                    minusCount++;
                    // Mark this code position as matched to avoid counting it again
                    codeMatched[j] = true;
                    break;
                }
            }
        }

        // Build the feedback string with + symbols first then - symbols
        StringBuilder feedback = new StringBuilder();
        for (int i = 0; i < plusCount; i++) feedback.append("+");
        for (int i = 0; i < minusCount; i++) feedback.append("-");

        // If there are no matches at all inform the player
        if (feedback.length() == 0) {
            return "No matches";
        }

        return feedback.toString();
    }

    // Sets up the customised game mode by asking the player for their preferences
    // The player can choose the code length (3-6) and maximum number of guesses (1-10)
    // Input is validated and the player is re-prompted if invalid values are entered
    private void setupCustomMode() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        System.out.println("CUSTOMISED MODE SETUP");

        // Ask for the code length and validate the input
        System.out.println("How many colours in the code? (3-6): ");
        while (true) {
            try {
                int length = Integer.parseInt(scanner.next());
                if (length >= 3 && length <= 6) {
                    codeLength = length;
                    break; // Valid input received, exit the loop
                } else {
                    // Value is out of the valid range
                    System.out.println("Please enter a number between 3 and 6:");
                }
            } catch (NumberFormatException e) {
                // Input was not a number
                System.out.println("Invalid input. Please enter a number between 3 and 6:");
            }
        }

        // Ask for the maximum number of guesses and validate the input
        System.out.println("How many guesses would you like? (1-10): ");
        while (true) {
            try {
                int guesses = Integer.parseInt(scanner.next());
                if (guesses >= 1 && guesses <= 10) {
                    maxGuesses = guesses;
                    break; // Valid input received, exit the loop
                } else {
                    // Value is out of the valid range
                    System.out.println("Please enter a number between 1 and 10:");
                }
            } catch (NumberFormatException e) {
                // Input was not a number
                System.out.println("Invalid input. Please enter a number between 1 and 10:");
            }
        }

        System.out.println("Settings saved! Code length: " + codeLength
            + ", Max guesses: " + maxGuesses);
        System.out.println();
    }

    // Asks the player if they want to play another round
    // On the Pi buttons Y and X are used
    // In test mode keyboard input is used
    // Returns true if the player wants to continue, false if they want to quit
    private boolean askPlayAgain() {

        System.out.println("Play again?");

        if (runningOnPi) {
            System.out.println("Press Y to continue, X to quit.");

            // Use arrays because lambdas require effectively final variables
            final boolean[] keepPlaying = {true};
            final boolean[] buttonPressed = {false};

            // Enable button Y - player wants to continue
            swiftBot.enableButton(Button.Y, () -> {
                keepPlaying[0] = true;
                buttonPressed[0] = true;
                swiftBot.disableButton(Button.Y);
                swiftBot.disableButton(Button.X);
            });

            // Enable button X - player wants to quit
            swiftBot.enableButton(Button.X, () -> {
                keepPlaying[0] = false;
                buttonPressed[0] = true;
                swiftBot.disableButton(Button.Y);
                swiftBot.disableButton(Button.X);
            });

            // Wait until one of the buttons is pressed
            while (!buttonPressed[0]) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            return keepPlaying[0];

        } else {
            // In test mode use keyboard input
            System.out.println("TEST MODE - Type Y to continue, X to quit:");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String input = scanner.next().toUpperCase();
            return input.equals("Y");
        }
    }
}