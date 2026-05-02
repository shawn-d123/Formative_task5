package src_DrawShape_Miguel;

import swiftbot.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

// Task 6: Draw Shape - CS1814 Software Implementation

public class DrawShape {

    private static final int MAX_SHAPES_PER_QR = 5;

    private static final String RESET      = "[0m";
    private static final String WHITE      = "[37m";
    private static final String GREEN_TEXT = "[32m";
    private static final String RED_TEXT   = "[31m";
    private static final String CYAN       = "[36m";
    private static final String YELLOW     = "[33m";

    // Stops a single button press from firing more than once
    private static volatile long lastPressTime  = 0;
    private static final long    DEBOUNCE_DELAY = 250;

    private static SwiftBotAPI       swiftBot;
    private static ShapeDrawer       drawer;
    private static Logger            logger;
    private static List<ShapeRecord> sessionShapes;

    // These flags are shared between the main thread and button callback threads,
    // so we use AtomicBoolean to avoid any threading issues
    private static final AtomicBoolean xPressed = new AtomicBoolean(false);
    private static final AtomicBoolean qrReady  = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {

        // Ask the user if they want to start before doing anything else
        Scanner scanner = new Scanner(System.in);
        xPressed.set(false);
        qrReady.set(false);
        lastPressTime = 0;
        boolean validInput;
        do {
            validInput = true;
            System.out.println("Would you like to start the program? (yes/no)");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("no")) {
                System.out.println("Exiting...");
                return;
            } else if (!answer.equals("yes")) {
                System.out.println("Invalid input. Please enter yes or no.");
                validInput = false;
            }
        } while (!validInput);

        // Connect to the SwiftBot
        try {
            swiftBot = SwiftBotAPI.INSTANCE;
        } catch (Exception e) {
            System.out.println(RED_TEXT + "ERROR: Could not initialise SwiftBot: " + e.getMessage() + RESET);
            return;
        }

        drawer        = new ShapeDrawer(swiftBot);
        logger        = new Logger();
        sessionShapes = new ArrayList<>();

        System.out.println(CYAN + "==========================================");
        System.out.println("       SWIFTBOT - DRAW SHAPE (Task 6)     ");
        System.out.println("==========================================" + RESET);
        System.out.println(YELLOW + "Press X at any time to quit." + RESET);
        System.out.println();

        // Keep running until the user presses X
        while (!xPressed.get()) {

            System.out.println(CYAN + "------------------------------------------");
            System.out.println(WHITE + "  Scan a QR code to draw a shape.");
            System.out.println(YELLOW + "  Press X to quit." + RESET);
            System.out.println(CYAN + "------------------------------------------" + RESET);

            registerXButton();

            String scanned = waitForQRCode();
            if (scanned == null) break;

            System.out.println("QR scanned: " + scanned);

            List<ShapeCommand> commands = parseQRContent(scanned);
            if (commands.isEmpty()) {
                System.out.println(RED_TEXT + "ERROR: No valid shape commands found. Please try again." + RESET);
                continue;
            }

            // Draw each shape from the QR code one at a time
            for (int i = 0; i < commands.size(); i++) {
                if (xPressed.get()) break;

                ShapeCommand cmd = commands.get(i);
                System.out.println();
                System.out.println("Shape " + (i + 1) + " of " + commands.size() + ": " + cmd.describe());

                long startTime  = System.currentTimeMillis();
                drawer.draw(cmd);
                long durationMs = System.currentTimeMillis() - startTime;

                sessionShapes.add(new ShapeRecord(cmd, durationMs));
                System.out.println("Done. Took " + durationMs + " ms.");

                // If there are more shapes to draw, reverse 15 cm to give space
                if (i < commands.size() - 1 && !xPressed.get()) {
                    System.out.println(YELLOW + "Moving back 15 cm before next shape..." + RESET);
                    drawer.moveBackward15cm();
                    Thread.sleep(1000);
                }
            }
        }

