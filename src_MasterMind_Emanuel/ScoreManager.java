package src_MasterMind_Emanuel;

import swiftbot.*;

// ScoreManager.java
// This class tracks the scores for both the player and the computer
// across multiple games of Mastermind.
public class ScoreManager {

    // The number of games the player has won
    private int playerScore;

    // The number of games the computer has won
    private int computerScore;

    // Constructor - both scores start at zero at the beginning of a session
    public ScoreManager() {
        this.playerScore = 0;
        this.computerScore = 0;
    }

    // Increments the player's score by one
    // Called when the player successfully guesses the secret code
    public void addPlayerWin() {
        playerScore++;
    }

    // Increments the computer's score by one
    // Called when the player runs out of guesses without finding the code
    public void addComputerWin() {
        computerScore++;
    }

    // Returns the player's current score
    public int getPlayerScore() {
        return playerScore;
    }

    // Returns the computer's current score
    public int getComputerScore() {
        return computerScore;
    }

    // Returns a formatted string showing both scores
    // For example "You: 2 | Computer: 1"
    public String getScoreDisplay() {
        return "You: " + playerScore + " | Computer: " + computerScore;
    }

    // Resets both scores back to zero
    // Not currently used but provided for future use
    public void resetScores() {
        playerScore = 0;
        computerScore = 0;
    }
}
