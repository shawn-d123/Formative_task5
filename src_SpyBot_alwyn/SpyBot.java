package src_SpyBot_alwyn;

import swiftbot.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// SpyBot - CS1814 Software Implementation, Task 5
// This program turns the SwiftBot into a Morse code messenger between 3 agents.
// Agents at safe houses A, B and C can send authenticated messages to each other.
// The robot physically travels to the destination and delivers the message via LEDs.
public class SpyBot {

    public static void main(String[] args) {
        UI.printBanner();

        // Set up all the main components the program needs
        SwiftBotController controller = new SwiftBotController();
        AgentRegistry      registry   = new AgentRegistry();
        MessageManager     msgManager = new MessageManager();
        Logger             logger     = new Logger();

        boolean running = true;

        while (running) {
            UI.printMainMenu();
            char choice = controller.waitForButton(new char[]{'A', 'B', 'X', 'Y'});

            switch (choice) {
                case 'A':
                    handleSend(controller, registry, msgManager, logger);
                    break;
                case 'B':
                    handleCheckMessages(controller, registry, msgManager);
                    break;
                case 'Y':
                    handleReplay(logger);
                    break;
                case 'X':
                    running = false;
                    break;
            }
        }

        // Save the log before shutting down and tell the user where it is
        logger.writeLog();
        UI.print("Log saved to: " + logger.getLogPath());
        UI.print("SpyBot shutting down. Stay covert.");
        controller.shutdown();
        return;
    }

    // Handles the full send flow:
    // 1. Sender scans QR to authenticate
    // 2. Sender enters message in Morse using buttons
    // 3. Robot travels to destination
    // 4. Receiver scans QR to authenticate
    // 5. Message is delivered via LED blinks
    // 6. Robot waits 10 seconds then returns to origin
    private static void handleSend(SwiftBotController controller,
                                   AgentRegistry registry,
                                   MessageManager msgManager,
                                   Logger logger) {
        UI.printSeparator();
        UI.print("SEND MESSAGE");
        UI.print("Scan your agent QR code to authenticate.");

        String qrData = controller.scanQRCode();
        Agent sender  = Authenticator.authenticate(qrData, registry);

        if (sender == null) {
            UI.printError("Authentication failed. Returning to main menu.");
            return;
        }

        UI.print("Authenticated: " + sender.getCallSign() + " @ " + sender.getLocation());
        UI.printSeparator();
        UI.print("Enter your message in Morse code:");
        UI.print("  X = dot (.)    Y = dash (-)");
        UI.print("  A = end of character    B = end of word");
        UI.print("  End of message = enter Morse for 0 (5 dashes): press Y five times then A");
        UI.print("  FIRST WORD must be the DESTINATION (A, B, or C).");
        UI.printSeparator();

        MorseCodec  codec      = new MorseCodec();
        MorseInput  morseInput = new MorseInput(controller);
        String      morseRaw   = morseInput.captureMessage();

        // User pressed cancel during Morse input
        if (morseRaw == null || morseRaw.trim().isEmpty()) {
            UI.print("Message cancelled. Returning to main menu.");
            return;
        }

        String plainText = codec.decode(morseRaw);

        if (plainText == null || plainText.trim().isEmpty()) {
            UI.printError("Could not decode message. Aborting.");
            return;
        }

        UI.print("Decoded: \"" + plainText + "\"");

        // The first word in the message must be the destination location
        String[] words = plainText.trim().split("\\s+");
        if (words.length < 2) {
            UI.printError("Message must have a destination and a body (at least 2 words).");
            return;
        }

        String destCode    = words[0].toUpperCase();
        Agent  destination = registry.getAgentByLocation(destCode);

        if (destination == null) {
            UI.printError("Unknown destination: '" + destCode + "'. Must be A, B, or C.");
            return;
        }

        // Can't send a message to yourself
        if (destCode.equals(sender.getLocation())) {
            UI.printError("Destination cannot be your own location.");
            return;
        }

        // Everything after the destination word is the message body
        StringBuilder bodyBuilder = new StringBuilder();
        for (int i = 1; i < words.length; i++) {
            bodyBuilder.append(words[i]);
            if (i < words.length - 1) bodyBuilder.append(" ");
        }
        String body = bodyBuilder.toString();

        Message message = new Message(sender, destination, body, morseRaw);
        msgManager.storeMessage(message);

        // Show estimated travel time before moving (A* additional feature)
        long estimatedMs = SwiftBotController.estimateTravelMs(sender.getLocation(), destCode);
        UI.print("Estimated travel time to " + destCode + ": ~" + (estimatedMs / 1000) + " seconds.");
        UI.print("Message stored. Travelling to safe house " + destCode + "...");
        controller.travelTo(sender.getLocation(), destCode);

        // Blink red 3 times on arrival to signal a message is waiting
        controller.blinkUnderlights(LEDColour.RED, 3);
        UI.print("Arrived at " + destCode + ". Awaiting receiver authentication.");
        UI.print("Receiver: scan your QR code.");

        String receiverQr = controller.scanQRCode();
        Agent  receiver   = Authenticator.authenticate(receiverQr, registry);

        // Make sure the receiver is actually at this location, not an imposter
        if (receiver == null || !receiver.getLocation().equalsIgnoreCase(destCode)) {
            UI.printError("Receiver authentication failed. Message not delivered.");
            UI.print("Returning to origin...");
            controller.returnTo(destCode, sender.getLocation());
            return;
        }

        UI.print("Receiver verified: " + receiver.getCallSign());
        UI.printSeparator();
        UI.print("Delivering via LED Morse code...");
        UI.print("Format: [sender_location] [body]");

        // Prepend the sender's location so the receiver knows who sent it
        String fullDelivery = sender.getLocation() + " " + body;
        MorseDelivery delivery = new MorseDelivery(controller, codec);
        delivery.deliver(fullDelivery);

        message.markDelivered();
        logger.logMessage(message);

        // Wait 10 seconds at the destination before heading back
        UI.print("Message delivered. Returning to origin in 10 seconds...");
        controller.waitSeconds(10);
        controller.returnTo(destCode, sender.getLocation());
        UI.print("Returned to safe house " + sender.getLocation() + ".");
    }