        // Clean up and save the log when the session ends
        swiftBot.disableAllButtons();
        System.out.println();
        System.out.println(CYAN + "=== Session ended ===" + RESET);
        if (sessionShapes.isEmpty()) {
            System.out.println(YELLOW + "No shapes were drawn this session." + RESET);
        } else {
            System.out.println(WHITE + "Total shapes drawn : " + GREEN_TEXT + sessionShapes.size() + RESET);
            String logPath = logger.writeSessionLog(sessionShapes);
            System.out.println(WHITE + "Log saved to       : " + GREEN_TEXT + logPath + RESET);
        }
        System.out.println(CYAN + "Goodbye!" + RESET);
    }

    // Registers the X button so the user can quit at any time.
    // We always disable all buttons first to clear any old handlers.
    private static void registerXButton() {
        swiftBot.disableAllButtons();
        swiftBot.enableButton(Button.X, () -> {
            long now = System.currentTimeMillis();
            if (now - lastPressTime < DEBOUNCE_DELAY) return;
            lastPressTime = now;
            System.out.println("X pressed - stopping...");
            xPressed.set(true);
            qrReady.set(true); // wake up the QR wait loop if it is sleeping
        });
    }

    // Waits for the user to press A, takes a photo, and tries to decode a QR code.
    // If the QR is not detected clearly it just asks them to try again.
    // Returns null if X was pressed instead.
    private static String waitForQRCode() throws InterruptedException {
        String result = null;

        while (!xPressed.get() && (result == null || result.trim().isEmpty())) {
            System.out.println(YELLOW + "Press A to scan QR code, or X to quit." + RESET);

            swiftBot.disableAllButtons();
            qrReady.set(false);

            swiftBot.enableButton(Button.A, () -> {
                long now = System.currentTimeMillis();
                if (now - lastPressTime < DEBOUNCE_DELAY) return;
                lastPressTime = now;
                qrReady.set(true);
            });

            swiftBot.enableButton(Button.X, () -> {
                long now = System.currentTimeMillis();
                if (now - lastPressTime < DEBOUNCE_DELAY) return;
                lastPressTime = now;
                System.out.println("X pressed - stopping...");
                xPressed.set(true);
                qrReady.set(true);
            });

            // Wait here until A or X is pressed
            while (!qrReady.get()) {
                Thread.sleep(50);
            }

            if (xPressed.get()) return null;

            // Take a photo and decode it
            try {
                swiftBot.disableAllButtons();
                java.awt.image.BufferedImage img = swiftBot.getQRImage();
                result = swiftBot.decodeQRImage(img);
                if (result == null || result.trim().isEmpty()) {
                    System.out.println(RED_TEXT + "ERROR: QR not detected. Hold the code steady and try again." + RESET);
                    result = null;
                }
            } catch (Exception e) {
                System.out.println(RED_TEXT + "ERROR: Camera problem - " + e.getMessage() + RESET);
                result = null;
            }
        }

        return (result != null) ? result.trim() : null;
    }

    // Breaks the raw QR string into individual shape tokens split by '&',
    // validates each one, and skips anything invalid with a message.
    private static List<ShapeCommand> parseQRContent(String raw) {
        List<ShapeCommand> result = new ArrayList<>();
        String[] tokens = raw.trim().split("&");

        if (tokens.length > MAX_SHAPES_PER_QR) {
            System.out.println(YELLOW + "WARNING: QR has " + tokens.length
                + " shapes but only the first " + MAX_SHAPES_PER_QR + " will be used." + RESET);
        }

        int limit = Math.min(tokens.length, MAX_SHAPES_PER_QR);
        for (int i = 0; i < limit; i++) {
            String token = tokens[i].trim();
            try {
                result.add(ShapeParser.parse(token));
            } catch (IllegalArgumentException e) {
                System.out.println(YELLOW + "Skipping \"" + token + "\": " + e.getMessage() + RESET);
            }
        }
        return result;
    }
}

// Holds a single validated shape instruction - either a square or a triangle.
// Once created this object never changes.
class ShapeCommand {

    enum ShapeType { SQUARE, TRIANGLE }

    private final ShapeType type;
    private final int[]     sides;   // side lengths in cm
    private final double[]  angles;  // interior angles in degrees (triangles only)

    ShapeCommand(ShapeType type, int[] sides, double[] angles) {
        this.type   = type;
        this.sides  = sides.clone();
        this.angles = (angles != null) ? angles.clone() : new double[0];
    }

