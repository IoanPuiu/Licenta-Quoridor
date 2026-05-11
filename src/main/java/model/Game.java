package model;

import AI.Algorithm;
import AI.GameState;
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
        this(gui,
                isFirstPlayerAI ? PlayerType.MINIMAX : PlayerType.HUMAN,
                isSecondPlayerAI ? PlayerType.MINIMAX : PlayerType.HUMAN);
    }

    public Game(GameUI gui, PlayerType firstPlayerType, PlayerType secondPlayerType) {
        this.gui = gui;
        board = new Board(9);
        firstPlayer = new Player("First Player", firstPlayerType, board.getBoardLength() - 1, board.getBoardLength() / 2, 0, Color.CYAN);
        secondPlayer = new Player("Second Player", secondPlayerType, 0, board.getBoardLength() / 2, board.getBoardLength() - 1, Color.ORANGE);
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
                    Player opponent = playerInTurn == firstPlayer ? secondPlayer : firstPlayer;
                    GameState state = new GameState(board, playerInTurn, opponent);
                    int moveCode = algorithm.generateMove(state, playerInTurn.getPlayerType());
                    Move move = createMoveFromCode(moveCode);
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

    private Move createMoveFromCode(int moveCode) {
        int boardLength = board.getBoardLength();

        if (GameState.isPawnMoveCode(moveCode)) {
            return new Move(
                    playerInTurn,
                    MoveType.PAWN_MOVE,
                    GameState.decodePawnMoveRow(moveCode, boardLength),
                    GameState.decodePawnMoveCol(moveCode, boardLength));
        }

        return new Move(
                playerInTurn,
                MoveType.WALL_PLACE,
                GameState.decodeWallRow(moveCode, boardLength),
                GameState.decodeWallCol(moveCode, boardLength),
                GameState.decodeWallIsHorizontal(moveCode));
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
