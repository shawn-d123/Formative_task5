public class Player {
    private final String name;
    private int position;
    private boolean isHuman;

    public Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        this.position = 1; // start at square 1
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isHuman() {
        return isHuman;
    }
}


public class SnakeLadder {
    private final int start;
    private final int end;
    private final boolean isSnake;

    public SnakeLadder(int start, int end, boolean isSnake) {
        this.start = start;
        this.end = end;
        this.isSnake = isSnake;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isSnake() {
        return isSnake;
    }
}




import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int size = 25;
    private final List<SnakeLadder> snakesAndLadders;

    public Board() {
        snakesAndLadders = new ArrayList<>();
        // Example configuration (you can adjust if needed)
        snakesAndLadders.add(new SnakeLadder(14, 7, true));  // snake
        snakesAndLadders.add(new SnakeLadder(22, 10, true)); // snake
        snakesAndLadders.add(new SnakeLadder(3, 11, false)); // ladder
        snakesAndLadders.add(new SnakeLadder(8, 20, false)); // ladder
    }

    public int getSize() {
        return size;
    }

    public int applySnakeOrLadder(int position) {
        for (SnakeLadder sl : snakesAndLadders) {
            if (sl.getStart() == position) {
                return sl.getEnd();
            }
        }
        return position;
    }

    public SnakeLadder getSnakeLadderAt(int position) {
        for (SnakeLadder sl : snakesAndLadders) {
            if (sl.getStart() == position) {
                return sl;
            }
        }
        return null;
    }
}

// NOTE: Adjust the package/import to match your project setup.
import swiftbot.api.SwiftBotAPI;

public class SwiftBotController {

    private final SwiftBotAPI bot;
    private Orientation orientation;

    private static final double SQUARE_DISTANCE_METRES = 0.25; // 25 cm

    public enum Orientation {
        NORTH, EAST, SOUTH, WEST
    }