    ShapeType getType()   { return type; }
    int[]     getSides()  { return sides.clone(); }
    double[]  getAngles() { return angles.clone(); }

    // Returns a readable summary shown in the console before drawing starts
    String describe() {
        if (type == ShapeType.SQUARE) {
            return "Square (side = " + sides[0] + " cm)";
        }
        return String.format(
            "Triangle (sides = %d, %d, %d cm | angles = %.2f deg, %.2f deg, %.2f deg)",
            sides[0], sides[1], sides[2], angles[0], angles[1], angles[2]);
    }
}

// Stores a completed shape alongside how long it took to draw.
// The Logger uses this at the end of the session.
class ShapeRecord {

    private final ShapeCommand command;
    private final long         durationMs;

    ShapeRecord(ShapeCommand command, long durationMs) {
        this.command    = command;
        this.durationMs = durationMs;
    }

    ShapeCommand getCommand()    { return command; }
    long         getDurationMs() { return durationMs; }

    // Calculates area so we can find the largest shape drawn
    double getAreaCm2() {
        if (command.getType() == ShapeCommand.ShapeType.SQUARE) {
            double s = command.getSides()[0];
            return s * s;
        }
        // Use Heron's formula for triangles
        int[] s  = command.getSides();
        double a = s[0], b = s[1], c = s[2];
        double sp = (a + b + c) / 2.0;
        return Math.sqrt(sp * (sp - a) * (sp - b) * (sp - c));
    }
}

// Reads a raw QR token like "S:30" or "T:20:40:30" and turns it into
// a ShapeCommand. Throws an IllegalArgumentException if anything is wrong.
class ShapeParser {

    private static final int MIN_SIDE = 15;
    private static final int MAX_SIDE = 85;

    static ShapeCommand parse(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty token.");
        }
        String[] parts     = token.trim().split(":");
        String   shapeCode = parts[0].trim().toUpperCase();

        switch (shapeCode) {
            case "S": return parseSquare(parts);
            case "T": return parseTriangle(parts);
            default:
                throw new IllegalArgumentException(
                    "Unknown shape code \"" + parts[0] + "\". Use S for square or T for triangle.");
        }
    }

    private static ShapeCommand parseSquare(String[] parts) {
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Square needs one side length. Format: S:<length>  e.g. S:30");
        }
        int side = parseLength(parts[1], "Square side");
        return new ShapeCommand(ShapeCommand.ShapeType.SQUARE, new int[]{side}, null);
    }

    private static ShapeCommand parseTriangle(String[] parts) {
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                "Triangle needs three side lengths. Format: T:<a>:<b>:<c>  e.g. T:20:40:30");
        }
        int a = parseLength(parts[1], "Side A");
        int b = parseLength(parts[2], "Side B");
        int c = parseLength(parts[3], "Side C");

        // Check the three lengths can actually form a triangle
        if (!(a + b > c && a + c > b && b + c > a)) {
            throw new IllegalArgumentException(
                "Sides " + a + ", " + b + ", " + c + " cannot form a valid triangle.");
        }
        double[] angles = computeAngles(a, b, c);
        return new ShapeCommand(ShapeCommand.ShapeType.TRIANGLE, new int[]{a, b, c}, angles);
    }

    // Parses a side length string and checks it is within the allowed range
    private static int parseLength(String raw, String label) {
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                label + " must be a whole number, got: \"" + raw.trim() + "\"");
        }
        if (value < MIN_SIDE || value > MAX_SIDE) {
            throw new IllegalArgumentException(
                label + " must be between " + MIN_SIDE + " and " + MAX_SIDE + " cm, got: " + value);
        }
        return value;
    }

    // Works out interior angles using the Law of Cosines
    private static double[] computeAngles(int a, int b, int c) {
        double A = Math.toDegrees(Math.acos((b * b + c * c - a * a) / (2.0 * b * c)));
        double B = Math.toDegrees(Math.acos((a * a + c * c - b * b) / (2.0 * a * c)));
        double C = 180.0 - A - B;
        return new double[]{A, B, C};
    }
}