    // Lets an agent check if they have any undelivered messages waiting
    private static void handleCheckMessages(SwiftBotController controller,
                                            AgentRegistry registry,
                                            MessageManager msgManager) {
        UI.printSeparator();
        UI.print("CHECK MESSAGES");
        UI.print("Scan your QR code.");

        String qrData = controller.scanQRCode();
        Agent  agent  = Authenticator.authenticate(qrData, registry);

        if (agent == null) {
            UI.printError("Authentication failed.");
            return;
        }

        List<Message> pending = msgManager.getPendingFor(agent.getLocation());

        if (pending.isEmpty()) {
            UI.print("No pending messages for " + agent.getCallSign() + ".");
            return;
        }

        UI.print(pending.size() + " pending message(s) for " + agent.getCallSign() + ":");
        for (Message m : pending) {
            UI.print("  From: " + m.getSender().getCallSign()
                    + " | Recorded: " + m.getRecordedTime()
                    + " | Body: " + m.getBody());
        }
    }

    // Replay mode - shows all messages sent during this session (A* additional feature)
    private static void handleReplay(Logger logger) {
        UI.printSeparator();
        UI.print("MESSAGE REPLAY MODE");

        List<String> entries = logger.getEntries();

        if (entries.isEmpty()) {
            UI.print("No messages have been logged in this session.");
            return;
        }

        UI.print(entries.size() + " message(s) on record this session:");
        UI.printSeparator();
        for (int i = 0; i < entries.size(); i++) {
            UI.print("--- Message " + (i + 1) + " ---");
            for (String line : entries.get(i).split("\n")) {
                if (!line.trim().isEmpty()) UI.print(line.trim());
            }
        }
        UI.printSeparator();
        UI.print("End of replay. Returning to main menu.");
    }
}

// Represents one agent in the SpyBot network.
// Each agent has a unique call sign and is assigned to one safe house (A, B or C).
class Agent {

    private final String callSign;
    private final String location;

    Agent(String callSign, String location) {
        this.callSign = callSign;
        this.location = location.toUpperCase();
    }

    String getCallSign() { return callSign; }
    String getLocation() { return location; }

    @Override
    public String toString() {
        return "Agent[" + callSign + "@" + location + "]";
    }
}

// Stores the three registered agents for this network.
// Each agent has a unique call sign I designed for this task.
// QR code format is: callSign:location   e.g. DELTA7:A
class AgentRegistry {

    private final Map<String, Agent> byCallSign = new HashMap<>();
    private final Map<String, Agent> byLocation = new HashMap<>();