    public SwiftBotController() {
        this.bot = SwiftBotAPI.INSTANCE;
        this.orientation = Orientation.NORTH;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void moveOneSquare() {
        try {
            bot.setAllLED(0, 255, 0); // green
            bot.move(SQUARE_DISTANCE_METRES, 0.2);
            bot.stop();
            bot.setAllLED(0, 0, 0);
        } catch (Exception e) {
            System.out.println("Error moving SwiftBot: " + e.getMessage());
        }
    }

    public void turnLeft() {
        try {
            bot.turn(-90, 0.2);
            updateOrientationLeft();
        } catch (Exception e) {
            System.out.println("Error turning left: " + e.getMessage());
        }
    }

    public void turnRight() {
        try {
            bot.turn(90, 0.2);
            updateOrientationRight();
        } catch (Exception e) {
            System.out.println("Error turning right: " + e.getMessage());
        }
    }

    private void updateOrientationLeft() {
        switch (orientation) {
            case NORTH -> orientation = Orientation.WEST;
            case WEST -> orientation = Orientation.SOUTH;
            case SOUTH -> orientation = Orientation.EAST;
            case EAST -> orientation = Orientation.NORTH;
        }
    }

    private void updateOrientationRight() {
        switch (orientation) {
            case NORTH -> orientation = Orientation.EAST;
            case EAST -> orientation = Orientation.SOUTH;
            case SOUTH -> orientation = Orientation.WEST;
            case WEST -> orientation = Orientation.NORTH;
        }
    }

    public void snakeLED() {
        bot.setAllLED(255, 0, 0); // red
        sleep(500);
        bot.setAllLED(0, 0, 0);
    }

    public void ladderLED() {
        bot.setAllLED(0, 0, 255); // blue
        sleep(500);
        bot.setAllLED(0, 0, 0);
    }

    public void winLED() {
        for (int i = 0; i < 3; i++) {
            bot.setAllLED(0, 255, 0);
            sleep(300);
            bot.setAllLED(0, 0, 0);
            sleep(300);
        }
    }

    public void normalMoveLED() {
        bot.setAllLED(0, 255, 0);
        sleep(200);
        bot.setAllLED(0, 0, 0);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}


// NOTE: Adjust the package/import to match your project setup.
import swiftbot.api.SwiftBotAPI;

public class SwiftBotController {

    private final SwiftBotAPI bot;
    private Orientation orientation;

    private static final double SQUARE_DISTANCE_METRES = 0.25; // 25 cm

    public enum Orientation {
        NORTH, EAST, SOUTH, WEST
    }

    public SwiftBotController() {
        this.bot = SwiftBotAPI.INSTANCE;
        this.orientation = Orientation.NORTH;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void moveOneSquare() {
        try {
            bot.setAllLED(0, 255, 0); // green
            bot.move(SQUARE_DISTANCE_METRES, 0.2);
            bot.stop();
            bot.setAllLED(0, 0, 0);
        } catch (Exception e) {
            System.out.println("Error moving SwiftBot: " + e.getMessage());
        }
    }

    public void turnLeft() {
        try {
            bot.turn(-90, 0.2);
            updateOrientationLeft();
        } catch (Exception e) {
            System.out.println("Error turning left: " + e.getMessage());
        }
    }

    public void turnRight() {
        try {
            bot.turn(90, 0.2);
            updateOrientationRight();
        } catch (Exception e) {
            System.out.println("Error turning right: " + e.getMessage());
        }
    }

    private void updateOrientationLeft() {
        switch (orientation) {
            case NORTH -> orientation = Orientation.WEST;
            case WEST -> orientation = Orientation.SOUTH;
            case SOUTH -> orientation = Orientation.EAST;
            case EAST -> orientation = Orientation.NORTH;
        }
    }

    private void updateOrientationRight() {
        switch (orientation) {
            case NORTH -> orientation = Orientation.EAST;
            case EAST -> orientation = Orientation.SOUTH;
            case SOUTH -> orientation = Orientation.WEST;
            case WEST -> orientation = Orientation.NORTH;
        }
    }

    public void snakeLED() {
        bot.setAllLED(255, 0, 0); // red
        sleep(500);
        bot.setAllLED(0, 0, 0);
    }

    public void ladderLED() {
        bot.setAllLED(0, 0, 255); // blue
        sleep(500);
        bot.setAllLED(0, 0, 0);
    }

    public void winLED() {
        for (int i = 0; i < 3; i++) {
            bot.setAllLED(0, 255, 0);
            sleep(300);
            bot.setAllLED(0, 0, 0);
            sleep(300);
        }
    }

    public void normalMoveLED() {
        bot.setAllLED(0, 255, 0);
        sleep(200);
        bot.setAllLED(0, 0, 0);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}


import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Scanner;

public class Game {

    private final Board board;
    private final Player human;
    private final Player botPlayer;
    private final SwiftBotController controller;
    private final Random random;
    private final Scanner scanner;
    private boolean modeOverride;
    private FileWriter logWriter;

    public Game() {
        this.board = new Board();
        this.human = new Player("Human", true);
        this.botPlayer = new Player("SwiftBot", false);
        this.controller = new SwiftBotController();
        this.random = new Random();
        this.scanner = new Scanner(System.in);
        this.modeOverride = false;
        initLog();
    }

    private void initLog() {
        try {
            String fileName = "snakes_ladders_log_" + System.currentTimeMillis() + ".txt";
            logWriter = new FileWriter(fileName, true);
            log("Game started at: " + LocalDateTime.now());
        } catch (IOException e) {
            System.out.println("Could not create log file: " + e.getMessage());
        }
    }

    private void log(String message) {
        System.out.println(message);
        if (logWriter != null) {
            try {
                logWriter.write(message + System.lineSeparator());
                logWriter.flush();
            } catch (IOException e) {
                System.out.println("Error writing to log: " + e.getMessage());
            }
        }
    }

    public void selectMode() {
        System.out.println("Select Mode: A = Normal, B = Override");
        String input = scanner.nextLine().trim().toUpperCase();
        if (input.equals("B")) {
            modeOverride = true;
            log("Mode B (Override) selected.");
        } else {
            modeOverride = false;
            log("Mode A (Normal) selected.");
        }
    }

    private int rollDice() {
        int roll = random.nextInt(6) + 1;
        log("Dice roll: " + roll);
        return roll;
    }

    public void start() {
        log("Initialising Snakes & Ladders game...");
        selectMode();

        Player current = decideStartingPlayer();
        log("Starting player: " + current.getName());

        boolean gameOver = false;
        while (!gameOver) {
            log("Current player: " + current.getName());
            int roll;

            if (current.isHuman()) {
                System.out.println("Press ENTER to roll the dice...");
                scanner.nextLine();
                roll = rollDice();
            } else {
                roll = rollDice();
            }

            int oldPos = current.getPosition();
            int newPos = oldPos + roll;

            if (newPos > board.getSize()) {
                log("Roll too high, cannot move.");
                current = switchPlayer(current);
                continue;
            }

            log(current.getName() + " moves from " + oldPos + " to " + newPos);
            moveSwiftBotSquareBySquare(oldPos, newPos);

            current.setPosition(newPos);

            // Check snake or ladder
            int afterSL = board.applySnakeOrLadder(newPos);
            if (afterSL != newPos) {
                SnakeLadder sl = board.getSnakeLadderAt(newPos);
                if (sl != null && sl.isSnake()) {
                    log("Snake! " + current.getName() + " moves down to " + afterSL);
                    controller.snakeLED();
                } else {
                    log("Ladder! " + current.getName() + " climbs to " + afterSL);
                    controller.ladderLED();
                }
                moveSwiftBotSquareBySquare(newPos, afterSL);
                current.setPosition(afterSL);
            }

            // Check win
            if (current.getPosition() == board.getSize()) {
                log("Winner: " + current.getName());
                controller.winLED();
                gameOver = true;
            } else {
                current = switchPlayer(current);
            }
        }

        closeLog();
    }

    private Player switchPlayer(Player current) {
        return current == human ? botPlayer : human;
    }

    private Player decideStartingPlayer() {
        int humanRoll = rollDice();
        int botRoll = rollDice();
        log("Human roll: " + humanRoll + ", SwiftBot roll: " + botRoll);
        if (humanRoll >= botRoll) {
            return human;
        } else {
            return botPlayer;
        }
    }

    private void moveSwiftBotSquareBySquare(int from, int to) {
        if (from == to) return;
        int step = (to > from) ? 1 : -1;
        for (int pos = from; pos != to; pos += step) {
            controller.normalMoveLED();
            controller.moveOneSquare();
        }
    }

    private void closeLog() {
        log("Game ended at: " + LocalDateTime.now());
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException ignored) {
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
