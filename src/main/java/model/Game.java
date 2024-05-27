package model;

import AI.Algorithm;
import GUI.GameUI;
import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class Game {
    private final Board board;
    private final Player firstPlayer;
    private final Player secondPlayer;
    private Player playerInTurn;
    private final GameUI gui;

    private final Algorithm algorithm;

    public Game(GameUI gui, boolean isFirstPlayerAI, boolean isSecondPlayerAI) {
        this.gui = gui;
        board = new Board(9);
        firstPlayer = new Player("First Player", isFirstPlayerAI, board.getBoardLength() - 1, board.getBoardLength() / 2, 0, Color.CYAN);
        secondPlayer = new Player("Second Player", isSecondPlayerAI, 0, board.getBoardLength() / 2, board.getBoardLength() - 1, Color.ORANGE);
        board.getOneCell(firstPlayer.getRow(), firstPlayer.getCol()).setPlayer(firstPlayer);
        board.getOneCell(secondPlayer.getRow(), secondPlayer.getCol()).setPlayer(secondPlayer);
        board.setFirstPlayer(firstPlayer);
        board.setSecondPlayer(secondPlayer);
        playerInTurn = firstPlayer;
        algorithm = new Algorithm();

        play();
    }

    private int[] getMouseClickCoordinates() {
        try {
            return gui.getMouseClickCoordinates();
        } catch (Exception e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    public void play() {
        new Thread(() -> {
            while (true) {
                if (playerInTurn.isAI()) {
                    Move move = algorithm.generateMove();
                    makeMove(move);
                } else {
                    drawPossiblePawnMoves();
                    do {
                        int[] coordinates = getMouseClickCoordinates();
                        if (Arrays.equals(coordinates, new int[]{0, 0})) {
                            continue;
                        }
                        Move move = calculateMove(coordinates);
                        if (board.isLegalMove(move)) {
                            makeMove(move);
                            break;
                        }
                    } while (true);
                    deletePossiblePawnMoves();
                }
                if (isGameOver()) {
                    Platform.runLater(() -> gui.endGame(playerInTurn));
                    break;
                }
                playerInTurn = playerInTurn == firstPlayer ? secondPlayer : firstPlayer;
            }
        }).start();
    }

    private void drawPossiblePawnMoves() {
        Platform.runLater(() -> gui.drawPossiblePawnMoves(board.getPossiblePawnMoves(playerInTurn)));
    }

    private void deletePossiblePawnMoves() {
        Platform.runLater(gui::deletePossiblePawnMoves);
    }

    private void makeMove(Move move) {
        Platform.runLater(() -> gui.draw(move));
        board.update(move);
        playerInTurn.update(move);
    }

    private Move calculateMove(int[] coordinates) {
        int x = coordinates[0] - 6;
        int y = coordinates[1] - 6;

        int cellSize = 44;
        int wallThickness = 12;
        int totalDimension = cellSize + wallThickness;

        int cellY = x / totalDimension;
        int cellX = y / totalDimension;

        if (cellX > 8 || cellY > 8 || cellX < 0 || cellY < 0) {
            return new Move(playerInTurn, MoveType.WALL_PLACE, 10, 10, true);
        }

        MoveType moveType;
        boolean isHorizontal = true;

        if (x % totalDimension < cellSize && y % totalDimension < cellSize) {
            moveType = MoveType.PAWN_MOVE;
        } else {
            moveType = MoveType.WALL_PLACE;
            if (x % totalDimension >= cellSize && y % totalDimension < cellSize || x % totalDimension >= cellSize && x % totalDimension > y % totalDimension)
                isHorizontal = false;
        }

        return new Move(playerInTurn, moveType, cellX, cellY, isHorizontal);
    }

    private boolean isGameOver() {
        return playerInTurn.getRow() == playerInTurn.getFinishRow();
    }

}
