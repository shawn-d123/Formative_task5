/**
 * CodeGenerator.java
 * This class is responsible for generating the secret code
 * that the player needs to guess in the Mastermind game.
 * The code contains no repeating colours.
 */
public class CodeGenerator {

    // The six possible colours available in the game
    // R=Red, G=Green, B=Blue, Y=Yellow, O=Orange, P=Pink
    private static final char[] COLOURS = {'R', 'G', 'B', 'Y', 'O', 'P'};

    /**
     * Generates a random secret code of the specified length.
     * No colour appears more than once in the generated code.
     * @param codeLength the number of colours in the code (3-6)
     * @return the generated code as a string e.g. "RGBY"
     */
    public String generateCode(int codeLength) {

        // StringBuilder to build the code one colour at a time
        StringBuilder code = new StringBuilder();

        // Boolean array to track which colours have already been used
        // Index 0 = R, 1 = G, 2 = B, 3 = Y, 4 = O, 5 = P
        boolean[] used = new boolean[COLOURS.length];

        // Keep picking random colours until the code is the required length
        while (code.length() < codeLength) {

            // Generate a random index between 0 and 5
            int randomIndex = (int) (Math.random() * COLOURS.length);

            // Only add this colour if it has not been used already
            if (!used[randomIndex]) {
                code.append(COLOURS[randomIndex]);
                // Mark this colour as used so it cannot be picked again
                used[randomIndex] = true;
            }
        }

        return code.toString();
    }
}