package PerformanceModel;

import AI.Algorithm;
import AI.GymPython;
import AI.MiniMax;
import AI.MTCS.MtcsPerformance;
import AI.MTCS.MtcsV0;
import GUI.GameUI;
import SlowModel.PlayerProfile;
import SlowModel.PlayerType;
import javafx.application.Platform;

import java.util.Locale;

public class PerformanceGameController {

    private final Object gameLock = new Object();
    private final GameUI gui;
    private final int uiToken;

    private final GameState state;
    private Algorithm algCurrent;
    private Algorithm algOpponent;
    private Thread gameThread;
    private boolean isCurrentPlayerA;
    private boolean gameOver;
    private Boolean winnerIsPlayerA;

    public PerformanceGameController(GameUI gui, String playerAType, String playerBType) {
        this(gui, playerAType, playerBType, 0, true);
    }

    public PerformanceGameController(
            GameUI gui,
            String playerAType,
            String playerBType,
            int uiToken,
            boolean isPlayerAStarting) {
        this(
                gui,
                profileFromTypeName(playerAType),
                profileFromTypeName(playerBType),
                uiToken,
                isPlayerAStarting);
    }

    public PerformanceGameController(
            GameUI gui,
            PlayerProfile playerAProfile,
            PlayerProfile playerBProfile,
            int uiToken,
            boolean isPlayerAStarting) {
        this.gui = gui;
        this.uiToken = uiToken;
        Algorithm playerAAlgorithm = initAlg(playerAProfile);
        Algorithm playerBAlgorithm = initAlg(playerBProfile);
        this.state = new GameState(isPlayerAStarting);
        this.isCurrentPlayerA = isPlayerAStarting;
        this.algCurrent = isPlayerAStarting ? playerAAlgorithm : playerBAlgorithm;
        this.algOpponent = isPlayerAStarting ? playerBAlgorithm : playerAAlgorithm;
        this.gameOver = false;
        this.winnerIsPlayerA = null;

        play();
    }

    public void stop() {
        Thread threadToInterrupt;
        synchronized (gameLock) {
            gameOver = true;
            threadToInterrupt = gameThread;
        }

        if (threadToInterrupt != null) {
            threadToInterrupt.interrupt();
        }
    }

    public Boolean forfeitCurrentPlayer() {
        Thread threadToInterrupt;
        boolean forfeitureWinnerIsPlayerA;
        synchronized (gameLock) {
            if (winnerIsPlayerA != null) {
                return winnerIsPlayerA;
            }
            if (gameOver) {
                return null;
            }

            forfeitureWinnerIsPlayerA = !isCurrentPlayerA;
            winnerIsPlayerA = forfeitureWinnerIsPlayerA;
            gameOver = true;
            threadToInterrupt = gameThread;
        }

        if (threadToInterrupt != null) {
            threadToInterrupt.interrupt();
        }
        return forfeitureWinnerIsPlayerA;
    }

    private void play() {
        Thread thread = new Thread(this::runGameLoop);
        thread.setDaemon(true);

        synchronized (gameLock) {
            gameThread = thread;
        }
        thread.start();
    }

    private void runGameLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Algorithm algorithm;
                synchronized (gameLock) {
                    if (gameOver) {
                        break;
                    }
                    algorithm = algCurrent;
                }

                long thinkingStartedAt = System.nanoTime();
                int move = algorithm.generateMove(state);
                long thinkingTimeNanos = System.nanoTime() - thinkingStartedAt;

                MoveResult result;
                synchronized (gameLock) {
                    if (gameOver) {
                        break;
                    }

                    boolean playerAMoved = isCurrentPlayerA;
                    boolean includeInAverage = state.getCurrPlayerWalls() > 0;
                    int wallImpact = GameState.isPawnMoveCode(move) ? 0 : state.wallImpact(move);
                    int wallsAfterMove = wallsAfterMove(move);
                    boolean winningMove = isWinningMove(move);

                    if (winningMove) {
                        gameOver = true;
                        winnerIsPlayerA = playerAMoved;
                    } else {
                        state.update(move);
                        swapPlayers();
                    }

                    result = new MoveResult(
                            move,
                            playerAMoved,
                            wallsAfterMove,
                            thinkingTimeNanos,
                            includeInAverage,
                            wallImpact,
                            winningMove);
                }

                sendMoveToGui(result);
                if (result.winningMove()) {
                    sendWinnerToGui(result.playerAMoved());
                    break;
                }
                gui.pauseAfterFastMoveIfEnabled(uiToken, result.thinkingTimeNanos());
            }
        } finally {
            synchronized (gameLock) {
                if (gameThread == Thread.currentThread()) {
                    gameThread = null;
                }
            }
        }
    }

    private Algorithm initAlg(PlayerProfile playerProfile) {
        return switch (playerProfile.playerType()) {
            case MINIMAX -> new MiniMax(
                    playerProfile.minimaxDepth(),
                    playerProfile.minimaxMoveOrdering());
            case MTCS_EASY, MTCS_MEDIUM, MTCS_HARD, MTCS_EXTREME -> playerProfile.mtcsVariant() == PlayerProfile.MtcsVariant.PERFORMANCE
                    ? new MtcsPerformance(playerProfile.mtcsDepth())
                    : new MtcsV0(playerProfile.mtcsDepth());
            case GYM_PYTHON -> new GymPython();
            case HUMAN -> throw new IllegalArgumentException("Performance controller supports only AI players.");
        };
    }

    private static PlayerProfile profileFromTypeName(String playerType) {
        return new PlayerProfile(PlayerType.valueOf(normalizedPlayerType(playerType)), "");
    }

    private static String normalizedPlayerType(String playerType) {
        if (playerType == null) {
            throw new IllegalArgumentException("Player type cannot be null.");
        }
        return playerType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private int wallsAfterMove(int move) {
        if (GameState.isPawnMoveCode(move)) {
            return state.getCurrPlayerWalls();
        }
        return state.getCurrPlayerWalls() - 1;
    }

    private boolean isWinningMove(int move) {
        return GameState.isPawnMoveCode(move)
                && GameState.decodePawnMoveRow(move) == state.getCurrPlayerFinishLine();
    }

    private void swapPlayers() {
        Algorithm previousCurrentAlgorithm = algCurrent;
        algCurrent = algOpponent;
        algOpponent = previousCurrentAlgorithm;
        isCurrentPlayerA = !isCurrentPlayerA;
    }

    private void sendMoveToGui(MoveResult result) {
        Platform.runLater(() -> {
            gui.drawPerformanceMove(
                    uiToken,
                    result.move(),
                    result.playerAMoved(),
                    result.wallsAfterMove());
            gui.recordPerformanceMoveTime(
                    uiToken,
                    result.playerAMoved(),
                    result.includeInAverage(),
                    result.thinkingTimeNanos(),
                    result.wallImpact());
        });
    }

    private void sendWinnerToGui(boolean isPlayerAWinner) {
        Platform.runLater(() -> gui.endPerformanceGame(uiToken, isPlayerAWinner));
    }

    private record MoveResult(
            int move,
            boolean playerAMoved,
            int wallsAfterMove,
            long thinkingTimeNanos,
            boolean includeInAverage,
            int wallImpact,
            boolean winningMove) {
    }

}
