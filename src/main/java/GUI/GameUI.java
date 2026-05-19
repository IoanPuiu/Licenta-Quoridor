package GUI;

import PerformanceModel.PerformanceGameController;
import PerformanceModel.WallImpact;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameUI extends Application {

    private static final long FAST_MOVE_THRESHOLD_NANOS = 1_000_000_000L;
    private static final long FAST_MOVE_PAUSE_MILLIS = 1_000L;

    private final BlockingQueue<int[]> clickQueue = new LinkedBlockingQueue<>();

    private GameWindow gameWindow;
    private Game currentGame;
    private PerformanceGameController currentPerformanceGame;
    private PlayerProfile firstPlayerProfile;
    private PlayerProfile secondPlayerProfile;
    private PlayerProfile scoreFirstPlayerProfile;
    private PlayerProfile scoreSecondPlayerProfile;
    private int scoreFirstPlayerWins;
    private int scoreSecondPlayerWins;
    private int scoreFirstPlayerFirstRoleGames;
    private int scoreFirstPlayerFirstRoleWins;
    private int scoreFirstPlayerSecondRoleGames;
    private int scoreFirstPlayerSecondRoleWins;
    private int scoreSecondPlayerFirstRoleGames;
    private int scoreSecondPlayerFirstRoleWins;
    private int scoreSecondPlayerSecondRoleGames;
    private int scoreSecondPlayerSecondRoleWins;
    private int completedMatches;
    private int scoredGameToken = -1;
    private PlayerProfile scoredWinnerProfile;
    private boolean scoredGameFirstPlayerStarted;
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
        scoredGameFirstPlayerStarted = false;
        fastMoveDelayEnabled = false;

        gameWindow = new GameWindow(
                this::storeClickCoordinates,
                this::undoLastHumanMove,
                this::newGame,
                this::rematchGame,
                this::exitGame,
                this::setFastMoveDelayEnabled,
                firstPlayerProfile,
                secondPlayerProfile,
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
        clickQueue.clear();
    }

    private void storeClickCoordinates(int x, int y) {
        clickQueue.offer(new int[]{x, y});
    }

    private void undoLastHumanMove() {
        if (currentGame != null) {
            currentGame.undoToLastHumanMove();
        }
    }

    public void wakeWaitingInput() {
        clickQueue.clear();
        storeClickCoordinates(0, 0);
    }

    public void wakeWaitingInput(int gameToken) {
        if (isActiveGame(gameToken)) {
            wakeWaitingInput();
        }
    }

    public int[] getMouseClickCoordinates() {
        try {
            return clickQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new int[]{0, 0};
        }
    }

    public void draw(Move move) {
        gameWindow.draw(move);
    }

    public void draw(int gameToken, Move move) {
        if (isActiveGame(gameToken)) {
            draw(move);
        }
    }

    public void drawPerformanceMove(
            int gameToken,
            int moveCode,
            boolean isPlayerAMove,
            int wallsLeft,
            WallImpact wallImpact) {
        if (isActiveGame(gameToken)) {
            gameWindow.drawPerformanceMove(moveCode, isPlayerAMove, wallsLeft, wallImpact);
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
        scoreFirstPlayerWins = 0;
        scoreSecondPlayerWins = 0;
        resetWinRateStats();
        completedMatches = 0;
        scoredGameToken = -1;
        scoredWinnerProfile = null;
        scoredGameFirstPlayerStarted = false;
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
        scoreFirstPlayerWins = 0;
        scoreSecondPlayerWins = 0;
        resetWinRateStats();
        completedMatches = 0;
        scoredGameToken = -1;
        scoredWinnerProfile = null;
        scoredGameFirstPlayerStarted = false;
        resetThinkingStats();
    }

    private void resetWinRateStats() {
        scoreFirstPlayerFirstRoleGames = 0;
        scoreFirstPlayerFirstRoleWins = 0;
        scoreFirstPlayerSecondRoleGames = 0;
        scoreFirstPlayerSecondRoleWins = 0;
        scoreSecondPlayerFirstRoleGames = 0;
        scoreSecondPlayerFirstRoleWins = 0;
        scoreSecondPlayerSecondRoleGames = 0;
        scoreSecondPlayerSecondRoleWins = 0;
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
                winRate(scoreFirstPlayerFirstRoleWins, scoreFirstPlayerFirstRoleGames),
                winRate(scoreFirstPlayerSecondRoleWins, scoreFirstPlayerSecondRoleGames),
                displayStats.topAverageNanos(),
                displayStats.topMaxNanos(),
                averageNanosPerCompletedMatch(completedStats.topTotalThinkingNanos()),
                averageImpactPerCompletedMatch(completedStats.topWallImpactTotal()),
                winRate(scoreSecondPlayerFirstRoleWins, scoreSecondPlayerFirstRoleGames),
                winRate(scoreSecondPlayerSecondRoleWins, scoreSecondPlayerSecondRoleGames));
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

    private double winRate(int wins, int games) {
        return games == 0 ? Double.NaN : wins * 100.0 / games;
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
            addWin(winnerProfile, isFirstPlayerStarting);
            scoredGameToken = gameToken;
            scoredWinnerProfile = winnerProfile;
            scoredGameFirstPlayerStarted = isFirstPlayerStarting;
        }

        gameWindow.showGameResult(winner, scoreFirstPlayerWins, scoreSecondPlayerWins);
        updateThinkingTimeDisplay();
        scheduleAutomaticRematch(gameToken);
    }

    private void recordPerformanceGameResult(int gameToken, boolean isPlayerAWinner) {
        PlayerProfile winnerProfile = isPlayerAWinner ? firstPlayerProfile : secondPlayerProfile;
        if (scoredGameToken != gameToken) {
            addWin(winnerProfile, isFirstPlayerStarting);
            scoredGameToken = gameToken;
            scoredWinnerProfile = winnerProfile;
            scoredGameFirstPlayerStarted = isFirstPlayerStarting;
        }

        gameWindow.showPerformanceGameResult(isPlayerAWinner, scoreFirstPlayerWins, scoreSecondPlayerWins);
        updateThinkingTimeDisplay();
        scheduleAutomaticRematch(gameToken);
    }

    private PlayerProfile winnerProfile(Player winner) {
        return isBottomPlayer(winner) ? firstPlayerProfile : secondPlayerProfile;
    }

    private void addWin(PlayerProfile winnerProfile, boolean scoreFirstPlayerStarted) {
        if (winnerProfile == scoreFirstPlayerProfile) {
            scoreFirstPlayerWins++;
        } else if (winnerProfile == scoreSecondPlayerProfile) {
            scoreSecondPlayerWins++;
        }

        addWinRateResult(true, scoreFirstPlayerStarted, winnerProfile == scoreFirstPlayerProfile);
        addWinRateResult(false, !scoreFirstPlayerStarted, winnerProfile == scoreSecondPlayerProfile);
        completedMatches++;
    }

    private void addWinRateResult(boolean isScoreFirstPlayer, boolean playedFirstRole, boolean won) {
        if (isScoreFirstPlayer) {
            if (playedFirstRole) {
                scoreFirstPlayerFirstRoleGames++;
                scoreFirstPlayerFirstRoleWins += won ? 1 : 0;
            } else {
                scoreFirstPlayerSecondRoleGames++;
                scoreFirstPlayerSecondRoleWins += won ? 1 : 0;
            }
            return;
        }

        if (playedFirstRole) {
            scoreSecondPlayerFirstRoleGames++;
            scoreSecondPlayerFirstRoleWins += won ? 1 : 0;
        } else {
            scoreSecondPlayerSecondRoleGames++;
            scoreSecondPlayerSecondRoleWins += won ? 1 : 0;
        }
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

        removeWinRateResult(
                true,
                scoredGameFirstPlayerStarted,
                scoredWinnerProfile == scoreFirstPlayerProfile);
        removeWinRateResult(
                false,
                !scoredGameFirstPlayerStarted,
                scoredWinnerProfile == scoreSecondPlayerProfile);
        if (completedMatches > 0) {
            completedMatches--;
        }

        scoredGameToken = -1;
        scoredWinnerProfile = null;
        scoredGameFirstPlayerStarted = false;
    }

    private void removeWinRateResult(boolean isScoreFirstPlayer, boolean playedFirstRole, boolean won) {
        if (isScoreFirstPlayer) {
            if (playedFirstRole) {
                scoreFirstPlayerFirstRoleGames = Math.max(0, scoreFirstPlayerFirstRoleGames - 1);
                if (won) {
                    scoreFirstPlayerFirstRoleWins = Math.max(0, scoreFirstPlayerFirstRoleWins - 1);
                }
            } else {
                scoreFirstPlayerSecondRoleGames = Math.max(0, scoreFirstPlayerSecondRoleGames - 1);
                if (won) {
                    scoreFirstPlayerSecondRoleWins = Math.max(0, scoreFirstPlayerSecondRoleWins - 1);
                }
            }
            return;
        }

        if (playedFirstRole) {
            scoreSecondPlayerFirstRoleGames = Math.max(0, scoreSecondPlayerFirstRoleGames - 1);
            if (won) {
                scoreSecondPlayerFirstRoleWins = Math.max(0, scoreSecondPlayerFirstRoleWins - 1);
            }
        } else {
            scoreSecondPlayerSecondRoleGames = Math.max(0, scoreSecondPlayerSecondRoleGames - 1);
            if (won) {
                scoreSecondPlayerSecondRoleWins = Math.max(0, scoreSecondPlayerSecondRoleWins - 1);
            }
        }
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
