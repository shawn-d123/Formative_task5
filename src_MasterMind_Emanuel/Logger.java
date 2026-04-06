package src_MasterMind_Emanuel;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// Logger.java
// This class handles logging game details to a text file.
// It stores log entries during the game and writes them
// all to a file when the program terminates.
public class Logger {

    // Stores all log entries as strings during the game session
    private ArrayList<String> logEntries;

    // The full file path where the log will be saved
    private String filePath;

    // Constructor for Logger
    // Creates a unique filename using the current date and time
    // and adds a header to the log
    public Logger() {
        this.logEntries = new ArrayList<>();

        // Format the current date and time for use in the filename
        // e.g. "2026-03-17_14-30-00"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Create the log file path with the timestamp in the name
        this.filePath = "mastermind_log_" + timestamp + ".txt";

        // Add a header section to the log entries
        logEntries.add("==========================================");
        logEntries.add("       MASTERMIND GAME LOG");
        logEntries.add("==========================================");
        logEntries.add("Date/Time: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logEntries.add("");
    }

    // Logs the details of a single guess to the log entries list
    // roundNumber - the current round number
    // secretCode - the computer's secret code for this round
    // playerGuess - the player's guess for this turn
    // feedback - the + and - feedback string
    // guessNumber - which guess this was (1, 2, 3 etc.)
    // guessesLeft - how many guesses remain after this one
    public void logGuess(int roundNumber, String secretCode, String playerGuess,
            String feedback, int guessNumber, int guessesLeft) {

        // Add a formatted log entry for this guess
        logEntries.add("Round: " + roundNumber
            + " | Guess " + guessNumber
            + " | Player guessed: " + playerGuess
            + " | Feedback: " + feedback
            + " | Guesses left: " + guessesLeft);
    }

    // Logs the result of a completed round
    // roundNumber - the round that just finished
    // winner - either "Player" or "Computer"
    // secretCode - the secret code used in this round
    // totalGuesses - how many guesses the player used
    // playerScore - the player's current score
    // computerScore - the computer's current score
    public void logRoundResult(int roundNumber, String winner, String secretCode,
            int totalGuesses, int playerScore, int computerScore) {

        // Add a summary of the round result to the log entries
        logEntries.add("--- Round " + roundNumber + " Result ---");
        logEntries.add("Secret code was: " + secretCode);
        logEntries.add("Winner: " + winner);
        logEntries.add("Total guesses used: " + totalGuesses);
        logEntries.add("Score -> Player: " + playerScore
            + " | Computer: " + computerScore);
        logEntries.add("");
    }

    // Writes all stored log entries to the text file
    // Called when the player quits the program
    // Displays the file path so the user knows where to find the log
    public void saveLog() {
        try {
            // Create a FileWriter to write to the log file
            // This will create the file if it does not exist
            FileWriter writer = new FileWriter(filePath);

            // Write each log entry on its own line
            for (String entry : logEntries) {
                writer.write(entry + "\n");
            }

            // Close the writer to ensure all data is saved
            writer.close();
            System.out.println("Log saved to: " + filePath);

        } catch (IOException e) {
            // If the file cannot be written display an error message
            System.out.println("Error saving log file: " + e.getMessage());
        }
    }

    // Returns the file path of the log file
    public String getFilePath() {
        return filePath;
    }
}