// Handles all of the actual SwiftBot movement for drawing shapes.
// The calibration constants at the top will need adjusting for your specific robot.
class ShapeDrawer {

    private static final String RESET      = "[0m";
    private static final String GREEN_TEXT = "[32m";
    private static final String CYAN       = "[36m";
    private static final String YELLOW     = "[33m";

    // Tune these values by running the robot and measuring the results
    private static final int    DRIVE_SPEED = 60;   // wheel speed for straight lines (0-100)
    private static final double MS_PER_CM   = 42.0; // milliseconds to travel 1 cm at DRIVE_SPEED
    private static final int    TURN_SPEED  = 50;   // wheel speed used when turning
    private static final double MS_PER_DEG  = 7;  // milliseconds per degree of rotation at TURN_SPEED

    private static final int[] GREEN = {0, 255, 0};
    private static final int[] BLUE  = {0, 0, 255};
    private static final int[] OFF   = {0, 0, 0};

    private final SwiftBotAPI swiftBot;

    ShapeDrawer(SwiftBotAPI swiftBot) {
        this.swiftBot = swiftBot;
    }

    // Entry point - decides which drawing method to call and blinks when done
    void draw(ShapeCommand cmd) {
        switch (cmd.getType()) {
            case SQUARE:   drawSquare(cmd.getSides()[0]); break;
            case TRIANGLE: drawTriangle(cmd);             break;
        }
        signalComplete();
    }

    // Draws a square by going forward and turning 90 degrees, four times
    private void drawSquare(int sideLen) {
        setUnderlights(GREEN);
        System.out.println(GREEN_TEXT + "Drawing " + sideLen + " cm square..." + RESET);
        for (int i = 1; i <= 4; i++) {
            System.out.println(GREEN_TEXT + "  Side " + i + " of 4 - forward " + sideLen + " cm" + RESET);
            driveForward(sideLen);
            pause(500);
            if (i < 4) {
                System.out.println(GREEN_TEXT + "  Turning 90 degrees right" + RESET);
                turnRight(90);
                pause(300);
            }
        }
        setUnderlights(OFF);
    }

    // Draws a triangle by driving each side and turning by the exterior angle at each corner.
    // The exterior angle at a corner is 180 minus the interior angle at that corner.
    private void drawTriangle(ShapeCommand cmd) {
        int[]    sides  = cmd.getSides();
        double[] angles = cmd.getAngles();
        setUnderlights(BLUE);
        System.out.println(CYAN + "Drawing triangle with sides " + sides[0] + ", " + sides[1] + ", " + sides[2] + " cm" + RESET);
        System.out.printf(CYAN + "Interior angles: %.2f deg, %.2f deg, %.2f deg" + RESET + "%n",
            angles[0], angles[1], angles[2]);

        for (int i = 0; i < 3; i++) {
            System.out.println(CYAN + "  Side " + (i + 1) + " of 3 - forward " + sides[i] + " cm" + RESET);
            driveForward(sides[i]);
            pause(500);
            if (i < 2) {
                double exterior = 180.0 - angles[(i + 1) % 3];
                System.out.printf(CYAN + "  Turning %.2f degrees right" + RESET + "%n", exterior);
                turnRight(exterior);
                pause(300);
            }
        }
        setUnderlights(OFF);
    }

    // Moves back 15 cm to give space before drawing the next shape in a sequence
    void moveBackward15cm() {
        long ms = (long)(15 * MS_PER_CM);
        swiftBot.startMove(-DRIVE_SPEED, -DRIVE_SPEED);
        pause(ms);
        swiftBot.stopMove();
    }

    // Drives straight forward for the given distance
    private void driveForward(int distanceCm) {
        long ms = (long)(distanceCm * MS_PER_CM);
        swiftBot.startMove(DRIVE_SPEED, DRIVE_SPEED);
        pause(ms);
        swiftBot.stopMove();
    }

    // Turns right in place - left wheel forward, right wheel backward
    private void turnRight(double degrees) {
        long ms = (long)(degrees * MS_PER_DEG);
        swiftBot.startMove(TURN_SPEED, -TURN_SPEED);
        pause(ms);
        swiftBot.stopMove();
    }

