package model;

import AI.Algorithm;
import AI.GameState;
import GUI.GameUI;
import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {
    private final Object gameLock = new Object();
    private Board board;
    private final Player firstPlayer;
    private final Player secondPlayer;
    private Player playerInTurn;
    private final GameUI gui;
    private final List<Move> moveHistory;
    private int stateVersion;
    private boolean gameOver;
    private boolean gameLoopRunning;

    private final Algorithm algorithm;

    public Game(GameUI gui, boolean isFirstPlayerAI, boolean isSecondPlayerAI) {
        this(gui,
                new PlayerProfile(isFirstPlayerAI ? PlayerType.MINIMAX : PlayerType.HUMAN, "First Player"),
                new PlayerProfile(isSecondPlayerAI ? PlayerType.MINIMAX : PlayerType.HUMAN, "Second Player"));
    }

    public Game(GameUI gui, PlayerType firstPlayerType, PlayerType secondPlayerType) {
        this(gui, new PlayerProfile(firstPlayerType, "First Player"), new PlayerProfile(secondPlayerType, "Second Player"));
    }

    public Game(GameUI gui, PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile) {
        this.gui = gui;
        board = new Board(9);
        firstPlayer = new Player(
                firstPlayerProfile.displayName("First Player"),
                firstPlayerProfile.playerType(),
                board.getBoardLength() - 1,
                board.getBoardLength() / 2,
                0,
                Color.CYAN);
        secondPlayer = new Player(
                secondPlayerProfile.displayName("Second Player"),
                secondPlayerProfile.playerType(),
                0,
                board.getBoardLength() / 2,
                board.getBoardLength() - 1,
                Color.ORANGE);
        board.getOneCell(firstPlayer.getRow(), firstPlayer.getCol()).setPlayer(firstPlayer);
        board.getOneCell(secondPlayer.getRow(), secondPlayer.getCol()).setPlayer(secondPlayer);
        board.setFirstPlayer(firstPlayer);
        board.setSecondPlayer(secondPlayer);
        playerInTurn = firstPlayer;
        algorithm = new Algorithm();
        moveHistory = new ArrayList<>();
        stateVersion = 0;
        gameOver = false;
        gameLoopRunning = false;

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
        synchronized (gameLock) {
            if (gameLoopRunning) {
                return;
            }
            gameLoopRunning = true;
        }

        new Thread(() -> {
            try {
                while (true) {
                    Player currentPlayer;
                    int currentVersion;
                    synchronized (gameLock) {
                        if (gameOver) {
                            break;
                        }
                        currentPlayer = playerInTurn;
                        currentVersion = stateVersion;
                    }

                    boolean moveMade;
                    if (currentPlayer.isAI()) {
                        moveMade = playAiTurn(currentPlayer, currentVersion);
                    } else {
                        drawPossiblePawnMoves(currentPlayer, currentVersion);
                        moveMade = playHumanTurn(currentPlayer, currentVersion);
                        deletePossiblePawnMoves();
                    }

                    if (!moveMade) {
                        continue;
                    }

                    Player winner = finishTurn(currentPlayer, currentVersion);
                    if (winner != null) {
                        Platform.runLater(() -> gui.endGame(winner));
                        break;
                    }
                }
            } finally {
                synchronized (gameLock) {
                    gameLoopRunning = false;
                }
            }
        }).start();
    }

    private boolean playAiTurn(Player currentPlayer, int currentVersion) {
        Player opponent = opponentOf(currentPlayer);
        GameState state;
        synchronized (gameLock) {
            if (!isCurrentTurn(currentPlayer, currentVersion)) {
                return false;
            }
            state = new GameState(board, currentPlayer, opponent);
        }

        int moveCode = algorithm.generateMove(state, currentPlayer.getPlayerType());
        Move move = createMoveFromCode(moveCode, currentPlayer);
        return makeMoveIfCurrent(move, currentPlayer, currentVersion);
    }

    private boolean playHumanTurn(Player currentPlayer, int currentVersion) {
        do {
            int[] coordinates = getMouseClickCoordinates();
            if (!isCurrentTurn(currentPlayer, currentVersion)) {
                return false;
            }
            if (Arrays.equals(coordinates, new int[]{0, 0})) {
                continue;
            }

            Move move = calculateMove(coordinates, currentPlayer);
            if (makeLegalHumanMove(move, currentPlayer, currentVersion)) {
                return true;
            }
        } while (true);
    }

    private void drawPossiblePawnMoves(Player player, int currentVersion) {
        List<Move> possibleMoves;
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return;
            }
            possibleMoves = board.getPossiblePawnMoves(player);
        }
        Platform.runLater(() -> gui.drawPossiblePawnMoves(possibleMoves));
    }

    private void deletePossiblePawnMoves() {
        Platform.runLater(gui::deletePossiblePawnMoves);
    }

    private boolean makeLegalHumanMove(Move move, Player currentPlayer, int currentVersion) {
        synchronized (gameLock) {
            if (!isCurrentTurn(currentPlayer, currentVersion) || !board.isLegalMove(move)) {
                return false;
            }
        }

        return makeMoveIfCurrent(move, currentPlayer, currentVersion);
    }

    private boolean makeMoveIfCurrent(Move move, Player currentPlayer, int currentVersion) {
        boolean undoAvailable;
        synchronized (gameLock) {
            if (!isCurrentTurn(currentPlayer, currentVersion)) {
                return false;
            }
            board.update(move);
            currentPlayer.update(move);
            moveHistory.add(move);
            undoAvailable = hasHumanMoveInHistory();
        }

        Platform.runLater(() -> {
            gui.draw(move);
            gui.setUndoAvailable(undoAvailable);
        });
        return true;
    }

    private Move createMoveFromCode(int moveCode, Player player) {
        int boardLength = board.getBoardLength();

        if (GameState.isPawnMoveCode(moveCode)) {
            return new Move(
                    player,
                    MoveType.PAWN_MOVE,
                    GameState.decodePawnMoveRow(moveCode, boardLength),
                    GameState.decodePawnMoveCol(moveCode, boardLength));
        }

        return new Move(
                player,
                MoveType.WALL_PLACE,
                GameState.decodeWallRow(moveCode, boardLength),
                GameState.decodeWallCol(moveCode, boardLength),
                GameState.decodeWallIsHorizontal(moveCode));
    }

    private Move calculateMove(int[] coordinates, Player player) {
        int x = coordinates[0] - 6;
        int y = coordinates[1] - 6;

        int cellSize = 44;
        int wallThickness = 12;
        int totalDimension = cellSize + wallThickness;

        int cellY = x / totalDimension;
        int cellX = y / totalDimension;

        if (cellX > 8 || cellY > 8 || cellX < 0 || cellY < 0) {
            return new Move(player, MoveType.WALL_PLACE, 10, 10, true);
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

        return new Move(player, moveType, cellX, cellY, isHorizontal);
    }

    private Player finishTurn(Player currentPlayer, int currentVersion) {
        synchronized (gameLock) {
            if (!isCurrentTurn(currentPlayer, currentVersion)) {
                return null;
            }

            if (isGameOver(currentPlayer)) {
                gameOver = true;
                return currentPlayer;
            }

            playerInTurn = opponentOf(currentPlayer);
            return null;
        }
    }

    public void undoToLastHumanMove() {
        List<Move> movesAfterUndo;
        int firstPlayerWalls;
        int secondPlayerWalls;
        boolean isFirstPlayerTurnAfterUndo;
        boolean undoAvailableAfterUndo;
        boolean shouldRestartGameLoop;

        synchronized (gameLock) {
            int undoIndex = lastHumanMoveIndex();
            if (undoIndex < 0) {
                return;
            }

            shouldRestartGameLoop = gameOver;
            gameOver = false;
            Player undoPlayer = moveHistory.get(undoIndex).getPlayer();
            moveHistory.subList(undoIndex, moveHistory.size()).clear();
            rebuildGameStateFromHistory();
            playerInTurn = undoPlayer;
            stateVersion++;

            movesAfterUndo = new ArrayList<>(moveHistory);
            firstPlayerWalls = firstPlayer.wallsLeft();
            secondPlayerWalls = secondPlayer.wallsLeft();
            isFirstPlayerTurnAfterUndo = playerInTurn == firstPlayer;
            undoAvailableAfterUndo = hasHumanMoveInHistory();
        }

        Platform.runLater(() -> {
            gui.redrawGame(movesAfterUndo, firstPlayerWalls, secondPlayerWalls, isFirstPlayerTurnAfterUndo);
            gui.setUndoAvailable(undoAvailableAfterUndo);
        });
        gui.wakeWaitingInput();
        if (shouldRestartGameLoop) {
            play();
        }
    }

    private int lastHumanMoveIndex() {
        for (int i = moveHistory.size() - 1; i >= 0; i--) {
            if (!moveHistory.get(i).getPlayer().isAI()) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasHumanMoveInHistory() {
        return lastHumanMoveIndex() >= 0;
    }

    private void rebuildGameStateFromHistory() {
        List<Move> movesToReplay = new ArrayList<>(moveHistory);
        board = new Board(9);
        firstPlayer.reset(board.getBoardLength() - 1, board.getBoardLength() / 2, 10);
        secondPlayer.reset(0, board.getBoardLength() / 2, 10);
        board.getOneCell(firstPlayer.getRow(), firstPlayer.getCol()).setPlayer(firstPlayer);
        board.getOneCell(secondPlayer.getRow(), secondPlayer.getCol()).setPlayer(secondPlayer);
        board.setFirstPlayer(firstPlayer);
        board.setSecondPlayer(secondPlayer);

        for (Move move : movesToReplay) {
            board.update(move);
            move.getPlayer().update(move);
        }
    }

    private boolean isCurrentTurn(Player player, int currentVersion) {
        return stateVersion == currentVersion && playerInTurn == player && !gameOver;
    }

    private Player opponentOf(Player player) {
        return player == firstPlayer ? secondPlayer : firstPlayer;
    }

    private boolean isGameOver(Player player) {
        return player.getRow() == player.getFinishRow();
    }

}