    AgentRegistry() {
        register(new Agent("DELTA7", "A"));
        register(new Agent("BRAVO9", "B"));
        register(new Agent("ECHO3X", "C"));
    }

    private void register(Agent agent) {
        byCallSign.put(agent.getCallSign().toUpperCase(), agent);
        byLocation.put(agent.getLocation().toUpperCase(), agent);
    }

    // Look up by call sign - returns null if not found
    Agent getAgentByCallSign(String callSign) {
        if (callSign == null) return null;
        return byCallSign.get(callSign.toUpperCase());
    }

    // Look up by safe house letter - returns null if not found
    Agent getAgentByLocation(String location) {
        if (location == null) return null;
        return byLocation.get(location.toUpperCase());
    }
}

// Handles QR code validation. The QR must be in the format callSign:location
// and must match one of the registered agents exactly.
class Authenticator {

    private static final String VALID_LOCATION = "^[ABCabc]$";
    private static final String VALID_CALLSIGN = "^[A-Za-z0-9]+$";

    // Reads the QR data, splits it on the colon, and validates both parts.
    // Returns the matching agent if everything checks out, null otherwise.
    static Agent authenticate(String qrData, AgentRegistry registry) {
        if (qrData == null || qrData.trim().isEmpty()) {
            UI.printError("QR code is empty.");
            return null;
        }

        String[] parts = qrData.trim().split(":");

        if (parts.length != 2) {
            UI.printError("Invalid QR format. Expected <callSign>:<location>, got: " + qrData);
            return null;
        }

        String callSign = parts[0].trim();
        String location = parts[1].trim();

        if (!callSign.matches(VALID_CALLSIGN)) {
            UI.printError("Call sign must be alphanumeric: " + callSign);
            return null;
        }

        if (!location.matches(VALID_LOCATION)) {
            UI.printError("Location must be A, B, or C. Got: " + location);
            return null;
        }

        Agent agent = registry.getAgentByCallSign(callSign);

        if (agent == null) {
            UI.printError("Unknown call sign: " + callSign);
            return null;
        }

        // Also check the location matches - the call sign alone isn't enough
        if (!agent.getLocation().equalsIgnoreCase(location)) {
            UI.printError("Location mismatch for " + callSign
                    + ". Expected " + agent.getLocation() + ", got " + location);
            return null;
        }

        return agent;
    }
}

// Converts between plain text and Morse code.
// Tries to load from MorseCodeDictionary.txt first.
// If the file isn't found it falls back to a built-in version of the same dictionary.
// The format in the file is one entry per line e.g. "A .-"
class MorseCodec {

    private final Map<Character, String> encodeMap = new HashMap<>();
    private final Map<String, Character> decodeMap = new HashMap<>();

    // Morse for digit 0 is used as the end-of-message marker
    private static final String END_OF_MSG_MORSE = "-----";

    MorseCodec() {
        if (!loadFromFile("MorseCodeDictionary.txt")) {
            loadBuiltIn();
        }
    }

