package src_SnakeAndLadder_Minhaj;

import swiftbot.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

class Player {
    private final String name;
    private int position;
    private final boolean isHuman;

    Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        this.position = 1; // start at square 1
    }

    String getName() {
        return name;
    }

    int getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    boolean isHuman() {
        return isHuman;
    }
}

class SnakeLadder {
    private final int start;
    private final int end;
    private final boolean isSnake;

    SnakeLadder(int start, int end, boolean isSnake) {
        this.start = start;
        this.end = end;
        this.isSnake = isSnake;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    boolean isSnake() {
        return isSnake;
    }
}

class Board {
    private final int size = 25;
    private final List<SnakeLadder> snakesAndLadders;

    Board() {
        snakesAndLadders = new ArrayList<>();
        // Example configuration (you can adjust if needed)
        snakesAndLadders.add(new SnakeLadder(14, 7, true));  // snake
        snakesAndLadders.add(new SnakeLadder(22, 10, true)); // snake
        snakesAndLadders.add(new SnakeLadder(3, 11, false)); // ladder
        snakesAndLadders.add(new SnakeLadder(8, 20, false)); // ladder
    }

    int getSize() {
        return size;
    }

    int applySnakeOrLadder(int position) {
        for (SnakeLadder snakeLadder : snakesAndLadders) {
            if (snakeLadder.getStart() == position) {
                return snakeLadder.getEnd();
            }
        }
        return position;
    }

    SnakeLadder getSnakeLadderAt(int position) {
        for (SnakeLadder snakeLadder : snakesAndLadders) {
            if (snakeLadder.getStart() == position) {
                return snakeLadder;
            }
        }
        return null;
    }
}

class SwiftBotController {
    private final SwiftBotAPI bot;
    private Orientation orientation;

    private static final int MOVE_SPEED = 50;
    private static final int MOVE_ONE_SQUARE_TIME_MS = 1000;
    private static final int TURN_SPEED = 40;
    private static final int TURN_90_TIME_MS = 650;

    enum Orientation {
        NORTH, EAST, SOUTH, WEST
    }

    SwiftBotController() {
        this.bot = SwiftBotAPI.INSTANCE;
        this.orientation = Orientation.NORTH;
    }

    void moveOneSquare() {
        try {
            bot.fillUnderlights(new int[]{0, 255, 0}); // green
            bot.move(MOVE_SPEED, MOVE_SPEED, MOVE_ONE_SQUARE_TIME_MS);
            bot.stopMove();
            bot.disableUnderlights();
        } catch (Exception e) {
            System.out.println("Error moving SwiftBot: " + e.getMessage());
        }
    }

    void turnLeft() {
        try {
            bot.move(-TURN_SPEED, TURN_SPEED, TURN_90_TIME_MS);
            bot.stopMove();
            updateOrientationLeft();
        } catch (Exception e) {
            System.out.println("Error turning left: " + e.getMessage());
        }
    }

    void turnRight() {
        try {
            bot.move(TURN_SPEED, -TURN_SPEED, TURN_90_TIME_MS);
            bot.stopMove();
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

    void snakeLED() {
        bot.fillUnderlights(new int[]{255, 0, 0}); // red
        sleep(500);
        bot.disableUnderlights();
    }

    void ladderLED() {
        bot.fillUnderlights(new int[]{0, 0, 255}); // blue
        sleep(500);
        bot.disableUnderlights();
    }

    void winLED() {
        for (int i = 0; i < 3; i++) {
            bot.fillUnderlights(new int[]{0, 255, 0});
            sleep(300);
            bot.disableUnderlights();
            sleep(300);
        }
    }

    void normalMoveLED() {
        bot.fillUnderlights(new int[]{0, 255, 0});
        sleep(200);
        bot.disableUnderlights();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}

class Game {
    private final Board board;
    private final Player human;
    private final Player botPlayer;
    private final SwiftBotController controller;
    private final Random random;
    private final Scanner scanner;
    private FileWriter logWriter;

    Game() {
        this.board = new Board();
        this.human = new Player("Human", true);
        this.botPlayer = new Player("SwiftBot", false);
        this.controller = new SwiftBotController();
        this.random = new Random();
        this.scanner = new Scanner(System.in);
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

    void selectMode() {
        System.out.println("Select Mode: A = Normal, B = Override");
        String input = scanner.nextLine().trim().toUpperCase();
        if (input.equals("B")) {
            log("Mode B (Override) selected.");
        } else {
            log("Mode A (Normal) selected.");
        }
    }

    private int rollDice() {
        int roll = random.nextInt(6) + 1;
        log("Dice roll: " + roll);
        return roll;
    }

    void start() {
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
            int afterSnakeOrLadder = board.applySnakeOrLadder(newPos);
            if (afterSnakeOrLadder != newPos) {
                SnakeLadder snakeLadder = board.getSnakeLadderAt(newPos);
                if (snakeLadder != null && snakeLadder.isSnake()) {
                    log("Snake! " + current.getName() + " moves down to " + afterSnakeOrLadder);
                    controller.snakeLED();
                } else {
                    log("Ladder! " + current.getName() + " climbs to " + afterSnakeOrLadder);
                    controller.ladderLED();
                }
                moveSwiftBotSquareBySquare(newPos, afterSnakeOrLadder);
                current.setPosition(afterSnakeOrLadder);
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
        }
        return botPlayer;
    }

    private void moveSwiftBotSquareBySquare(int from, int to) {
        if (from == to) {
            return;
        }
        int step = (to > from) ? 1 : -1;
        for (int position = from; position != to; position += step) {
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

public class SnakeandLadder {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
