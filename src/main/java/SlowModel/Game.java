package SlowModel;

import AI.Algorithm;
import AI.GymPython;
import AI.MiniMax;
import AI.MTCS.MtcsPerformance;
import AI.MTCS.MtcsV0;
import GUI.GameUI;
import PerformanceModel.GameState;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {
    private final Object gameLock = new Object();
    private Board board;
    private final Player bottomPlayer;
    private final Player topPlayer;
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

    private final boolean isBottomPlayerStarting;
    private final Algorithm bottomPlayerAlgorithm;
    private final Algorithm topPlayerAlgorithm;
    private GameState aiState;

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType) {
        this(gui, new PlayerProfile(bottomPlayerType, ""), new PlayerProfile(topPlayerType, ""), 0, true);
    }

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType, int uiToken) {
        this(gui, new PlayerProfile(bottomPlayerType, ""), new PlayerProfile(topPlayerType, ""), uiToken, true);
    }

    public Game(GameUI gui, PlayerType bottomPlayerType, PlayerType topPlayerType, int uiToken, boolean isBottomPlayerStarting) {
        this(
                gui,
                new PlayerProfile(bottomPlayerType, ""),
                new PlayerProfile(topPlayerType, ""),
                uiToken,
                isBottomPlayerStarting);
    }

    public Game(GameUI gui, PlayerProfile bottomPlayerProfile, PlayerProfile topPlayerProfile, int uiToken, boolean isBottomPlayerStarting) {
        this.gui = gui;
        this.uiToken = uiToken;
        this.isBottomPlayerStarting = isBottomPlayerStarting;
        board = new Board(9);
        bottomPlayer = new Player(
                bottomPlayerProfile.playerType(),
                board.getBoardLength() - 1,
                board.getBoardLength() / 2,
                0);
        topPlayer = new Player(
                topPlayerProfile.playerType(),
                0,
                board.getBoardLength() / 2,
                board.getBoardLength() - 1);
        board.getOneCell(bottomPlayer.getRow(), bottomPlayer.getCol()).setPlayer(bottomPlayer);
        board.getOneCell(topPlayer.getRow(), topPlayer.getCol()).setPlayer(topPlayer);
        board.setFirstPlayer(bottomPlayer);
        board.setSecondPlayer(topPlayer);
        currentPlayer = isBottomPlayerStarting ? bottomPlayer : topPlayer;
        opponent = isBottomPlayerStarting ? topPlayer : bottomPlayer;
        aiState = new GameState(isBottomPlayerStarting);
        bottomPlayerAlgorithm = initAlgorithm(bottomPlayerProfile);
        topPlayerAlgorithm = initAlgorithm(topPlayerProfile);
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

                    TurnResult turnResult;
                    if (turnPlayer.isAI()) {
                        turnResult = playAiTurn(turnPlayer, currentVersion);
                    } else {
                        drawPossiblePawnMoves(turnPlayer, currentVersion);
                        turnResult = playHumanTurn(turnPlayer, currentVersion);
                        deletePossiblePawnMoves();
                    }

                    if (!turnResult.moveMade()) {
                        continue;
                    }

                    Player winner = finishTurn(turnPlayer, currentVersion);
                    if (winner != null) {
                        Platform.runLater(() -> gui.endGame(uiToken, winner));
                        break;
                    }

                    gui.pauseAfterFastMoveIfEnabled(uiToken, turnResult.thinkingTimeNanos());
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

    private TurnResult playAiTurn(Player player, int currentVersion) {
        GameState state;
        Algorithm algorithm;
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return new TurnResult(false, 0);
            }
            state = new GameState(aiState);
            algorithm = algorithmFor(player);
        }

        long thinkingStartedAt = System.nanoTime();
        int moveCode = algorithm.generateMove(state);
        long thinkingTimeNanos = System.nanoTime() - thinkingStartedAt;
        Move move = createMoveFromCode(moveCode, player);
        MoveApplicationResult moveResult = makeMoveIfCurrent(move, player, currentVersion);
        if (moveResult.moveMade()) {
            recordThinkingTime(move, thinkingTimeNanos, moveResult.includeInAverage(), moveResult.wallImpact());
            return new TurnResult(true, thinkingTimeNanos);
        }
        return new TurnResult(false, 0);
    }

    private TurnResult playHumanTurn(Player player, int currentVersion) {
        long thinkingStartedAt = System.nanoTime();
        do {
            int[] coordinates = getMouseClickCoordinates();
            if (!isCurrentTurn(player, currentVersion)) {
                return new TurnResult(false, 0);
            }
            if (Arrays.equals(coordinates, new int[]{0, 0})) {
                continue;
            }

            Move move = calculateMove(coordinates, player);
            MoveApplicationResult moveResult = makeLegalHumanMove(move, player, currentVersion);
            if (moveResult.moveMade()) {
                long thinkingTimeNanos = System.nanoTime() - thinkingStartedAt;
                recordThinkingTime(move, thinkingTimeNanos, moveResult.includeInAverage(), moveResult.wallImpact());
                return new TurnResult(true, thinkingTimeNanos);
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

    private MoveApplicationResult makeLegalHumanMove(Move move, Player player, int currentVersion) {
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion) || !board.isLegalMove(move)) {
                return new MoveApplicationResult(false, false, 0);
            }
        }

        return makeMoveIfCurrent(move, player, currentVersion);
    }

    private MoveApplicationResult makeMoveIfCurrent(Move move, Player player, int currentVersion) {
        boolean undoAvailable;
        boolean includeInAverage;
        int wallImpact;
        synchronized (gameLock) {
            if (!isCurrentTurn(player, currentVersion)) {
                return new MoveApplicationResult(false, false, 0);
            }
            int moveCode = encodeMoveCode(move);
            includeInAverage = player.wallsLeft() > 0;
            wallImpact = move.getType() == MoveType.WALL_PLACE ? aiState.wallImpact(moveCode) : 0;
            GameState nextAiState = new GameState(aiState);
            nextAiState.update(moveCode);
            board.update(move);
            player.update(move);
            aiState = nextAiState;
            moveHistory.add(move);
            undoAvailable = hasHumanMoveInHistory();
        }

        Platform.runLater(() -> {
            gui.draw(uiToken, move);
            gui.setUndoAvailable(uiToken, undoAvailable);
        });
        return new MoveApplicationResult(true, includeInAverage, wallImpact);
    }

    private Move createMoveFromCode(int moveCode, Player player) {
        if (GameState.isPawnMoveCode(moveCode)) {
            return new Move(
                    player,
                    MoveType.PAWN_MOVE,
                    GameState.decodePawnMoveRow(moveCode),
                    GameState.decodePawnMoveCol(moveCode));
        }

        return new Move(
                player,
                MoveType.WALL_PLACE,
                GameState.decodeWallRow(moveCode),
                GameState.decodeWallCol(moveCode),
                GameState.decodeWallIsHorizontal(moveCode));
    }

    private int encodeMoveCode(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            return GameState.PAWN_MOVE_CODE_OFFSET
                    + move.getTargetRow() * GameState.BOARD_LENGTH
                    + move.getTargetCol();
        }

        int wallGridLength = GameState.BOARD_LENGTH - 1;
        int wallAnchor = move.getTargetRow() * wallGridLength + move.getTargetCol();
        return wallAnchor * 2 + (move.isHorizontal() ? 0 : 1);
    }

    private Algorithm initAlgorithm(PlayerProfile playerProfile) {
        return switch (playerProfile.playerType()) {
            case MINIMAX -> new MiniMax(
                    playerProfile.minimaxDepth(),
                    playerProfile.minimaxMoveOrdering());
            case MTCS_EASY, MTCS_MEDIUM, MTCS_HARD, MTCS_EXTREME -> playerProfile.mtcsVariant() == PlayerProfile.MtcsVariant.PERFORMANCE
                    ? new MtcsPerformance(playerProfile.mtcsDepth())
                    : new MtcsV0(playerProfile.mtcsDepth());
            case GYM_PYTHON -> new GymPython();
            case HUMAN -> null;
        };
    }

    private Algorithm algorithmFor(Player player) {
        Algorithm algorithm = isBottomPlayer(player) ? bottomPlayerAlgorithm : topPlayerAlgorithm;
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot generate a move for a human player.");
        }
        return algorithm;
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
            List<Move> removedMoves = new ArrayList<>(moveHistory.subList(undoIndex, moveHistory.size()));
            moveHistory.subList(undoIndex, moveHistory.size()).clear();
            thinkingTimes.removeIf(thinkingTime -> removedMoves.contains(thinkingTime.move()));
            rebuildGameStateFromHistory();
            restoreTurnFromHistory();
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

    private void recordThinkingTime(Move move, long thinkingTimeNanos, boolean includeInAverage, int wallImpact) {
        ThinkingStats thinkingStats;
        synchronized (gameLock) {
            thinkingTimes.add(new MoveThinkingTime(move, thinkingTimeNanos, includeInAverage, wallImpact));
            thinkingStats = thinkingStats();
        }

        Platform.runLater(() -> gui.updateThinkingTime(uiToken, thinkingStats));
    }

    private ThinkingStats thinkingStats() {
        if (thinkingTimes.isEmpty()) {
            return ThinkingStats.empty();
        }

        long bottomMoveTimeTotalNanos = 0;
        long topMoveTimeTotalNanos = 0;
        long bottomTotalThinkingNanos = 0;
        long topTotalThinkingNanos = 0;
        long bottomLastMoveNanos = 0;
        long topLastMoveNanos = 0;
        long bottomMaxNanos = 0;
        long topMaxNanos = 0;
        int bottomWallImpactTotal = 0;
        int topWallImpactTotal = 0;
        int bottomMoveCount = 0;
        int topMoveCount = 0;

        for (MoveThinkingTime thinkingTime : thinkingTimes) {
            if (isBottomPlayer(thinkingTime.move().getPlayer())) {
                bottomLastMoveNanos = thinkingTime.thinkingTimeNanos();
                bottomMaxNanos = Math.max(bottomMaxNanos, thinkingTime.thinkingTimeNanos());
                bottomTotalThinkingNanos += thinkingTime.thinkingTimeNanos();
                bottomWallImpactTotal += thinkingTime.wallImpact();
                if (thinkingTime.includeInAverage()) {
                    bottomMoveTimeTotalNanos += thinkingTime.thinkingTimeNanos();
                    bottomMoveCount++;
                }
            } else {
                topLastMoveNanos = thinkingTime.thinkingTimeNanos();
                topMaxNanos = Math.max(topMaxNanos, thinkingTime.thinkingTimeNanos());
                topTotalThinkingNanos += thinkingTime.thinkingTimeNanos();
                topWallImpactTotal += thinkingTime.wallImpact();
                if (thinkingTime.includeInAverage()) {
                    topMoveTimeTotalNanos += thinkingTime.thinkingTimeNanos();
                    topMoveCount++;
                }
            }
        }

        return new ThinkingStats(
                bottomLastMoveNanos,
                bottomMoveTimeTotalNanos,
                bottomMoveCount,
                bottomMaxNanos,
                bottomTotalThinkingNanos,
                bottomWallImpactTotal,
                topLastMoveNanos,
                topMoveTimeTotalNanos,
                topMoveCount,
                topMaxNanos,
                topTotalThinkingNanos,
                topWallImpactTotal);
    }

    private void rebuildGameStateFromHistory() {
        List<Move> movesToReplay = new ArrayList<>(moveHistory);
        board = new Board(9);
        bottomPlayer.reset(board.getBoardLength() - 1, board.getBoardLength() / 2, 10);
        topPlayer.reset(0, board.getBoardLength() / 2, 10);
        board.getOneCell(bottomPlayer.getRow(), bottomPlayer.getCol()).setPlayer(bottomPlayer);
        board.getOneCell(topPlayer.getRow(), topPlayer.getCol()).setPlayer(topPlayer);
        board.setFirstPlayer(bottomPlayer);
        board.setSecondPlayer(topPlayer);
        aiState = new GameState(isBottomPlayerStarting);

        for (Move move : movesToReplay) {
            aiState.update(encodeMoveCode(move));
            board.update(move);
            move.getPlayer().update(move);
        }
    }

    private void restoreTurnFromHistory() {
        currentPlayer = moveHistory.isEmpty()
                ? startingPlayer()
                : otherPlayer(moveHistory.get(moveHistory.size() - 1).getPlayer());
        opponent = otherPlayer(currentPlayer);
    }

    private Player startingPlayer() {
        return isBottomPlayerStarting ? bottomPlayer : topPlayer;
    }

    private boolean isCurrentTurn(Player player, int currentVersion) {
        return stateVersion == currentVersion && currentPlayer == player && !gameOver;
    }

    private Player otherPlayer(Player player) {
        return player == bottomPlayer ? topPlayer : bottomPlayer;
    }

    private Player bottomPlayer() {
        return bottomPlayer;
    }

    private Player topPlayer() {
        return topPlayer;
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }

    private boolean isGameOver(Player player) {
        return player.getRow() == player.getFinishRow();
    }

    private record MoveThinkingTime(Move move, long thinkingTimeNanos, boolean includeInAverage, int wallImpact) {
    }

    private record MoveApplicationResult(boolean moveMade, boolean includeInAverage, int wallImpact) {
    }

    private record TurnResult(boolean moveMade, long thinkingTimeNanos) {
    }

}