    // Reads the dictionary file line by line and builds both lookup maps
    private boolean loadFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2 && parts[0].length() == 1) {
                    char ch = Character.toUpperCase(parts[0].charAt(0));
                    encodeMap.put(ch, parts[1].trim());
                    decodeMap.put(parts[1].trim(), ch);
                }
            }
            UI.print("Morse dictionary loaded from: " + path);
            return true;
        } catch (IOException e) {
            UI.printError("Dictionary file not found. Using built-in dictionary.");
            return false;
        }
    }

    // Built-in fallback that matches the university-provided dictionary exactly.
    // Only includes A-Z and 0 since those are all that's needed for this task.
    private void loadBuiltIn() {
        String[][] entries = {
            {"A",".-"},   {"B","-..."},  {"C","-.-."},  {"D","-.."},
            {"E","."},    {"F","..-."},  {"G","--."},   {"H","...."},
            {"I",".."},   {"J",".---"},  {"K","-.-"},   {"L",".-.."},
            {"M","--"},   {"N","-."},    {"O","---"},   {"P",".--."},
            {"Q","--.-"}, {"R",".-."},   {"S","..."},   {"T","-"},
            {"U","..-"},  {"V","...-"},  {"W",".--"},   {"X","-..-"},
            {"Y","-.--"}, {"Z","--.."},
            {"0","-----"}
        };
        for (String[] e : entries) {
            char ch = e[0].charAt(0);
            encodeMap.put(ch, e[1]);
            decodeMap.put(e[1], ch);
        }
    }

    // Converts plain text to a Morse string.
    // Characters are separated by spaces, words by " / "
    String encode(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        String[] words = text.toUpperCase().trim().split("\\s+");

        for (int w = 0; w < words.length; w++) {
            for (int c = 0; c < words[w].length(); c++) {
                char ch = words[w].charAt(c);
                String morse = encodeMap.get(ch);
                if (morse == null) continue;
                sb.append(morse);
                if (c < words[w].length() - 1) sb.append(" ");
            }
            if (w < words.length - 1) sb.append(" / ");
        }
        return sb.toString();
    }

    // Converts a Morse string back to plain text.
    // Stops when it hits the end-of-message marker (-----)
    String decode(String morseText) {
        if (morseText == null || morseText.trim().isEmpty()) return null;

        StringBuilder result = new StringBuilder();
        String[] wordTokens = morseText.trim().split("\\s*/\\s*");

        for (int w = 0; w < wordTokens.length; w++) {
            String[] charCodes = wordTokens[w].trim().split("\\s+");

            for (String code : charCodes) {
                code = code.trim();
                if (code.isEmpty()) continue;

                if (code.equals(END_OF_MSG_MORSE)) {
                    return result.toString().trim();
                }

                Character ch = decodeMap.get(code);
                if (ch == null) {
                    UI.printError("Unknown Morse: '" + code + "' — skipping.");
                } else {
                    result.append(ch);
                }
            }

            if (w < wordTokens.length - 1) result.append(" ");
        }
        return result.toString().trim();
    }

    // Looks up the Morse string for a single character
    String getMorse(char ch) {
        return encodeMap.get(Character.toUpperCase(ch));
    }

    // Looks up the character for a Morse string
    Character getChar(String morse) {
        return decodeMap.get(morse);
    }
}

// Captures a Morse message from the SwiftBot buttons.
// X = dot, Y = dash, A = end of character, B = end of word
// Pressing A twice with nothing entered cancels the input.
// The end-of-message signal is Morse for 0 (five dashes then A).
class MorseInput {

    private final SwiftBotController controller;

    // Standard Morse codes are at most 6 symbols long
    private static final int MAX_CHAR_LENGTH = 6;

    MorseInput(SwiftBotController controller) {
        this.controller = controller;
    }

    // Loops waiting for button presses and builds up the Morse string.
    // Returns null if the user cancels, otherwise returns the raw Morse.
    String captureMessage() {
        StringBuilder fullMessage = new StringBuilder();
        StringBuilder currentChar = new StringBuilder();
        boolean done      = false;
        int emptyAPresses = 0;

        UI.print("Ready. Begin Morse input:");
        UI.print("(To cancel: press A twice in a row with nothing entered)");

        while (!done) {
            char btn = controller.waitForButton(new char[]{'X', 'Y', 'A', 'B'});

            switch (btn) {
                case 'X': // dot
                    currentChar.append('.');
                    emptyAPresses = 0;
                    UI.printInline(".");
                    warnIfTooLong(currentChar);
                    break;

                case 'Y': // dash
                    currentChar.append('-');
                    emptyAPresses = 0;
                    UI.printInline("-");
                    warnIfTooLong(currentChar);
                    break;

                case 'A': // end of character
                    if (currentChar.length() == 0) {
                        emptyAPresses++;
                        if (emptyAPresses >= 2) {
                            UI.print("\nCancelled. Returning to main menu.");
                            return null;
                        }
                        UI.printError("No dots/dashes entered. Press A again to cancel, or continue.");
                        break;
                    }
                    emptyAPresses = 0;
                    String code = currentChar.toString();
                    UI.print(" [" + code + "]");

                    // Add a space between characters unless this is the start of a new word
                    if (fullMessage.length() > 0 && !fullMessage.toString().endsWith("/ ")) {
                        fullMessage.append(" ");
                    }
                    fullMessage.append(code);
                    currentChar.setLength(0);

                    // Five dashes = Morse for 0 = end of message
                    if (code.equals("-----")) {
                        done = true;
                        UI.print("[END OF MESSAGE]");
                    }
                    break;

                case 'B': // end of word — commit any pending character first
                    emptyAPresses = 0;
                    if (currentChar.length() > 0) {
                        String pending = currentChar.toString();
                        if (fullMessage.length() > 0 && !fullMessage.toString().endsWith("/ ")) {
                            fullMessage.append(" ");
                        }
                        fullMessage.append(pending);
                        currentChar.setLength(0);
                        UI.print(" [" + pending + "]");
                    }
                    fullMessage.append(" / ");
                    UI.print(" [WORD BREAK]");
                    break;
            }
        }

        return fullMessage.toString().trim();
    }

