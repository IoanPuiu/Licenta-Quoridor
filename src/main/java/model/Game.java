package model;

import AI.Algorithm;
import AI.GameState;
import GUI.GameUI;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {
    private final Object gameLock = new Object();
    private Board board;
    private Player currentPlayer;
    private Player opponent;
    private final GameUI gui;
    private final List<Move> moveHistory;
    private final List<MoveThinkingTime> thinkingTimes;
    private final int uiToken;
    private Thread gameThread;
    private int stateVersion;
    private boolean gameOver;
    private boolean gameLoopRunning;
    private Player winner;

    private final Algorithm algorithm;

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType) {
        this(gui, bottomPlayerType, topPlayerType, 0, true);
    }

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType, int uiToken) {
        this(gui, bottomPlayerType, topPlayerType, uiToken, true);
    }

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType, int uiToken, boolean isBottomPlayerStarting) {
        this.gui = gui;
        this.uiToken = uiToken;
        board = new Board(9);
        Player bottomPlayer = new Player(
                bottomPlayerType,
                board.getBoardLength() - 1,
                board.getBoardLength() / 2,
                0);
        Player topPlayer = new Player(
                topPlayerType,
                0,
                board.getBoardLength() / 2,
                board.getBoardLength() - 1);
        board.getOneCell(bottomPlayer.getRow(), bottomPlayer.getCol()).setPlayer(bottomPlayer);
        board.getOneCell(topPlayer.getRow(), topPlayer.getCol()).setPlayer(topPlayer);
        board.setFirstPlayer(bottomPlayer);
        board.setSecondPlayer(topPlayer);
        currentPlayer = isBottomPlayerStarting ? bottomPlayer : topPlayer;
        opponent = isBottomPlayerStarting ? topPlayer : bottomPlayer;
        algorithm = new Algorithm();
        moveHistory = new ArrayList<>();
        thinkingTimes = new ArrayList<>();
        stateVersion = 0;
        gameOver = false;
        gameLoopRunning = false;
        gameThread = null;
        winner = null;

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

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    Player turnPlayer;
                    int currentVersion;
                    synchronized (gameLock) {
                        if (gameOver) {
                            break;
                        }
                        turnPlayer = currentPlayer;
                        currentVersion = stateVersion;
                    }

                    boolean moveMade;
                    if (turnPlayer.isAI()) {
                        moveMade = playAiTurn(turnPlayer, currentVersion);
                    } else {
                        drawPossiblePawnMoves(turnPlayer, currentVersion);
                        moveMade = playHumanTurn(turnPlayer, currentVersion);
                        deletePossiblePawnMoves();
                    }

                    if (!moveMade) {
                        continue;
                    }

                    Player winner = finishTurn(turnPlayer, currentVersion);
                    if (winner != null) {
                        Platform.runLater(() -> gui.endGame(uiToken, winner));
                        break;
                    }
                }
            } finally {
                synchronized (gameLock) {
                    gameLoopRunning = false;
                    if (gameThread == Thread.currentThread()) {
                        gameThread = null;
                    }
                }
            }
        });
        thread.setDaemon(true);

        synchronized (gameLock) {
            gameThread = thread;
        }
        thread.start();
    }

    public void stop() {
        Thread threadToInterrupt;
        synchronized (gameLock) {
            gameOver = true;
            stateVersion++;
            threadToInterrupt = gameThread;
        }

        if (threadToInterrupt != null) {
            threadToInterrupt.interrupt();
        }
        gui.wakeWaitingInput(uiToken);
    }

    public Player forfeitCurrentPlayer() {
        Player forfeitureWinner;
        Thread threadToInterrupt;
        synchronized (gameLock) {
            if (gameOver) {
                return winner;
            }

            forfeitureWinner = opponent;
            winner = forfeitureWinner;
            gameOver = true;
            stateVersion++;
            threadToInterrupt = gameThread;
        }

        if (threadToInterrupt != null) {
            threadToInterrupt.interrupt();
        }
        gui.wakeWaitingInput(uiToken);
        return forfeitureWinner;
    }

    private boolean playAiTurn(Player player, int currentVersion) {
        Player currentOpponent;
        GameState state;
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return false;
            }
            currentOpponent = opponent;
            state = new GameState(board, player, currentOpponent);
        }

        long thinkingStartedAt = System.nanoTime();
        int moveCode = algorithm.generateMove(state, player.getPlayerType());
        long thinkingTimeNanos = System.nanoTime() - thinkingStartedAt;
        Move move = createMoveFromCode(moveCode, player);
        boolean moveMade = makeMoveIfCurrent(move, player, currentVersion);
        if (moveMade) {
            recordThinkingTime(move, thinkingTimeNanos);
        }
        return moveMade;
    }

    private boolean playHumanTurn(Player player, int currentVersion) {
        long thinkingStartedAt = System.nanoTime();
        do {
            int[] coordinates = getMouseClickCoordinates();
            if (!isCurrentTurn(player, currentVersion)) {
                return false;
            }
            if (Arrays.equals(coordinates, new int[]{0, 0})) {
                continue;
            }

            Move move = calculateMove(coordinates, player);
            if (makeLegalHumanMove(move, player, currentVersion)) {
                recordThinkingTime(move, System.nanoTime() - thinkingStartedAt);
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
        Platform.runLater(() -> gui.drawPossiblePawnMoves(uiToken, possibleMoves));
    }

    private void deletePossiblePawnMoves() {
        Platform.runLater(() -> gui.deletePossiblePawnMoves(uiToken));
    }

    private boolean makeLegalHumanMove(Move move, Player player, int currentVersion) {
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion) || !board.isLegalMove(move)) {
                return false;
            }
        }

        return makeMoveIfCurrent(move, player, currentVersion);
    }

    private boolean makeMoveIfCurrent(Move move, Player player, int currentVersion) {
        boolean undoAvailable;
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return false;
            }
            board.update(move);
            player.update(move);
            moveHistory.add(move);
            undoAvailable = hasHumanMoveInHistory();
        }

        Platform.runLater(() -> {
            gui.draw(uiToken, move);
            gui.setUndoAvailable(uiToken, undoAvailable);
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

    private Player finishTurn(Player player, int currentVersion) {
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return null;
            }

            if (isGameOver(player)) {
                gameOver = true;
                winner = player;
                return player;
            }

            Player nextPlayer = opponent;
            opponent = player;
            this.currentPlayer = nextPlayer;
            return null;
        }
    }

    public void undoToLastHumanMove() {
        List<Move> movesAfterUndo;
        int bottomPlayerWalls;
        int topPlayerWalls;
        boolean isFirstPlayerTurnAfterUndo;
        boolean undoAvailableAfterUndo;
        boolean shouldRestartGameLoop;
        ThinkingStats thinkingStats;

        synchronized (gameLock) {
            int undoIndex = lastHumanMoveIndex();
            if (undoIndex < 0) {
                return;
            }

            shouldRestartGameLoop = gameOver;
            gameOver = false;
            winner = null;
            Player undoPlayer = moveHistory.get(undoIndex).getPlayer();
            List<Move> removedMoves = new ArrayList<>(moveHistory.subList(undoIndex, moveHistory.size()));
            moveHistory.subList(undoIndex, moveHistory.size()).clear();
            thinkingTimes.removeIf(thinkingTime -> removedMoves.contains(thinkingTime.move()));
            rebuildGameStateFromHistory();
            currentPlayer = undoPlayer;
            opponent = otherPlayer(undoPlayer);
            stateVersion++;

            movesAfterUndo = new ArrayList<>(moveHistory);
            bottomPlayerWalls = bottomPlayer().wallsLeft();
            topPlayerWalls = topPlayer().wallsLeft();
            isFirstPlayerTurnAfterUndo = isBottomPlayer(currentPlayer);
            undoAvailableAfterUndo = hasHumanMoveInHistory();
            thinkingStats = thinkingStats();
        }

        Platform.runLater(() -> {
            gui.redrawGame(uiToken, movesAfterUndo, bottomPlayerWalls, topPlayerWalls, isFirstPlayerTurnAfterUndo);
            gui.setUndoAvailable(uiToken, undoAvailableAfterUndo);
            gui.updateThinkingTime(uiToken, thinkingStats);
        });
        gui.wakeWaitingInput(uiToken);
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

    private void recordThinkingTime(Move move, long thinkingTimeNanos) {
        ThinkingStats thinkingStats;
        synchronized (gameLock) {
            thinkingTimes.add(new MoveThinkingTime(move, thinkingTimeNanos));
            thinkingStats = thinkingStats();
        }

        Platform.runLater(() -> gui.updateThinkingTime(uiToken, thinkingStats));
    }

    private ThinkingStats thinkingStats() {
        if (thinkingTimes.isEmpty()) {
            return ThinkingStats.empty();
        }

        long bottomTotalThinkingTimeNanos = 0;
        long topTotalThinkingTimeNanos = 0;
        long bottomLastMoveNanos = 0;
        long topLastMoveNanos = 0;
        long bottomMaxNanos = 0;
        long topMaxNanos = 0;
        int bottomMoveCount = 0;
        int topMoveCount = 0;

        for (MoveThinkingTime thinkingTime : thinkingTimes) {
            if (isBottomPlayer(thinkingTime.move().getPlayer())) {
                bottomTotalThinkingTimeNanos += thinkingTime.thinkingTimeNanos();
                bottomLastMoveNanos = thinkingTime.thinkingTimeNanos();
                bottomMaxNanos = Math.max(bottomMaxNanos, thinkingTime.thinkingTimeNanos());
                bottomMoveCount++;
            } else {
                topTotalThinkingTimeNanos += thinkingTime.thinkingTimeNanos();
                topLastMoveNanos = thinkingTime.thinkingTimeNanos();
                topMaxNanos = Math.max(topMaxNanos, thinkingTime.thinkingTimeNanos());
                topMoveCount++;
            }
        }

        return new ThinkingStats(
                bottomLastMoveNanos,
                bottomTotalThinkingTimeNanos,
                bottomMoveCount,
                bottomMaxNanos,
                topLastMoveNanos,
                topTotalThinkingTimeNanos,
                topMoveCount,
                topMaxNanos);
    }

    private void rebuildGameStateFromHistory() {
        List<Move> movesToReplay = new ArrayList<>(moveHistory);
        Player bottomPlayer = bottomPlayer();
        Player topPlayer = topPlayer();
        board = new Board(9);
        bottomPlayer.reset(board.getBoardLength() - 1, board.getBoardLength() / 2, 10);
        topPlayer.reset(0, board.getBoardLength() / 2, 10);
        board.getOneCell(bottomPlayer.getRow(), bottomPlayer.getCol()).setPlayer(bottomPlayer);
        board.getOneCell(topPlayer.getRow(), topPlayer.getCol()).setPlayer(topPlayer);
        board.setFirstPlayer(bottomPlayer);
        board.setSecondPlayer(topPlayer);

        for (Move move : movesToReplay) {
            board.update(move);
            move.getPlayer().update(move);
        }
    }

    private boolean isCurrentTurn(Player player, int currentVersion) {
        return stateVersion == currentVersion && currentPlayer == player && !gameOver;
    }

    private Player otherPlayer(Player player) {
        return player == currentPlayer ? opponent : currentPlayer;
    }

    private Player bottomPlayer() {
        return isBottomPlayer(currentPlayer) ? currentPlayer : opponent;
    }

    private Player topPlayer() {
        return isBottomPlayer(currentPlayer) ? opponent : currentPlayer;
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }

    private boolean isGameOver(Player player) {
        return player.getRow() == player.getFinishRow();
    }

    private record MoveThinkingTime(Move move, long thinkingTimeNanos) {
    }

}
