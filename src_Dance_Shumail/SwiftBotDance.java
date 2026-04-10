package src_Dance_Shumail;

import swiftbot.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ================================================================
 *  SwiftBot Hexadecimal Dance & Number Conversion System
 *  Task 9 – Swift Bot Dance
 * ================================================================
 *
 *  HOW IT WORKS (end-to-end):
 *  ─────────────────────────
 *  1. The robot's camera scans a QR code containing 1–5 hex values
 *     separated by '&'  (e.g. "F&3F&C8").
 *  2. Each hex value is VALIDATED (1–2 hex digits, 0–9 / A–F).
 *     Invalid values are SKIPPED and reported to the user.
 *  3. Each valid hex value is converted to decimal, binary and octal
 *     using CUSTOM algorithms (no built-in Java methods).
 *  4. Speed and LED colour are calculated from those conversions.
 *  5. All conversion info is printed to the console BEFORE moving.
 *  6. LEDs are set and the binary-encoded dance is executed.
 *  7. After the last move the LEDs turn off and the user is prompted
 *     to press Y (continue) or X (exit).
 *  8. On exit, all session hex values are sorted and saved to a file.
 *
 *  A* FEATURE:
 *  ──────────
 *  After pressing Y the user can set a repeat count via SwiftBot
 *  buttons (A = +1, B = confirm). The same dance replays that many
 *  times without changing any conversion results.
 *
 *  API USED (verified from DoesMySwiftBotWork.java):
 *  ──────────────────────────────────────────────────
 *  SwiftBotAPI.INSTANCE              – singleton, obtained once at start-up
 *  swiftBot.move(left, right, ms)    – drive wheels; both + = forward,
 *                                      opposing signs = spin
 *  swiftBot.fillUnderlights(rgb)     – set all 6 underlights to int[]{r,g,b}
 *  swiftBot.disableUnderlights()     – turn off all underlights
 *  swiftBot.enableButton(btn, λ)     – register a one-shot press callback
 *  swiftBot.disableButton(btn)       – cancel a button callback
 *  swiftBot.disableAllButtons()      – cancel all callbacks
 *  swiftBot.setButtonLight(btn, on)  – turn a button LED on / off
 *  swiftBot.getQRImage()             – capture a QR frame (BufferedImage)
 *  swiftBot.decodeQRImage(img)       – decode QR frame → String
 * ================================================================
 */
public class SwiftBotDance {

    // ----------------------------------------------------------------
    // ANSI COLOUR CODES  (same style as DoesMySwiftBotWork.java)
    // ----------------------------------------------------------------
    static final String RESET  = "\u001B[0m";
    static final String CYAN   = "\u001B[36m";
    static final String YELLOW = "\u001B[33m";
    static final String GREEN  = "\u001B[32m";
    static final String RED    = "\u001B[31m";
    static final String WHITE  = "\u001B[37m";
    static final String BOLD   = "\u001B[1m";

    // ----------------------------------------------------------------
    // CONSTANTS
    // ----------------------------------------------------------------

    /** Maximum wheel speed (%) supported by the SwiftBot hardware. */
    static final int MAX_SPEED = 100;

    /** If the octal value is below this, add 50 to bring the speed up. */
    static final int MIN_SPEED_THRESHOLD = 50;

    /** Forward movement duration (ms) when the hex input is 1 digit long. */
    static final int FORWARD_1DIGIT_MS = 1000;

    /** Forward movement duration (ms) when the hex input is 2 digits long. */
    static final int FORWARD_2DIGIT_MS = 500;

    /** Duration (ms) of every spin movement. */
    static final int SPIN_DURATION_MS = 500;

    /** Separator between hex values inside the QR code payload. */
    static final String HEX_SEPARATOR = "&";

    /** Maximum number of hex values accepted from a single QR scan. */
    static final int MAX_HEX_VALUES = 5;

    /** Maximum QR scan retries before giving up. */
    static final int MAX_SCAN_ATTEMPTS = 10;

    // ----------------------------------------------------------------
    // FIELDS
    // ----------------------------------------------------------------

    /**
     * The SwiftBot API singleton.
     * Obtained via SwiftBotAPI.INSTANCE (NOT new SwiftBotAPI()).
     */
    static SwiftBotAPI swiftBot;

    /**
     * Keyboard scanner used for manual hex input when QR scanning fails,
     * or when the user prefers to type values directly.
     * Kept open for the whole program lifetime and closed only on exit.
     */
    static final Scanner keyboardScanner = new Scanner(System.in);

    /**
     * All valid hex values entered in this session, accumulated across
     * every QR scan. Sorted and saved to a file when the user exits.
     */
    static List<String> sessionHexValues = new ArrayList<>();

    // ================================================================
    //  ENTRY POINT  –  Flowchart 1: System Start-Up & Initialisation
    // ================================================================