    // Sets all four underlights to the same colour
    private void setUnderlights(int[] rgb) {
        for (Underlight u : Underlight.values()) {
            swiftBot.setUnderlight(u, rgb);
        }
    }

    // Blinks green three times to let the user know the shape is finished
    private void signalComplete() {
        System.out.println(GREEN_TEXT + "Shape complete - blinking green." + RESET);
        for (int i = 0; i < 3; i++) {
            setUnderlights(GREEN);
            pause(300);
            setUnderlights(OFF);
            pause(300);
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}

// Writes a summary of everything drawn during the session to a text file.
// The filename includes the date and time so logs never overwrite each other.
class Logger {

    private static final String LOG_DIR = System.getProperty("user.home");

    String writeSessionLog(List<ShapeRecord> records) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filePath  = LOG_DIR + "/DrawShape_Log_" + timestamp + ".txt";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {

            pw.println("=================================================");
            pw.println("  SWIFTBOT DRAW SHAPE - SESSION LOG");
            pw.println("  Generated: " + new Date());
            pw.println("=================================================");
            pw.println();

            pw.println("--- Shapes drawn (in order) ---");
            for (int i = 0; i < records.size(); i++) {
                pw.println((i + 1) + ". " + formatShapeEntry(records.get(i)));
            }
            pw.println();

            pw.println("--- Largest shape by area ---");
            pw.println(formatLargestEntry(findLargest(records)));
            pw.println();

            pw.println("--- Most frequently drawn shape ---");
            pw.println(formatFrequency(records));
            pw.println();

            pw.println("--- Average draw time ---");
            pw.println(formatAverageTime(records));
            pw.println();

            pw.println("=================================================");
            pw.println("  END OF LOG");
            pw.println("=================================================");

        } catch (IOException e) {
            System.out.println("ERROR: Could not write log file: " + e.getMessage());
            return "UNAVAILABLE";
        }

        return filePath;
    }

    // Formats one shape entry to match the brief's example layout
    private String formatShapeEntry(ShapeRecord r) {
        ShapeCommand cmd = r.getCommand();
        long ms          = r.getDurationMs();
        if (cmd.getType() == ShapeCommand.ShapeType.SQUARE) {
            return "Square: " + cmd.getSides()[0] + " (time: " + ms + " ms)";
        }
        int[]    s = cmd.getSides();
        double[] a = cmd.getAngles();
        return String.format(
            "Triangle: %d, %d, %d (angles: %.2f, %.2f, %.2f; time: %d ms)",
            s[0], s[1], s[2], a[0], a[1], a[2], ms);
    }

    private String formatLargestEntry(ShapeRecord r) {
        ShapeCommand cmd = r.getCommand();
        if (cmd.getType() == ShapeCommand.ShapeType.SQUARE) {
            return String.format("Square: %d (area = %.2f cm2)", cmd.getSides()[0], r.getAreaCm2());
        }
        int[] s = cmd.getSides();
        return String.format("Triangle: %d, %d, %d (area = %.2f cm2)", s[0], s[1], s[2], r.getAreaCm2());
    }

    private String formatFrequency(List<ShapeRecord> records) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Square",   0);
        counts.put("Triangle", 0);
        for (ShapeRecord r : records) {
            String key = (r.getCommand().getType() == ShapeCommand.ShapeType.SQUARE)
                         ? "Square" : "Triangle";
            counts.put(key, counts.get(key) + 1);
        }
        String best = counts.get("Square") >= counts.get("Triangle") ? "Square" : "Triangle";
        return best + ": " + counts.get(best) + " time(s)";
    }

    private String formatAverageTime(List<ShapeRecord> records) {
        if (records.isEmpty()) return "N/A";
        long total = 0;
        for (ShapeRecord r : records) total += r.getDurationMs();
        return (total / records.size()) + " ms";
    }

    // Finds which shape had the biggest area across the whole session
    private ShapeRecord findLargest(List<ShapeRecord> records) {
        ShapeRecord largest = records.get(0);
        for (ShapeRecord r : records) {
            if (r.getAreaCm2() > largest.getAreaCm2()) largest = r;
        }
        return largest;
    }
}