    // Warns the user if they've entered more symbols than a valid Morse character
    private void warnIfTooLong(StringBuilder sb) {
        if (sb.length() >= MAX_CHAR_LENGTH) {
            UI.printError("Warning: " + sb.length() + " symbols in one character (max 6). Press A to finalise.");
        }
    }
}

// Delivers a message by blinking the SwiftBot's underlights in Morse.
// Each symbol type has its own colour:
//   dot = White, dash = Blue, end of char = Amber, end of word = Red, end of message = Green
class MorseDelivery {

    private final SwiftBotController controller;
    private final MorseCodec         codec;

    // Timing values for the LED blinks - these felt natural during testing
    private static final int BLINK_ON_MS  = 300;
    private static final int BLINK_OFF_MS = 200;
    private static final int CHAR_GAP_MS  = 400;
    private static final int WORD_GAP_MS  = 700;

    MorseDelivery(SwiftBotController controller, MorseCodec codec) {
        this.controller = controller;
        this.codec      = codec;
    }

    // Goes through each character of the message, looks up its Morse code,
    // and blinks the appropriate LED colour for each symbol.
    void deliver(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            UI.printError("Nothing to deliver.");
            return;
        }

        UI.print("Delivering: \"" + plainText + "\"");
        String[] words = plainText.toUpperCase().trim().split("\\s+");

        for (int w = 0; w < words.length; w++) {
            for (int c = 0; c < words[w].length(); c++) {
                char ch = words[w].charAt(c);
                String morse = codec.getMorse(ch);
                if (morse == null) {
                    UI.printError("Cannot encode '" + ch + "' — skipping.");
                    continue;
                }

                // Blink white for dot, blue for dash
                for (char sym : morse.toCharArray()) {
                    if (sym == '.') {
                        blink(LEDColour.WHITE, ".");
                    } else if (sym == '-') {
                        blink(LEDColour.BLUE, "-");
                    }
                }

                // Amber after each character
                blink(LEDColour.AMBER, "|char|");
                controller.waitMilliseconds(CHAR_GAP_MS);
            }

            // Red after each word, except the very last one which gets green
            if (w < words.length - 1) {
                blink(LEDColour.RED, "|word|");
                controller.waitMilliseconds(WORD_GAP_MS);
            }
        }

        // Green blink signals end of the whole message
        blink(LEDColour.GREEN, "|EOM|");
        UI.print("\nDelivery complete.");
    }

    private void blink(LEDColour colour, String label) {
        UI.printInline(label);
        controller.setUnderlights(colour);
        controller.waitMilliseconds(BLINK_ON_MS);
        controller.setUnderlights(LEDColour.OFF);
        controller.waitMilliseconds(BLINK_OFF_MS);
    }
}

// Stores the content of a single message along with who sent it,
// who it's for, and when it was recorded and delivered.
class Message {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Agent  sender;
    private final Agent  destination;
    private final String body;
    private final String morseRaw;
    private final String recordedTime;

    private String  deliveredTime = null;
    private boolean delivered     = false;

    Message(Agent sender, Agent destination, String body, String morseRaw) {
        this.sender       = sender;
        this.destination  = destination;
        this.body         = body;
        this.morseRaw     = morseRaw;
        this.recordedTime = LocalDateTime.now().format(FMT);
    }

    // Called once the message has been successfully delivered at the destination
    void markDelivered() {
        this.delivered     = true;
        this.deliveredTime = LocalDateTime.now().format(FMT);
    }

    Agent  getSender()        { return sender; }
    Agent  getDestination()   { return destination; }
    String getBody()          { return body; }
    String getMorseRaw()      { return morseRaw; }
    String getRecordedTime()  { return recordedTime; }
    String getDeliveredTime() { return deliveredTime; }
    boolean isDelivered()     { return delivered; }

    @Override
    public String toString() {
        return "Message[from=" + sender.getCallSign()
                + ", to=" + destination.getCallSign()
                + ", recorded=" + recordedTime
                + ", delivered=" + (delivered ? deliveredTime : "PENDING")
                + ", body=" + body + "]";
    }
}