    /**
     * Program entry point.
     *
     * Overall flow (Flowchart 1):
     *   Start → Initialise System → QR Code Processing
     *        → Movement Execution → Program Completion → End
     */
    public static void main(String[] args) {

        // ── Obtain the SwiftBot singleton ──────────────────────────────
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
        } catch (Exception e) {
            // I2C must be enabled on the Raspberry Pi.
            System.out.println(RED + "\n[ERROR] I2C is disabled! " +
                    "Please ask staff to enable it." + RESET);
            return;
        }

        // ── Welcome banner ─────────────────────────────────────────────
        System.out.printf("""

                %s%s*************************************************************%s
                %s%s*       SWIFTBOT HEXADECIMAL DANCE SYSTEM – TASK 9         *%s
                %s%s*************************************************************%s

                """, CYAN, BOLD, RESET, CYAN, BOLD, RESET, CYAN, BOLD, RESET);

        System.out.println(WHITE + "Press " + GREEN + "Y" +
                WHITE + " on the SwiftBot to begin." + RESET);

        // Block until the user presses Y
        waitForButton(Button.Y);

        // ── Main session loop ──────────────────────────────────────────
        boolean keepRunning = true;

        while (keepRunning) {

            // Flowchart 2: scan QR code and extract validated hex values
            List<String> validHexValues = scanAndParseQRCode();

            if (validHexValues.isEmpty()) {
                System.out.println(YELLOW +
                        "[INFO] No valid hex values found. Please try again." + RESET);
                // Loop back and let the user scan again
                continue;
            }

            // Flowcharts 4–7: for each valid hex value, convert → display → dance
            for (String hex : validHexValues) {

                // Step 1 – Convert (Flowchart 4)
                int    decimal = hexToDecimal(hex);
                String binary  = decimalToBinary(decimal);
                String octal   = decimalToOctal(decimal);

                // Step 2 – Calculate speed and LED colour (Flowchart 6)
                int   speed = calculateSpeed(octal);
                int[] led   = calculateLED(decimal);

                // Step 3 – Display info BEFORE any movement (Flowchart 5)
                displayConversionInfo(hex, decimal, binary, octal, speed, led);

                // Step 4 – Apply LED colour to all underlights
                setUnderlightColour(led[0], led[1], led[2]);

                // Step 5 – Execute the binary-driven dance (Flowchart 7)
                executeDanceRoutine(binary, speed, hex.length());
            }

            // After the final move: LEDs off
            swiftBot.disableUnderlights();
            System.out.println(GREEN + "\n[INFO] All movements complete – LEDs off." + RESET);

            // Flowchart 8: prompt the user to continue or exit
            keepRunning = handleDanceCompletion(validHexValues);
        }

