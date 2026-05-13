package GUI;

import PerformanceModel.PerformanceGameController;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import SlowModel.Game;
import SlowModel.Move;
import SlowModel.Player;
import SlowModel.PlayerProfile;
import SlowModel.ThinkingStats;

import java.util.List;
import java.util.concurrent.Semaphore;

public class GameUI extends Application {

    private static final long FAST_MOVE_THRESHOLD_NANOS = 1_000_000_000L;
    private static final long FAST_MOVE_PAUSE_MILLIS = 1_000L;

    private final Semaphore clickSemaphore = new Semaphore(0);

    private volatile int boardX;
    private volatile int boardY;
    private GameWindow gameWindow;
    private Game currentGame;
    private PerformanceGameController currentPerformanceGame;
    private PlayerProfile firstPlayerProfile;
    private PlayerProfile secondPlayerProfile;
    private PlayerProfile scoreFirstPlayerProfile;
    private PlayerProfile scoreSecondPlayerProfile;
    private String scoreFirstPlayerDisplayName;
    private String scoreSecondPlayerDisplayName;
    private int scoreFirstPlayerWins;
    private int scoreSecondPlayerWins;
    private int completedMatches;
    private int scoredGameToken = -1;
    private PlayerProfile scoredWinnerProfile;
    private ThinkingStats accumulatedThinkingStats = ThinkingStats.empty();
    private ThinkingStats currentGameThinkingStats = ThinkingStats.empty();
    private PauseTransition automaticRematchDelay;
    private int remainingAutomaticRematches;
    private int activeGameToken;
    private boolean isFirstPlayerStarting;
    private volatile boolean fastMoveDelayEnabled;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        showStartWindow(primaryStage);
    }

    @Override
    public void stop() {
        cancelAutomaticRematchDelay();
        stopCurrentGame();
    }

    private void showStartWindow(Stage stage) {
        StartWindow startWindow = new StartWindow(stage, this::startGame);
        startWindow.show();
    }

    private void startGame(PlayerProfile selectedFirstPlayerProfile, PlayerProfile selectedSecondPlayerProfile, int automaticRematches) {
        cancelAutomaticRematchDelay();
        remainingAutomaticRematches = isAiVsAi(selectedFirstPlayerProfile, selectedSecondPlayerProfile)
                ? Math.max(0, automaticRematches)
                : 0;

        PlayerProfile firstPlayerProfile = bottomPlayerProfile(selectedFirstPlayerProfile, selectedSecondPlayerProfile);
        PlayerProfile secondPlayerProfile = firstPlayerProfile == selectedFirstPlayerProfile
                ? selectedSecondPlayerProfile
                : selectedFirstPlayerProfile;
        startGame(firstPlayerProfile, secondPlayerProfile, firstPlayerProfile == selectedFirstPlayerProfile);
    }

    private void startGame(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile, boolean isFirstPlayerStarting) {
        resetInputState();
        if (scoreFirstPlayerProfile == null || scoreSecondPlayerProfile == null) {
            initializeScore(firstPlayerProfile, secondPlayerProfile);
        }

        this.firstPlayerProfile = firstPlayerProfile;
        this.secondPlayerProfile = secondPlayerProfile;
        this.isFirstPlayerStarting = isFirstPlayerStarting;
        int gameToken = ++activeGameToken;
        scoredGameToken = -1;
        scoredWinnerProfile = null;
        fastMoveDelayEnabled = false;

        gameWindow = new GameWindow(
                this::storeClickCoordinates,
                this::undoLastHumanMove,
                this::newGame,
                this::rematchGame,
                this::exitGame,
                this::setFastMoveDelayEnabled,
                firstPlayerProfile.displayName("First Player"),
                secondPlayerProfile.displayName("Second Player"),
                scoreFirstPlayerDisplayName,
                scoreSecondPlayerDisplayName,
                scoreFirstPlayerProfile == firstPlayerProfile,
                scoreSecondPlayerProfile == firstPlayerProfile,
                scoreFirstPlayerWins,
                scoreSecondPlayerWins,
                true,
                isFirstPlayerStarting);
        gameWindow.show();
        updateThinkingTimeDisplay();

        startController(firstPlayerProfile, secondPlayerProfile, gameToken, isFirstPlayerStarting);
    }

    private void startController(
            PlayerProfile firstPlayerProfile,
            PlayerProfile secondPlayerProfile,
            int gameToken,
            boolean isFirstPlayerStarting) {
        currentGame = null;
        currentPerformanceGame = null;

        if (isAiVsAi(firstPlayerProfile, secondPlayerProfile)) {
            currentPerformanceGame = new PerformanceGameController(
                    this,
                    firstPlayerProfile,
                    secondPlayerProfile,
                    gameToken,
                    isFirstPlayerStarting);
        } else {
            currentGame = new Game(
                    this,
                    firstPlayerProfile,
                    secondPlayerProfile,
                    gameToken,
                    isFirstPlayerStarting);
        }
    }

    private void resetInputState() {
        boardX = 0;
        boardY = 0;
        clickSemaphore.drainPermits();
    }

    private void storeClickCoordinates(int x, int y) {
        boardX = x;
        boardY = y;
        clickSemaphore.release();
    }

    private void undoLastHumanMove() {
        if (currentGame != null) {
            currentGame.undoToLastHumanMove();
        }
    }

    public void wakeWaitingInput() {
        storeClickCoordinates(0, 0);
    }

    public void wakeWaitingInput(int gameToken) {
        if (isActiveGame(gameToken)) {
            wakeWaitingInput();
        }
    }

    public int[] getMouseClickCoordinates() {
        try {
            clickSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new int[]{boardX, boardY};
    }

    public void draw(Move move) {
        gameWindow.draw(move);
    }

    public void draw(int gameToken, Move move) {
        if (isActiveGame(gameToken)) {
            draw(move);
        }
    }

    public void drawPerformanceMove(int gameToken, int moveCode, boolean isPlayerAMove, int wallsLeft) {
        if (isActiveGame(gameToken)) {
            gameWindow.drawPerformanceMove(moveCode, isPlayerAMove, wallsLeft);
        }
    }

    public void drawPossiblePawnMoves(List<Move> moves) {
        gameWindow.drawPossiblePawnMoves(moves);
    }

    public void drawPossiblePawnMoves(int gameToken, List<Move> moves) {
        if (isActiveGame(gameToken)) {
            drawPossiblePawnMoves(moves);
        }
    }

    public void deletePossiblePawnMoves() {
        gameWindow.deletePossiblePawnMoves();
    }

    public void deletePossiblePawnMoves(int gameToken) {
        if (isActiveGame(gameToken)) {
            deletePossiblePawnMoves();
        }
    }

    public void redrawGame(List<Move> moves, int firstPlayerWalls, int secondPlayerWalls, boolean isFirstPlayerTurn) {
        gameWindow.redraw(moves, firstPlayerWalls, secondPlayerWalls, isFirstPlayerTurn);
    }

    public void redrawGame(int gameToken, List<Move> moves, int firstPlayerWalls, int secondPlayerWalls, boolean isFirstPlayerTurn) {
        if (isActiveGame(gameToken)) {
            removeScoredResult(gameToken);
            redrawGame(moves, firstPlayerWalls, secondPlayerWalls, isFirstPlayerTurn);
            gameWindow.updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, true);
        }
    }

    public void setUndoAvailable(boolean undoAvailable) {
        gameWindow.setUndoAvailable(undoAvailable);
    }

    public void setUndoAvailable(int gameToken, boolean undoAvailable) {
        if (isActiveGame(gameToken)) {
            setUndoAvailable(undoAvailable);
        }
    }

    public void updateThinkingTime(ThinkingStats thinkingStats) {
        currentGameThinkingStats = thinkingStats;
        updateThinkingTimeDisplay();
    }

    public void updateThinkingTime(int gameToken, ThinkingStats thinkingStats) {
        if (isActiveGame(gameToken)) {
            updateThinkingTime(thinkingStats);
        }
    }

    public void recordPerformanceMoveTime(
            int gameToken,
            boolean isPlayerAMove,
            boolean includeInAverage,
            long thinkingTimeNanos,
            int wallImpact) {
        if (!isActiveGame(gameToken)) {
            return;
        }

        currentGameThinkingStats = appendMoveStats(
                currentGameThinkingStats,
                isPlayerAMove,
                includeInAverage,
                thinkingTimeNanos,
                wallImpact);
        updateThinkingTimeDisplay();
    }

    public void pauseAfterFastMoveIfEnabled(int gameToken, long thinkingTimeNanos) {
        if (!fastMoveDelayEnabled
                || thinkingTimeNanos >= FAST_MOVE_THRESHOLD_NANOS
                || !isActiveGame(gameToken)) {
            return;
        }

        long pauseUntilNanos = System.nanoTime() + FAST_MOVE_PAUSE_MILLIS * 1_000_000L;
        while (fastMoveDelayEnabled && isActiveGame(gameToken)) {
            long remainingNanos = pauseUntilNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return;
            }

            try {
                Thread.sleep(Math.min(50, Math.max(1, remainingNanos / 1_000_000L)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void endGame(Player player) {
        recordGameResult(activeGameToken, player);
    }

    public void endGame(int gameToken, Player player) {
        if (isActiveGame(gameToken)) {
            recordGameResult(gameToken, player);
        }
    }

    public void endPerformanceGame(int gameToken, boolean isPlayerAWinner) {
        if (isActiveGame(gameToken)) {
            recordPerformanceGameResult(gameToken, isPlayerAWinner);
        }
    }

    private void newGame() {
        remainingAutomaticRematches = 0;
        cancelAutomaticRematchDelay();
        stopCurrentGame();
        closeGameWindow();
        resetScore();
        showStartWindow(new Stage());
    }

    private void rematchGame() {
        remainingAutomaticRematches = 0;
        cancelAutomaticRematchDelay();
        if (firstPlayerProfile == null || secondPlayerProfile == null) {
            newGame();
            return;
        }

        if (currentGame != null) {
            Player winner = currentGame.forfeitCurrentPlayer();
            if (winner != null) {
                recordGameResult(activeGameToken, winner);
            }
        } else if (currentPerformanceGame != null) {
            Boolean isPlayerAWinner = currentPerformanceGame.forfeitCurrentPlayer();
            if (isPlayerAWinner != null) {
                recordPerformanceGameResult(activeGameToken, isPlayerAWinner);
            }
        }

        boolean nextIsFirstPlayerStarting = !isFirstPlayerStarting;
        startRematchInCurrentWindow(nextIsFirstPlayerStarting);
    }

    private void exitGame() {
        remainingAutomaticRematches = 0;
        cancelAutomaticRematchDelay();
        stopCurrentGame();
        closeGameWindow();
        Platform.exit();
    }

    private void stopCurrentGame() {
        if (currentGame != null) {
            currentGame.stop();
            currentGame = null;
        }
        if (currentPerformanceGame != null) {
            currentPerformanceGame.stop();
            currentPerformanceGame = null;
        }
        activeGameToken++;
        resetInputState();
    }

    private void closeGameWindow() {
        if (gameWindow != null) {
            gameWindow.close();
            gameWindow = null;
        }
    }

    public void illegalMove() {
        gameWindow.showIllegalMove();
    }

    private void setFastMoveDelayEnabled(boolean fastMoveDelayEnabled) {
        this.fastMoveDelayEnabled = fastMoveDelayEnabled;
    }

    private void initializeScore(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile) {
        scoreFirstPlayerProfile = firstPlayerProfile;
        scoreSecondPlayerProfile = secondPlayerProfile;
        scoreFirstPlayerDisplayName = firstPlayerProfile.displayName("First Player");
        scoreSecondPlayerDisplayName = secondPlayerProfile.displayName("Second Player");
        scoreFirstPlayerWins = 0;
        scoreSecondPlayerWins = 0;
        completedMatches = 0;
        scoredGameToken = -1;
        scoredWinnerProfile = null;
        resetThinkingStats();
    }

    private PlayerProfile bottomPlayerProfile(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile) {
        if (firstPlayerProfile.playerType().isAI() && !secondPlayerProfile.playerType().isAI()) {
            return secondPlayerProfile;
        }

        return firstPlayerProfile;
    }

    private void resetScore() {
        scoreFirstPlayerProfile = null;
        scoreSecondPlayerProfile = null;
        scoreFirstPlayerDisplayName = null;
        scoreSecondPlayerDisplayName = null;
        scoreFirstPlayerWins = 0;
        scoreSecondPlayerWins = 0;
        completedMatches = 0;
        scoredGameToken = -1;
        scoredWinnerProfile = null;
        resetThinkingStats();
    }

    private void resetThinkingStats() {
        accumulatedThinkingStats = ThinkingStats.empty();
        currentGameThinkingStats = ThinkingStats.empty();
    }

    private void commitCurrentThinkingStats() {
        accumulatedThinkingStats = accumulatedThinkingStats.plus(currentGameThinkingStats, false);
        currentGameThinkingStats = ThinkingStats.empty();
    }

    private void updateThinkingTimeDisplay() {
        if (gameWindow == null) {
            return;
        }

        ThinkingStats displayStats = accumulatedThinkingStats.plus(currentGameThinkingStats, true);
        ThinkingStats completedStats = completedThinkingStats();
        gameWindow.updateThinkingTime(
                displayStats.bottomAverageNanos(),
                displayStats.bottomMaxNanos(),
                averageNanosPerCompletedMatch(completedStats.bottomTotalThinkingNanos()),
                averageImpactPerCompletedMatch(completedStats.bottomWallImpactTotal()),
                displayStats.topAverageNanos(),
                displayStats.topMaxNanos(),
                averageNanosPerCompletedMatch(completedStats.topTotalThinkingNanos()),
                averageImpactPerCompletedMatch(completedStats.topWallImpactTotal()));
    }

    private ThinkingStats completedThinkingStats() {
        if (scoredGameToken == activeGameToken) {
            return accumulatedThinkingStats.plus(currentGameThinkingStats, false);
        }

        return accumulatedThinkingStats;
    }

    private double averageNanosPerCompletedMatch(long totalNanos) {
        return completedMatches == 0 ? Double.NaN : totalNanos / (double) completedMatches;
    }

    private double averageImpactPerCompletedMatch(int totalImpact) {
        return completedMatches == 0 ? Double.NaN : totalImpact / (double) completedMatches;
    }

    private ThinkingStats appendMoveStats(
            ThinkingStats stats,
            boolean isPlayerAMove,
            boolean includeInAverage,
            long thinkingTimeNanos,
            int wallImpact) {
        if (isPlayerAMove) {
            return stats.appendBottomMove(includeInAverage, thinkingTimeNanos, wallImpact);
        }

        return stats.appendTopMove(includeInAverage, thinkingTimeNanos, wallImpact);
    }

    private void recordGameResult(int gameToken, Player winner) {
        PlayerProfile winnerProfile = winnerProfile(winner);
        if (scoredGameToken != gameToken) {
            addWin(winnerProfile);
            scoredGameToken = gameToken;
            scoredWinnerProfile = winnerProfile;
        }

        gameWindow.showGameResult(winner, scoreFirstPlayerWins, scoreSecondPlayerWins);
        updateThinkingTimeDisplay();
        scheduleAutomaticRematch(gameToken);
    }

    private void recordPerformanceGameResult(int gameToken, boolean isPlayerAWinner) {
        PlayerProfile winnerProfile = isPlayerAWinner ? firstPlayerProfile : secondPlayerProfile;
        if (scoredGameToken != gameToken) {
            addWin(winnerProfile);
            scoredGameToken = gameToken;
            scoredWinnerProfile = winnerProfile;
        }

        gameWindow.showPerformanceGameResult(isPlayerAWinner, scoreFirstPlayerWins, scoreSecondPlayerWins);
        updateThinkingTimeDisplay();
        scheduleAutomaticRematch(gameToken);
    }

    private PlayerProfile winnerProfile(Player winner) {
        return isBottomPlayer(winner) ? firstPlayerProfile : secondPlayerProfile;
    }

    private void addWin(PlayerProfile winnerProfile) {
        if (winnerProfile == scoreFirstPlayerProfile) {
            scoreFirstPlayerWins++;
        } else if (winnerProfile == scoreSecondPlayerProfile) {
            scoreSecondPlayerWins++;
        }
        completedMatches++;
    }

    private void removeScoredResult(int gameToken) {
        if (scoredGameToken != gameToken || scoredWinnerProfile == null) {
            return;
        }

        if (scoredWinnerProfile == scoreFirstPlayerProfile && scoreFirstPlayerWins > 0) {
            scoreFirstPlayerWins--;
        } else if (scoredWinnerProfile == scoreSecondPlayerProfile && scoreSecondPlayerWins > 0) {
            scoreSecondPlayerWins--;
        }
        if (completedMatches > 0) {
            completedMatches--;
        }

        scoredGameToken = -1;
        scoredWinnerProfile = null;
    }

    private boolean isActiveGame(int gameToken) {
        return gameToken == activeGameToken && gameWindow != null;
    }

    private void scheduleAutomaticRematch(int gameToken) {
        if (remainingAutomaticRematches <= 0 || !isActiveGame(gameToken) || !isAiVsAi(firstPlayerProfile, secondPlayerProfile)) {
            return;
        }

        remainingAutomaticRematches--;
        cancelAutomaticRematchDelay();
        automaticRematchDelay = new PauseTransition(Duration.seconds(1));
        automaticRematchDelay.setOnFinished(event -> {
            automaticRematchDelay = null;
            if (isActiveGame(gameToken)) {
                startAutomaticRematch();
            }
        });
        automaticRematchDelay.play();
    }

    private void startAutomaticRematch() {
        if (firstPlayerProfile == null || secondPlayerProfile == null) {
            return;
        }

        boolean nextIsFirstPlayerStarting = !isFirstPlayerStarting;
        startRematchInCurrentWindow(nextIsFirstPlayerStarting);
    }

    private void startRematchInCurrentWindow(boolean nextIsFirstPlayerStarting) {
        if (firstPlayerProfile == null || secondPlayerProfile == null) {
            return;
        }

        commitCurrentThinkingStats();
        stopCurrentGame();
        this.isFirstPlayerStarting = nextIsFirstPlayerStarting;
        int gameToken = ++activeGameToken;
        scoredGameToken = -1;
        scoredWinnerProfile = null;

        if (gameWindow == null) {
            startGame(firstPlayerProfile, secondPlayerProfile, nextIsFirstPlayerStarting);
            return;
        }

        gameWindow.redraw(List.of(), 10, 10, nextIsFirstPlayerStarting);
        gameWindow.updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, true);
        gameWindow.setUndoAvailable(false);
        updateThinkingTimeDisplay();

        startController(firstPlayerProfile, secondPlayerProfile, gameToken, nextIsFirstPlayerStarting);
    }

    private void cancelAutomaticRematchDelay() {
        if (automaticRematchDelay != null) {
            automaticRematchDelay.stop();
            automaticRematchDelay = null;
        }
    }

    private boolean isAiVsAi(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile) {
        return firstPlayerProfile != null
                && secondPlayerProfile != null
                && firstPlayerProfile.playerType().isAI()
                && secondPlayerProfile.playerType().isAI();
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }
}