// Keeps track of all messages sent during this session.
// Messages are stored in memory so they won't survive a restart.
class MessageManager {

    private final List<Message> messages = new ArrayList<>();

    void storeMessage(Message message) {
        messages.add(message);
        UI.print("Message #" + messages.size() + " stored.");
    }

    // Returns any undelivered messages addressed to a given location
    List<Message> getPendingFor(String location) {
        List<Message> pending = new ArrayList<>();
        for (Message m : messages) {
            if (m.getDestination().getLocation().equalsIgnoreCase(location)
                    && !m.isDelivered()) {
                pending.add(m);
            }
        }
        return pending;
    }

    List<Message> getAll() {
        return new ArrayList<>(messages);
    }
}

// Handles writing the communication log to a text file.
// The filename includes the date and time so logs don't overwrite each other.
class Logger {

    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String       logPath;
    private final List<String> entries = new ArrayList<>();

    Logger() {
        logPath = "spybot_log_" + LocalDateTime.now().format(FILE_FMT) + ".txt";
    }

    // Adds a message to the in-memory list of entries
    void logMessage(Message message) {
        String entry = "--- MESSAGE ---\n"
                + "Sender:    " + message.getSender().getCallSign()
                + " @ " + message.getSender().getLocation() + "\n"
                + "Receiver:  " + message.getDestination().getCallSign()
                + " @ " + message.getDestination().getLocation() + "\n"
                + "Body:      " + message.getBody() + "\n"
                + "Morse:     " + message.getMorseRaw() + "\n"
                + "Recorded:  " + message.getRecordedTime() + "\n"
                + "Delivered: " + (message.isDelivered()
                        ? message.getDeliveredTime() : "PENDING") + "\n";
        entries.add(entry);
        UI.print("Message logged.");
    }

    // Writes everything to disk when the program exits
    void writeLog() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logPath))) {
            bw.write("=== SPYBOT COMMUNICATION LOG ===\n");
            bw.write("Generated: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "\n\n");

            if (entries.isEmpty()) {
                bw.write("No messages communicated this session.\n");
            } else {
                for (String e : entries) {
                    bw.write(e);
                    bw.newLine();
                }
            }
            bw.write("=== END OF LOG ===\n");
        } catch (IOException ex) {
            UI.printError("Could not write log: " + ex.getMessage());
        }
    }

    String getLogPath() { return logPath; }

    // Used by the replay feature to show logged messages on screen
    List<String> getEntries() { return new ArrayList<>(entries); }
}

// Enum for all the LED colours used in the program.
// toRGB() converts each colour to the int array that fillUnderlights() expects.
enum LEDColour {
    WHITE, BLUE, AMBER, RED, GREEN, YELLOW, OFF;

    int[] toRGB() {
        switch (this) {
            case WHITE:  return new int[]{255, 255, 255};
            case BLUE:   return new int[]{0,   0,   255};
            case AMBER:  return new int[]{255, 191, 0};
            case RED:    return new int[]{255, 0,   0};
            case GREEN:  return new int[]{0,   255, 0};
            case YELLOW: return new int[]{255, 255, 0};
            case OFF:
            default:     return new int[]{0,   0,   0};
        }
    }
}

// Handles all direct interaction with the SwiftBot hardware.
// Wraps movement, LEDs, buttons, and the QR camera into clean methods
// so the rest of the code doesn't need to know the API details.
//
// The three safe houses form an equilateral triangle with 50cm sides (1:40 scale).
// Every node faces into the triangle, so from any node:
//   left neighbour  = 60 degree left turn
//   right neighbour = 60 degree right turn
// Heading resets to 0 on every arrival so turns never accumulate.
class SwiftBotController {

    // These values were measured by physically running the robot and timing it.
    // SPEED_RIGHT is slightly higher than SPEED_LEFT to correct a drift issue
    // where the right wheel runs a bit slower than the left.
    private static final int    SPEED_VALUE   = 40;
    private static final int    SPEED_LEFT    = 40;
    private static final int    SPEED_RIGHT   = 49;
    private static final double MS_PER_CM     = 55.8;
    private static final double TRIANGLE_SIDE = 50.0;
    private static final long   TRAVEL_MS     = (long)(TRIANGLE_SIDE * MS_PER_CM);
    private static final long   TURN_90_MS    = 550;
    private static final long   TURN_60_MS    = (long)(TURN_90_MS * 60.0 / 90.0);

