import swiftbot.*;
import java.awt.image.BufferedImage;

// ColourDetector.java
// This class handles colour card detection using the SwiftBot camera.
// It takes photos of colour cards held in front of the camera,
// calculates the average RGB values and classifies the colour.
public class ColourDetector {

    // The SwiftBot object used to take photos
    private SwiftBotAPI swiftBot;

    // Whether we are running on the Raspberry Pi or in test mode
    private boolean runningOnPi;

    // Constructor for ColourDetector
    // swiftBot - the SwiftBot API object used to take photos
    // runningOnPi - true if running on Raspberry Pi, false if test mode
    public ColourDetector(SwiftBotAPI swiftBot, boolean runningOnPi) {
        this.swiftBot = swiftBot;
        this.runningOnPi = runningOnPi;
    }

    // Scans colour cards one at a time using the camera
    // Prompts the player to hold each card in front of the camera
    // and press button A when ready
    // Returns the full guess string e.g. "RGBY"
    public String scanColourCards(int codeLength) {

        // StringBuilder to build the guess one colour at a time
        StringBuilder guess = new StringBuilder();

        // Scan one card at a time until we have the required number of colours
        for (int i = 0; i < codeLength; i++) {

            System.out.println("Hold up colour card " + (i + 1)
                + " of " + codeLength + " in front of the camera.");
            System.out.println("Press A when ready to scan.");

            // Use an array because lambdas require effectively final variables
            final boolean[] buttonPressed = {false};

            // Enable button A so the player can trigger the photo
            swiftBot.enableButton(Button.A, () -> {
                buttonPressed[0] = true;
                swiftBot.disableButton(Button.A);
            });

            // Wait until button A is pressed before taking the photo
            while (!buttonPressed[0]) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            // Take the photo and detect the colour of the card
            char detectedColour = detectColourFromCamera();
            System.out.println("Detected colour: " + detectedColour);

            // Add the detected colour to the guess string
            guess.append(detectedColour);
        }

        return guess.toString();
    }

    // Takes a photo using the SwiftBot camera and determines the colour
    // Converts the image to a pixel matrix, calculates the average RGB
    // values and classifies the colour using threshold values
    // Returns the detected colour as a char e.g. 'R' for Red
    private char detectColourFromCamera() {

        try {
            // Take a photo using the SwiftBot camera
            // SQUARE_720x720 is used as it is available in SwiftBot API 6.0.0
            BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_720x720);

            // Check if the image was captured successfully
            if (image == null) {
                System.out.println("Error: Could not take photo. Defaulting to Red.");
                return 'R';
            }

            // Calculate the average RGB values across all pixels in the image
            int[] avgRGB = getAverageRGB(image);
            int r = avgRGB[0];
            int g = avgRGB[1];
            int b = avgRGB[2];

            // Print the RGB values so thresholds can be tuned during testing
            System.out.println("Average RGB: R=" + r + " G=" + g + " B=" + b);

            // Classify the colour based on the average RGB values
            return classifyColour(r, g, b);

        } catch (Exception e) {
            System.out.println("Error taking photo: " + e.getMessage());
            return 'R';
        }
    }

    // Calculates the average red, green and blue values across all pixels
    // in the image. This is used to determine the overall colour of the card.
    // image - the BufferedImage captured by the camera
    // Returns an integer array containing [avgRed, avgGreen, avgBlue]
    private int[] getAverageRGB(BufferedImage image) {

        // Running totals for each colour channel
        long totalR = 0;
        long totalG = 0;
        long totalB = 0;

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        // Loop through every pixel in the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                // getRGB returns a single integer containing all colour channel data
                int pixel = image.getRGB(x, y);

                // Extract each colour channel using bit shifting
                // Red is stored in bits 16-23, Green in bits 8-15, Blue in bits 0-7
                int r = (pixel >> 16) & 0xff;  // Extract red channel
                int g = (pixel >> 8) & 0xff;   // Extract green channel
                int b = pixel & 0xff;           // Extract blue channel

                // Add to the running totals
                totalR += r;
                totalG += g;
                totalB += b;
            }
        }

        // Calculate the average for each colour channel
        int avgR = (int) (totalR / totalPixels);
        int avgG = (int) (totalG / totalPixels);
        int avgB = (int) (totalB / totalPixels);

        return new int[]{avgR, avgG, avgB};
    }

    // Classifies the colour of the card based on average RGB values
    // Uses threshold values to determine which of the six game colours
    // the card represents
    // r - the average red value (0-255)
    // g - the average green value (0-255)
    // b - the average blue value (0-255)
    // Returns the classified colour as a char: R, G, B, Y, O or P
    private char classifyColour(int r, int g, int b) {

        // Find the dominant colour channel
        int max = Math.max(r, Math.max(g, b));

        // Red - high red value, low green and blue values
        if (r == max && r > 150 && g < 100 && b < 100) {
            return 'R';
        }

        // Green - high green value, low red and blue values
        if (g == max && g > 150 && r < 100 && b < 100) {
            return 'G';
        }

        // Blue - high blue value, low red and green values
        if (b == max && b > 150 && r < 100 && g < 100) {
            return 'B';
        }

        // Yellow - high red and green values, low blue value
        if (r > 150 && g > 150 && b < 100) {
            return 'Y';
        }

        // Orange - high red, medium green, low blue
        if (r > 150 && g > 80 && g < 160 && b < 80) {
            return 'O';
        }

        // Pink - high red and blue, lower green value
        if (r > 150 && b > 100 && g < 130) {
            return 'P';
        }

        // If no colour could be confidently identified warn the user
        System.out.println("Warning: Could not confidently identify colour.");
        System.out.println("Defaulting to Red. Try holding the card closer.");
        return 'R';
    }
}