        // Sort and save all session hex values, then exit
        finaliseAndExit();
    }

    // ================================================================
    //  FLOWCHART 2 – QR Code Scanning & Input Parsing
    // ================================================================

    /**
     * Primary entry point for obtaining hex values.
     *
     * Step 1 – tries to read a QR code with the SwiftBot camera
     *          (up to MAX_SCAN_ATTEMPTS retries).
     * Step 2 – if QR scanning fails, automatically falls back to
     *          manualHexInput() so the user can type the values instead.
     *
     * In both cases the raw payload string is validated and only
     * the valid hex tokens are returned.
     *
     * @return List of validated, upper-case hex strings (never null).
     */
    static List<String> scanAndParseQRCode() {

        System.out.println(CYAN + "\n--- QR Code Scanning ---" + RESET);

        String rawInput = "";
        int attempts = 0;

        // ── Step 1: Try the camera ─────────────────────────────────────
        while (attempts < MAX_SCAN_ATTEMPTS) {
            System.out.println(WHITE + "Scanning... (attempt " +
                    (attempts + 1) + "/" + MAX_SCAN_ATTEMPTS + ")" + RESET);

            try {
                // Capture a QR image frame from the SwiftBot camera
                BufferedImage img = swiftBot.getQRImage();

                // Attempt to decode the captured frame into a String
                String decoded = swiftBot.decodeQRImage(img);

                if (decoded != null && !decoded.isEmpty()) {
                    rawInput = decoded.trim();
                    System.out.println(GREEN + "[QR] Decoded: " + rawInput + RESET);
                    break; // Success – leave the retry loop
                }

            } catch (Exception e) {
                // Camera error or no QR found in frame – try again
                System.out.println(YELLOW +
                        "[WARNING] Could not detect QR – retrying..." + RESET);
            }

            attempts++;
        }

        // ── Step 2: Fall back to manual keyboard input if QR failed ───
        if (rawInput.isEmpty()) {
            System.out.println(RED + "[INFO] QR scan failed after " +
                    MAX_SCAN_ATTEMPTS + " attempts." + RESET);
            System.out.println(YELLOW +
                    "[INFO] Switching to manual keyboard input." + RESET);
            rawInput = manualHexInput();

            // If the user still provided nothing useful, return empty
            if (rawInput.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // ── Parse: split on '&', cap, validate ────────────────────────
        String[] tokens = rawInput.split(HEX_SEPARATOR);
        int limit = Math.min(tokens.length, MAX_HEX_VALUES);
        return validateAndFilterValues(tokens, limit);
    }

    /**
     * Prompts the user to type one or more hex values on the keyboard
     * as a fallback when the QR camera scan fails.
     *
     * Format: values separated by '&', e.g.  F   or   1F&3A&C8
     *
     * Rules enforced live (re-prompted until valid format is entered):
     *   • The input must not be blank.
     *   • At most MAX_HEX_VALUES tokens separated by '&'.
     *   • Each token must be 1–2 hex characters (0–9 / A–F, case-insensitive).
     *   • At least ONE valid hex token must be present.
     *
     * Invalid individual tokens are reported and skipped, but the overall
     * input is only re-requested if ZERO valid tokens survive validation.
     *
     * @return A raw input string that contains at least one valid hex token,
     *         ready to be split and passed to validateAndFilterValues().
     */
    static String manualHexInput() {

        System.out.println(CYAN + "\n--- Manual Hex Input ---" + RESET);
        System.out.println(WHITE +
                "Enter 1–" + MAX_HEX_VALUES + " hex value(s) separated by '&'." + RESET);
        System.out.println(WHITE +
                "Each value must be 1–2 characters: digits 0–9 or letters A–F." + RESET);
        System.out.println(WHITE +
                "Examples:  " + GREEN + "F" + WHITE +
                "   or   " + GREEN + "1F&3A&C8" + RESET);

        String rawInput = "";

        // Keep asking until at least one valid hex value is entered
        while (true) {
            System.out.print(YELLOW + "\nEnter hex value(s): " + RESET);
            String line = keyboardScanner.nextLine().trim();

            // ── Guard: blank input ─────────────────────────────────────
            if (line.isEmpty()) {
                System.out.println(RED +
                        "[ERROR] Input cannot be blank. Please try again." + RESET);
                continue;
            }

            // ── Guard: too many '&'-separated tokens ───────────────────
            String[] tokens = line.split(HEX_SEPARATOR);
            if (tokens.length > MAX_HEX_VALUES) {
                System.out.println(RED + "[ERROR] Too many values. Maximum allowed: " +
                        MAX_HEX_VALUES + ". You entered: " + tokens.length +
                        ". Please try again." + RESET);
                continue;
            }

            // ── Pre-check: count how many tokens would survive validation ──
            int validCount = 0;
            for (String token : tokens) {
                if (isValidHex(token.trim().toUpperCase())) {
                    validCount++;
                }
            }

            if (validCount == 0) {
                // Every token is invalid – show guidance and re-prompt
                System.out.println(RED +
                        "[ERROR] No valid hex values found in your input." + RESET);
                System.out.println(WHITE +
                        "  Reminder: valid hex uses digits 0–9 and letters A–F, " +
                        "1–2 characters each." + RESET);
                System.out.println(WHITE +
                        "  Example: " + GREEN + "A&1F&FF" + RESET);
                continue;
            }

            // ── At least one valid token – accept this input ───────────
            rawInput = line;

            // Echo back what was accepted so the user can confirm
            System.out.println(GREEN + "[INPUT] Accepted: " + rawInput + RESET);

            // Warn if some tokens were invalid (they will be skipped later)
            if (validCount < tokens.length) {
                System.out.println(YELLOW +
                        "[WARNING] " + (tokens.length - validCount) +
                        " invalid token(s) in your input will be ignored." + RESET);
            }

            break; // Input is good enough – exit the loop
        }

        return rawInput;
    }

    // ================================================================
    //  FLOWCHART 3 – Hexadecimal Validation & Error Handling
    // ================================================================

    /**
     * Examines each raw token, applies hex validation rules, reports any
     * invalid values to the console, and returns only the valid ones.
     *
     * Flowchart 3 path:
     *   Check Input Length → Validate Hex Characters → Input Valid?
     *     Yes → store and return
     *     No  → Display Error Message → skip token
     *
     * @param tokens Array of raw strings from the QR payload.
     * @param limit  Number of tokens to process (capped at MAX_HEX_VALUES).
     * @return       List of valid, upper-case, trimmed hex strings.
     */
    static List<String> validateAndFilterValues(String[] tokens, int limit) {

        List<String> valid   = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            // Normalise: trim whitespace and convert to upper-case
            // (the spec says letter case must not affect processing)
            String token = tokens[i].trim().toUpperCase();

            if (isValidHex(token)) {
                valid.add(token);
                // Also record in the session history for final file export
                sessionHexValues.add(token);
            } else {
                invalid.add(tokens[i].trim()); // Keep original text for the message
            }
        }

        // Report every ignored value BEFORE any robot movement
        if (!invalid.isEmpty()) {
            System.out.println(RED +
                    "\n[WARNING] The following value(s) were IGNORED (invalid hex):");
            for (String inv : invalid) {
                System.out.println("  x  \"" + inv + "\"");
            }
            System.out.println(RESET);
        }

        // Confirm accepted values
        if (!valid.isEmpty()) {
            System.out.println(GREEN + "[INFO] Valid hex values accepted:");
            for (String v : valid) {
                System.out.println("  ok  " + v);
            }
            System.out.println(RESET);
        }

        return valid;
    }

    /**
     * Returns true if the given string is a legal 1–2 character
     * hexadecimal value (digits 0–9, letters A–F, already upper-case).
     *
     * Two rules (Flowchart 3):
     *   1. String length must be exactly 1 or 2.
     *   2. Every character must be in the set {0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F}.
     *
     * @param hex Upper-case, trimmed candidate string.
     * @return    true if valid, false otherwise.
     */
    static boolean isValidHex(String hex) {
        // Rule 1: must be 1 or 2 characters long
        if (hex == null || hex.length() < 1 || hex.length() > 2) {
            return false;
        }

        // Rule 2: every character must be a valid hex digit
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            boolean isDigit    = (c >= '0' && c <= '9');
            boolean isHexAlpha = (c >= 'A' && c <= 'F');
            if (!isDigit && !isHexAlpha) {
                return false;
            }
        }

        return true;
    }

    // ================================================================
    //  FLOWCHART 4 – Custom Number Conversion
    //  !! NO built-in Java conversion methods (Integer.parseInt, etc.) !!
    // ================================================================

    /**
     * Converts a hexadecimal string to its decimal (base-10) integer.
     *
     * Algorithm – positional notation, base 16:
     *   Traverse each character left to right.
     *   result = result × 16 + digitValue
     *
     *   '0'–'9'  → digitValue = c - '0'       (ASCII subtraction)
     *   'A'–'F'  → digitValue = c - 'A' + 10  (offset by 10)
     *
     * Example: "5A"
     *   i=0 '5' → result = 0×16 + 5  =  5
     *   i=1 'A' → result = 5×16 + 10 = 90
     *
     * @param hex Upper-case, validated hex string (1–2 chars).
     * @return    Decimal integer equivalent.
     */
    static int hexToDecimal(String hex) {
        int result = 0;

        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            int digitValue;

            if (c >= '0' && c <= '9') {
                digitValue = c - '0';          // Numeric digit
            } else {
                digitValue = (c - 'A') + 10;   // Hex letter A–F
            }

            result = result * 16 + digitValue; // Shift one hex place, add digit
        }

        return result;
    }

    /**
     * Converts a non-negative decimal integer to a binary string.
     *
     * Algorithm – repeated division by 2:
     *   Divide by 2 → collect remainder (0 or 1) → repeat until value = 0.
     *   Remainders are collected LSB-first, then reversed for MSB-first output.
     *
     * Example: 90
     *   90÷2=45 r0 | 45÷2=22 r1 | 22÷2=11 r0 | 11÷2=5 r1
     *    5÷2=2  r1 |  2÷2=1  r0 |  1÷2=0  r1
     *   Reversed → "1011010"
     *
     * @param decimal Non-negative integer.
     * @return        Binary string (e.g. "1011010"). Returns "0" for input 0.
     */
    static String decimalToBinary(int decimal) {
        if (decimal == 0) return "0";

        StringBuilder sb = new StringBuilder();
        int value = decimal;

        while (value > 0) {
            sb.append(value % 2); // Remainder is the next bit (LSB first)
            value = value / 2;    // Divide by 2 (right shift)
        }

        return sb.reverse().toString(); // Reverse to get MSB-first string
    }

    /**
     * Converts a non-negative decimal integer to an octal string.
     *
     * Algorithm – repeated division by 8:
     *   Divide by 8 → collect remainder (0–7) → repeat until value = 0.
     *   Remainders are collected LSB-first, then reversed for MSB-first output.
     *
     * Example: 90
     *   90÷8=11 r2 | 11÷8=1 r3 | 1÷8=0 r1
     *   Reversed → "132"
     *
     * @param decimal Non-negative integer.
     * @return        Octal string (e.g. "132"). Returns "0" for input 0.
     */
    static String decimalToOctal(int decimal) {
        if (decimal == 0) return "0";

        StringBuilder sb = new StringBuilder();
        int value = decimal;

        while (value > 0) {
            sb.append(value % 8); // Remainder is the next octal digit (LSB first)
            value = value / 8;    // Divide by 8 (right shift)
        }

        return sb.reverse().toString(); // Reverse to get MSB-first string
    }

    // ================================================================
    //  FLOWCHART 5 – Display Conversion & Movement Information
    // ================================================================

    /**
     * Prints all conversion results and robot parameters to the console
     * BEFORE the SwiftBot begins any movement (required by the spec).
     *
     * Spec examples:
     *   F,  17,  15,    1111, speed =  67, LED colour (red 15,  green 45,  blue 45).
     *   3F, 77,  63,  111111, speed =  77, LED colour (red 63,  green 189, blue 189).
     *   C8, 310, 200, 11001000, speed = 100, LED colour (red 200, green 120, blue 200).
     *
     * @param hex     Original hex string.
     * @param decimal Decimal equivalent.
     * @param binary  Binary equivalent string.
     * @param octal   Octal equivalent string.
     * @param speed   Calculated movement speed.
     * @param led     int[3] – { red, green, blue }.
     */
    static void displayConversionInfo(String hex, int decimal, String binary,
                                      String octal, int speed, int[] led) {

        System.out.println(CYAN + "\n--- Conversion Info: " + hex + " ---" + RESET);
        System.out.println(WHITE + "  Hexadecimal : " + GREEN + hex);
        System.out.println(WHITE + "  Octal       : " + GREEN + octal);
        System.out.println(WHITE + "  Decimal     : " + GREEN + decimal);
        System.out.println(WHITE + "  Binary      : " + GREEN + binary);
        System.out.println(WHITE + "  Speed       : " + GREEN + speed);
        System.out.printf (WHITE + "  LED Colour  : " + GREEN +
                "(red %d, green %d, blue %d)%n" + RESET, led[0], led[1], led[2]);

        // Also print in the compact single-line format from the spec
        System.out.printf(YELLOW + "\n  [Spec Format] %s, %s, %d, %s, speed = %d, " +
                        "LED colour (red %d, green %d, blue %d).%n" + RESET,
                hex, octal, decimal, binary, speed, led[0], led[1], led[2]);
    }

    // ================================================================
    //  FLOWCHART 6 – Speed & LED Colour Calculation
    // ================================================================

    /**
     * Calculates the SwiftBot movement speed from the octal string value.
     *
     * Spec rules:
     *   1. Convert the octal string to an integer (custom method).
     *   2. If result < 50         → speed = result + 50   (too slow, boost it)
     *   3. If result > MAX_SPEED  → speed = MAX_SPEED     (hardware cap)
     *   4. Otherwise              → speed = result
     *
     * @param octal Octal string produced by decimalToOctal().
     * @return      Clamped integer speed for swiftBot.move().
     */
    static int calculateSpeed(String octal) {
        int octalInt = parseOctalString(octal); // Convert string to int manually

        if (octalInt < MIN_SPEED_THRESHOLD) {
            return octalInt + MIN_SPEED_THRESHOLD; // Boost: value too small
        } else if (octalInt > MAX_SPEED) {
            return MAX_SPEED;                      // Cap: value exceeds hardware limit
        } else {
            return octalInt;                       // Value is in range – use as-is
        }
    }

    /**
     * Manually converts an octal digit string to its integer value.
     *
     * Algorithm – positional notation, base 8:
     *   result = result × 8 + digit   (for each char left to right)
     *
     * Example: "132"  → 1×64 + 3×8 + 2 = 90
     *
     * @param octal String of octal digits (0–7).
     * @return      Integer equivalent.
     */
    static int parseOctalString(String octal) {
        int result = 0;
        for (int i = 0; i < octal.length(); i++) {
            int digit = octal.charAt(i) - '0'; // ASCII subtraction gives digit value
            result = result * 8 + digit;       // Shift one octal place, add digit
        }
        return result;
    }

    /**
     * Calculates the RGB underlight colour from the decimal equivalent.
     *
     * Spec formulas:
     *   red   = decimal
     *   green = (decimal % 80) × 3
     *   blue  = max(red, green)
     *
     * All channels are clamped to 0–255 for hardware safety.
     *
     * @param decimal Decimal equivalent of the hex input.
     * @return        int[3] – { red, green, blue }.
     */
    static int[] calculateLED(int decimal) {
        int red   = clamp(decimal, 0, 255);
        int green = clamp((decimal % 80) * 3, 0, 255);
        int blue  = Math.max(red, green); // Blue = whichever of red/green is greater

        return new int[]{ red, green, blue };
    }

    /**
     * Clamps an integer value between a lower and upper bound.
     *
     * @param value Value to clamp.
     * @param min   Lower bound (inclusive).
     * @param max   Upper bound (inclusive).
     * @return      Clamped value.
     */
    static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Sets ALL six SwiftBot underlights to the same RGB colour.
     *
     * Uses the exact API confirmed in DoesMySwiftBotWork:
     *   swiftBot.fillUnderlights(int[] rgb)
     * where rgb is a 3-element array: { red, green, blue }.
     *
     * @param r Red channel   (0–255).
     * @param g Green channel (0–255).
     * @param b Blue channel  (0–255).
     */
    static void setUnderlightColour(int r, int g, int b) {
        try {
            int[] rgb = new int[]{ r, g, b };
            swiftBot.fillUnderlights(rgb);  // Sets all 6 underlights at once
        } catch (Exception e) {
            System.out.println(RED +
                    "[ERROR] Could not set underlight colour: " + e.getMessage() + RESET);
        }
    }

    // ================================================================
    //  FLOWCHART 7 – Binary-Driven Movement Execution
    // ================================================================

    /**
     * Reads the binary string from RIGHT to LEFT, one bit at a time,
     * and issues move commands to the SwiftBot accordingly:
     *
     *   bit '1' → move FORWARD   (duration depends on hex digit count)
     *   bit '0' → SPIN in place  (always SPIN_DURATION_MS)
     *
     * Movement API (from DoesMySwiftBotWork.java):
     *   swiftBot.move(leftVelocity, rightVelocity, durationMs)
     *
     *   Forward: move(+speed, +speed, ms)  – both wheels at the same positive %
     *   Spin:    move(+speed, -speed, ms)  – wheels oppose each other → rotate
     *
     * Spec example – binary "1011010", read right → left: 0,1,0,1,1,0,1
     *   → spin, forward, spin, forward, forward, spin, forward
     *
     * @param binary    Binary string from decimalToBinary().
     * @param speed     Wheel speed (%), clamped in calculateSpeed().
     * @param hexLength Number of characters in the original hex string (1 or 2).
     */
    static void executeDanceRoutine(String binary, int speed, int hexLength) {

        // Forward duration: 1 second for 1-digit hex, 0.5 seconds for 2-digit
        int forwardMs = (hexLength == 1) ? FORWARD_1DIGIT_MS : FORWARD_2DIGIT_MS;

        System.out.println(CYAN + "\n--- Executing Dance Routine ---" + RESET);
        System.out.println(WHITE + "  Binary (read right to left) : " + binary);
        System.out.println("  Speed                        : " + speed);
        System.out.println("  Forward duration             : " + forwardMs + " ms");
        System.out.println("  Spin duration                : " + SPIN_DURATION_MS + " ms" + RESET);

        // ── Read bits from the rightmost index down to index 0 ─────────
        for (int i = binary.length() - 1; i >= 0; i--) {
            char bit = binary.charAt(i);

            if (bit == '1') {
                // ── BIT 1: Move FORWARD ─────────────────────────────────
                System.out.println(GREEN +
                        "  Bit=1 → FORWARD (" + forwardMs + "ms)" + RESET);
                try {
                    // Both wheels at +speed drives the robot straight forward
                    swiftBot.move(speed, speed, forwardMs);
                } catch (Exception e) {
                    System.out.println(RED +
                            "  [ERROR] Forward move failed: " + e.getMessage() + RESET);
                }

            } else {
                // ── BIT 0: SPIN in place ────────────────────────────────
                System.out.println(YELLOW +
                        "  Bit=0 → SPIN (" + SPIN_DURATION_MS + "ms)" + RESET);
                try {
                    // Left wheel forward, right wheel backward → robot rotates in place
                    swiftBot.move(speed, -speed, SPIN_DURATION_MS);
                } catch (Exception e) {
                    System.out.println(RED +
                            "  [ERROR] Spin move failed: " + e.getMessage() + RESET);
                }
            }
        }

        System.out.println(GREEN +
                "  [OK] Dance sequence complete for hex value." + RESET);
    }

    // ================================================================
    //  FLOWCHART 8 – Dance Completion, Repeat & Exit Handling
    // ================================================================

    /**
     * Called once all movement sequences for the current QR scan are done.
     *
     * Flowchart 8:
     *   All Movements Complete → Turn Off LEDs → Prompt User Choice
     *     X Button → Sort Hex Values → Save to File → Display Path → End
     *     Y Button → Ask for Repeat Count → Repeat Dance Routine → loop
     *
     * Button handling uses the callback API from DoesMySwiftBotWork:
     *   swiftBot.enableButton(Button.Y, () -> { ... })
     *   swiftBot.enableButton(Button.X, () -> { ... })
     *   swiftBot.disableAllButtons()
     *
     * @param validHexValues The hex values just executed (used for repeats).
     * @return true  if the user pressed Y  → main loop scans again.
     *         false if the user pressed X  → main loop exits.
     */
    static boolean handleDanceCompletion(List<String> validHexValues) {

        System.out.println(CYAN + "\n--- Dance Complete ---" + RESET);
        System.out.println(WHITE + "Press " + GREEN + "Y" + WHITE +
                " to continue scanning  |  Press " + RED + "X" + WHITE +
                " to exit." + RESET);

        // Light up Y and X to help the user identify the right buttons
        try {
            swiftBot.setButtonLight(Button.Y, true);
            swiftBot.setButtonLight(Button.X, true);
        } catch (Exception e) { /* button lights are optional */ }

        // ── Shared flags updated by button callbacks ───────────────────
        // Arrays used so lambda can modify the value (effectively final workaround)
        final boolean[] yPressed = { false };
        final boolean[] xPressed = { false };

        // Register Y callback
        swiftBot.enableButton(Button.Y, () -> {
            yPressed[0] = true;
            swiftBot.disableButton(Button.Y);
        });

        // Register X callback
        swiftBot.enableButton(Button.X, () -> {
            xPressed[0] = true;
            swiftBot.disableButton(Button.X);
        });

        // Spin-wait (100ms sleep) until one flag is set
        while (!yPressed[0] && !xPressed[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // Clean up all button callbacks and lights
        swiftBot.disableAllButtons();
        try {
            swiftBot.setButtonLight(Button.Y, false);
            swiftBot.setButtonLight(Button.X, false);
        } catch (Exception e) { /* optional */ }

        // ── X: signal the main loop to exit ───────────────────────────
        if (xPressed[0]) {
            System.out.println(RED + "[INFO] X pressed – exiting program." + RESET);
            return false;
        }

        // ── Y: offer A* repeat feature then continue ───────────────────
        System.out.println(GREEN + "[INFO] Y pressed – continuing." + RESET);

        // A* FEATURE: let the user choose how many times to repeat
        int repeatCount = askRepeatCount();

        if (repeatCount > 0) {
            System.out.println(CYAN + "[A*] Repeating dance " +
                    repeatCount + " time(s)..." + RESET);

            for (int r = 1; r <= repeatCount; r++) {
                System.out.println(YELLOW + "\n  --- Repeat " + r +
                        " of " + repeatCount + " ---" + RESET);

                // Replay using the same conversions
                for (String hex : validHexValues) {
                    int    decimal = hexToDecimal(hex);
                    String binary  = decimalToBinary(decimal);
                    String octal   = decimalToOctal(decimal);
                    int    speed   = calculateSpeed(octal);
                    int[]  led     = calculateLED(decimal);

                    setUnderlightColour(led[0], led[1], led[2]);
                    executeDanceRoutine(binary, speed, hex.length());
                }

                // LEDs off between repeats
                swiftBot.disableUnderlights();
            }
            System.out.println(GREEN + "[A*] All repeats finished." + RESET);
        }

        System.out.println(WHITE + "[INFO] Ready for next QR scan." + RESET);
        return true; // Continue the main loop
    }

    // ================================================================
    //  A* FEATURE – Repeat Count via Button A / B
    // ================================================================

    /**
     * Asks the user how many times to repeat the dance routine by using
     * the SwiftBot's physical A and B buttons:
     *   Button A → increment the count by 1 (can be pressed multiple times)
     *   Button B → confirm the current count and proceed
     *
     * Meets A* criteria:
     *   ✓ Accepts input from the user  (button presses)
     *   ✓ Performs meaningful processing  (counts repeat iterations)
     *   ✓ Produces output related to SwiftBot movement  (replay quantity)
     *
     * @return Number of extra repeats selected (0 = no repeat, just continue).
     */
    static int askRepeatCount() {
        System.out.println(CYAN + "\n[A*] Set repeat count for the dance:" + RESET);
        System.out.println(WHITE + "  Press " + GREEN + "A" + WHITE +
                " to increment  |  Press " + YELLOW + "B" + WHITE +
                " to confirm and proceed." + RESET);

        // Turn on A and B button lights to guide the user
        try {
            swiftBot.setButtonLight(Button.A, true);
            swiftBot.setButtonLight(Button.B, true);
        } catch (Exception e) { /* optional */ }

        // Shared state for callbacks
        final int[]     count     = { 0 };
        final boolean[] confirmed = { false };

        // Register A callback – re-registers itself so A can be pressed many times
        registerIncrementCallback(count, confirmed);

        // Register B callback – sets the confirmed flag once and fires once
        swiftBot.enableButton(Button.B, () -> {
            confirmed[0] = true;
            swiftBot.disableButton(Button.B);
        });

        // Wait until the user confirms with B
        while (!confirmed[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // Clean up
        swiftBot.disableAllButtons();
        try {
            swiftBot.setButtonLight(Button.A, false);
            swiftBot.setButtonLight(Button.B, false);
        } catch (Exception e) { /* optional */ }

        System.out.println(GREEN + "  [A*] Repeat count confirmed: " +
                count[0] + RESET);
        return count[0];
    }

    /**
     * Registers a one-shot callback on Button A that increments the shared
     * count array and then re-registers itself (so A can be pressed many times).
     *
     * The SwiftBot API fires each enableButton() callback only ONCE, so we
     * must re-register inside the callback to support multiple A presses.
     * Re-registration is skipped once the user has confirmed with B.
     *
     * @param count     Shared int[1] counter, incremented on each A press.
     * @param confirmed Shared boolean[1] flag; stops re-registration when true.
     */
    static void registerIncrementCallback(int[] count, boolean[] confirmed) {
        swiftBot.enableButton(Button.A, () -> {
            count[0]++;
            System.out.println(WHITE + "  Count: " + GREEN + count[0] +
                    WHITE + "  (A to add more, B to confirm)" + RESET);

            // Only re-register if the user has not yet confirmed
            if (!confirmed[0]) {
                registerIncrementCallback(count, confirmed);
            }
        });
    }

    // ================================================================
    //  PROGRAM COMPLETION – Sort & Save Session Hex Values
    // ================================================================

    /**
     * Called when the user presses X to exit.
     *
     * Actions:
     *   1. Sort all session hex values in ascending numeric order
     *      using a custom bubble sort (no Collections.sort / Arrays.sort).
     *   2. Write the sorted values to a timestamped text file.
     *   3. Print the full file path to the console.
     */
    static void finaliseAndExit() {
        System.out.println(CYAN +
                "\n============================================" + RESET);
        System.out.println(CYAN + " Finalising Session..." + RESET);
        System.out.println(CYAN +
                "============================================" + RESET);

        if (sessionHexValues.isEmpty()) {
            System.out.println(YELLOW +
                    "[INFO] No hex values were recorded this session." + RESET);
        } else {
            // Sort in ascending numeric order using custom bubble sort
            List<String> sorted = bubbleSortHex(sessionHexValues);

            System.out.println(WHITE + "\n[INFO] Sorted hex values (ascending):" + RESET);
            for (String val : sorted) {
                System.out.println(GREEN + "  " + val + RESET);
            }

            // Save to file and display the path
            String filePath = saveToFile(sorted);
            if (filePath != null) {
                System.out.println(GREEN +
                        "\n[INFO] Session values saved to: " + filePath + RESET);
            }
        }

        System.out.println(CYAN +
                "\n[INFO] Program terminated. Goodbye!" + RESET);
    }

    /**
     * Sorts a list of hex strings in ascending numeric order using a
     * custom BUBBLE SORT implementation.
     *
     * Sorting key: each hex string is converted to its decimal integer so
     * that numeric order is respected (e.g. "A"=10 sorts before "1F"=31).
     *
     * No Collections.sort, Arrays.sort, or Comparator is used.
     *
     * @param hexList Input list (not modified; a copy is sorted and returned).
     * @return        New sorted list in ascending numeric order.
     */
    static List<String> bubbleSortHex(List<String> hexList) {
        List<String> sorted = new ArrayList<>(hexList); // Work on a copy
        int n = sorted.size();

        // Outer pass: at most n-1 passes needed (worst case already sorted)
        for (int pass = 0; pass < n - 1; pass++) {

            // Inner pass: compare every adjacent pair in the unsorted portion
            for (int i = 0; i < n - 1 - pass; i++) {
                int valA = hexToDecimal(sorted.get(i));
                int valB = hexToDecimal(sorted.get(i + 1));

                // Swap if left value is greater than right (ascending order)
                if (valA > valB) {
                    String temp = sorted.get(i);
                    sorted.set(i, sorted.get(i + 1));
                    sorted.set(i + 1, temp);
                }
            }
        }

        return sorted;
    }

    /**
     * Writes the sorted hex value list to a plain text file in the
     * user's home directory. The filename includes a timestamp to
     * ensure uniqueness across multiple runs.
     *
     * @param sortedValues Sorted list of hex strings to write.
     * @return             Absolute file path on success, or null on failure.
     */
    static String saveToFile(List<String> sortedValues) {
        String fileName = "SwiftBotDance_" + System.currentTimeMillis() + ".txt";
        String filePath = System.getProperty("user.home") +
                File.separator + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("SwiftBot Dance Session – Sorted Hex Values\n");
            writer.write("==========================================\n");
            for (String val : sortedValues) {
                writer.write(val + "\n");
            }
            writer.write("\nTotal: " + sortedValues.size() + " value(s)\n");
        } catch (IOException e) {
            System.out.println(RED +
                    "[ERROR] Could not write file: " + e.getMessage() + RESET);
            return null;
        }

        return filePath;
    }

    // ================================================================
    //  UTILITY – Generic single-button wait
    // ================================================================

    /**
     * Blocks the current thread until the specified SwiftBot button is
     * pressed once.
     *
     * Internally uses the callback pattern confirmed in DoesMySwiftBotWork:
     *   swiftBot.enableButton(button, callback)
     *   swiftBot.disableButton(button)
     *
     * A 100ms sleep avoids busy-waiting and matches the style of the
     * test file's button polling loop.
     *
     * @param button The SwiftBot physical button to wait for.
     */
    static void waitForButton(Button button) {
        final boolean[] pressed = { false };

        // Register a one-shot callback that flips the flag
        swiftBot.enableButton(button, () -> {
            pressed[0] = true;
            swiftBot.disableButton(button);
        });

        // Sleep-poll until the callback fires
        while (!pressed[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