    private final SwiftBotAPI swiftBot;
    private String currentLocation = "A";
    private int    headingDegrees  = 0;

    SwiftBotController() {
        swiftBot = SwiftBotAPI.INSTANCE;
    }

    // Scans a QR code using the camera.
    // Keeps retrying until a valid code is read or the user presses X to cancel.
    String scanQRCode() {
        String result = null;
        while (result == null || result.trim().isEmpty()) {
            UI.print("Press A to scan QR code, or X to cancel...");
            char btn = waitForButton(new char[]{'A', 'X'});

            if (btn == 'X') {
                UI.print("Cancelled. Returning to main menu.");
                return null;
            }

            try {
                java.awt.image.BufferedImage img = swiftBot.getQRImage();
                result = swiftBot.decodeQRImage(img);
                if (result == null || result.trim().isEmpty()) {
                    UI.printError("QR not detected. Try again or press X to cancel.");
                    result = null;
                } else {
                    result = result.trim();
                }
            } catch (Exception e) {
                UI.printError("Camera error: " + e.getMessage());
            }
        }
        return result;
    }

    // Waits for one of the specified buttons to be pressed.
    // Uses the enableButton callback approach from the SwiftBot API.
    // An AtomicBoolean is used to safely communicate between the callback thread
    // and the main thread without needing synchronisation.
    char waitForButton(char[] valid) {
        java.util.concurrent.atomic.AtomicBoolean pressed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        char[] result = new char[]{0};

        swiftBot.disableAllButtons();

        for (char btn : valid) {
            Button b = toButton(btn);
            if (b == null) continue;
            final char btnChar = btn;
            swiftBot.enableButton(b, () -> {
                if (!pressed.get()) {
                    result[0] = btnChar;
                    pressed.set(true);
                }
            });
        }

        while (!pressed.get()) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        swiftBot.disableAllButtons();
        return result[0];
    }

    private Button toButton(char c) {
        switch (Character.toUpperCase(c)) {
            case 'A': return Button.A;
            case 'B': return Button.B;
            case 'X': return Button.X;
            case 'Y': return Button.Y;
            default:  return null;
        }
    }

    // Sets all four underlights to the same colour
    void setUnderlights(LEDColour colour) {
        int[] rgb = colour.toRGB();
        if (colour == LEDColour.OFF) {
            swiftBot.disableUnderlights();
        } else {
            swiftBot.fillUnderlights(rgb);
        }
    }

    // Flashes the underlights a set number of times
    void blinkUnderlights(LEDColour colour, int times) {
        int[] rgb = colour.toRGB();
        for (int i = 0; i < times; i++) {
            swiftBot.fillUnderlights(rgb);
            waitMilliseconds(300);
            swiftBot.disableUnderlights();
            waitMilliseconds(300);
        }
    }

    // Drives from one safe house to another.
    // Turns to the correct heading first, then drives straight.
    void travelTo(String from, String to) {
        from = from.toUpperCase();
        to   = to.toUpperCase();

        if (from.equals(to)) {
            UI.print("Already at " + to + ".");
            return;
        }

        UI.print("Travelling " + from + " -> " + to + "...");
        driveSegment(from, to);
        currentLocation = to;
        headingDegrees  = 0; // reset so the next trip always starts fresh
        UI.print("Arrived at " + to + ".");
    }

    // Returns to origin by doing a 180 degree turn and driving straight back.
    // This is simpler and more reliable than trying to recalculate a new heading.
    void returnTo(String from, String to) {
        from = from.toUpperCase();
        to   = to.toUpperCase();

        if (from.equals(to)) {
            UI.print("Already at " + to + ".");
            return;
        }

        UI.print("Returning " + from + " -> " + to + "...");
        turn180();
        swiftBot.move(SPEED_LEFT, SPEED_RIGHT, (int) TRAVEL_MS);
        waitMilliseconds(500);
        currentLocation = to;
        headingDegrees  = 0;
        UI.print("Returned to " + to + ".");
    }

    // 1325ms was the calibrated value for a 180 degree turn at speed 40
    private void turn180() {
        long turn180Ms = 1325;
        UI.print("Turning 180 degrees...");
        swiftBot.move(SPEED_VALUE, -SPEED_VALUE, (int) turn180Ms);
        waitMilliseconds(300);
    }

    // Looks up the required turn direction then drives the edge
    private void driveSegment(String from, String to) {
        int target = headingFor(from, to);
        turnToHeading(target);
        swiftBot.move(SPEED_LEFT, SPEED_RIGHT, (int) TRAVEL_MS);
        waitMilliseconds(500);
    }

    // Every node faces into the triangle toward the opposite edge.
    // This means from any node, the left neighbour is always 60 left (300 degrees)
    // and the right neighbour is always 60 right (60 degrees).
    private int headingFor(String from, String to) {
        switch (from + to) {
            case "AB": return 300; // B is to the left of A
            case "AC": return 60;  // C is to the right of A
            case "BC": return 300; // C is to the left of B
            case "BA": return 60;  // A is to the right of B
            case "CA": return 300; // A is to the left of C
            case "CB": return 60;  // B is to the right of C
            default:   return headingDegrees;
        }
    }

    // Calculates how many 60-degree steps are needed and turns that many times.
    // delta <= 180 means turn right, otherwise turn left (shorter path).
    private void turnToHeading(int target) {
        int delta = ((target - headingDegrees) % 360 + 360) % 360;
        if (delta == 0) return;

        if (delta <= 180) {
            int steps = delta / 60;
            for (int i = 0; i < steps; i++) {
                swiftBot.move(SPEED_VALUE, -SPEED_VALUE, (int) TURN_60_MS);
                waitMilliseconds(200);
            }
        } else {
            int steps = (360 - delta) / 60;
            for (int i = 0; i < steps; i++) {
                swiftBot.move(-SPEED_VALUE, SPEED_VALUE, (int) TURN_60_MS);
                waitMilliseconds(200);
            }
        }
        headingDegrees = target;
    }

    void waitMilliseconds(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    void waitSeconds(int seconds) {
        waitMilliseconds(seconds * 1000L);
    }

    void shutdown() {
        swiftBot.move(0, 0, 100);
        setUnderlights(LEDColour.OFF);
    }

    String getCurrentLocation() { return currentLocation; }

    // Returns estimated travel time for a single edge of the triangle.
    // All sides are equal so this is always TRAVEL_MS regardless of which nodes.
    // Used to display the estimated time before the robot sets off.
    static long estimateTravelMs(String from, String to) {
        if (from == null || to == null || from.equalsIgnoreCase(to)) return 0;
        return TRAVEL_MS;
    }
}

// Handles all console output so formatting is consistent throughout the program.
// ANSI colour codes are used for the banner, errors and success messages.
class UI {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";
    private static final String CYAN  = "\u001B[36m";

    static void printBanner() {
        System.out.println(CYAN);
        System.out.println("  +-------------------------------------------------+");
        System.out.println("  |   ____              ____        _                |");
        System.out.println("  |  / ___| _ __  _   _| __ )  ___ | |_             |");
        System.out.println("  |  \\___ \\| '_ \\| | | |  _ \\ / _ \\| __|            |");
        System.out.println("  |   ___) | |_) | |_| | |_) | (_) | |_             |");
        System.out.println("  |  |____/| .__/ \\__, |____/ \\___/ \\__|            |");
        System.out.println("  |        |_|    |___/                              |");
        System.out.println("  +-------------------------------------------------+");
        System.out.println(RESET);
        System.out.println("  Secure Two-Way Morse Code Communication System");
        System.out.println("  CS1814 Software Implementation - Task 5");
        System.out.println("  Network: Safe Houses A | B | C");
        System.out.println("  Agents:  DELTA7@A  |  BRAVO9@B  |  ECHO3X@C");
        System.out.println("  =================================================\n");
    }

    static void printMainMenu() {
        System.out.println("\n  +--------------------------------+");
        System.out.println("  |       SPYBOT MAIN MENU         |");
        System.out.println("  +--------------------------------+");
        System.out.println("  |  [A]  Send a message           |");
        System.out.println("  |  [B]  Check pending messages   |");
        System.out.println("  |  [Y]  Replay message log       |");
        System.out.println("  |  [X]  Exit                     |");
        System.out.println("  +--------------------------------+");
    }

    static void print(String msg)        { System.out.println("  " + msg); }
    static void printInline(String msg)  { System.out.print(msg); }
    static void printSeparator()         { System.out.println("  --------------------------------------------------"); }

    static void printError(String msg) {
        System.out.println(RED + "  [ERROR] " + msg + RESET);
    }

    static void printSuccess(String msg) {
        System.out.println(GREEN + "  [OK] " + msg + RESET);
    }
}
