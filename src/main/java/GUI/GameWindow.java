package GUI;

import PerformanceModel.GameState;
import PerformanceModel.WallImpact;
import StandardModel.Move;
import StandardModel.MoveType;
import StandardModel.Player;
import StandardModel.PlayerProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GameWindow {

    private static final int SCENE_WIDTH = 1000;
    private static final int SCENE_HEIGHT = 720;

    private final Stage stage;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final Runnable undoHandler;
    private final Runnable newGameHandler;
    private final Runnable rematchHandler;
    private final Runnable exitHandler;
    private final Consumer<Boolean> fastMoveDelayHandler;
    private final PlayerProfile firstPlayerProfile;
    private final PlayerProfile secondPlayerProfile;
    private final boolean scoreFirstPlayerUsesFirstColor;
    private final boolean scoreSecondPlayerUsesFirstColor;
    private int scoreFirstPlayerWins;
    private int scoreSecondPlayerWins;
    private boolean showScore;
    private boolean isFirstPlayerTurn;
    private boolean isFirstPlayerWinner;
    private boolean isGameOver;

    private BorderPane root;
    private GameBoardPanel boardPanel;
    private ControlsPanel controlsPanel;
    private PlayersPanel playersPanel;
    private StatsPanel statsPanel;

    public GameWindow(
            BiConsumer<Integer, Integer> boardClickHandler,
            Runnable undoHandler,
            Runnable newGameHandler,
            Runnable rematchHandler,
            Runnable exitHandler,
            Consumer<Boolean> fastMoveDelayHandler,
            PlayerProfile firstPlayerProfile,
            PlayerProfile secondPlayerProfile,
            boolean scoreFirstPlayerUsesFirstColor,
            boolean scoreSecondPlayerUsesFirstColor,
            int scoreFirstPlayerWins,
            int scoreSecondPlayerWins,
            boolean showScore,
            boolean isFirstPlayerStarting) {
        this.stage = new Stage();
        this.boardClickHandler = boardClickHandler;
        this.undoHandler = undoHandler;
        this.newGameHandler = newGameHandler;
        this.rematchHandler = rematchHandler;
        this.exitHandler = exitHandler;
        this.fastMoveDelayHandler = fastMoveDelayHandler;
        this.firstPlayerProfile = firstPlayerProfile;
        this.secondPlayerProfile = secondPlayerProfile;
        this.scoreFirstPlayerUsesFirstColor = scoreFirstPlayerUsesFirstColor;
        this.scoreSecondPlayerUsesFirstColor = scoreSecondPlayerUsesFirstColor;
        this.scoreFirstPlayerWins = scoreFirstPlayerWins;
        this.scoreSecondPlayerWins = scoreSecondPlayerWins;
        this.showScore = showScore;
        this.isFirstPlayerTurn = isFirstPlayerStarting;
        this.isGameOver = false;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.setOnCloseRequest(event -> {
            event.consume();
            exitHandler.run();
        });
        stage.show();
    }

    public void close() {
        stage.setOnCloseRequest(null);
        stage.close();
    }

    public void setUndoAvailable(boolean undoAvailable) {
        controlsPanel.setUndoAvailable(undoAvailable);
    }

    public void showGameResult(Player winner, int scoreFirstPlayerWins, int scoreSecondPlayerWins) {
        isGameOver = true;
        isFirstPlayerWinner = isBottomPlayer(winner);
        updateTurnIndicators();
        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, true);
    }

    public void showPerformanceGameResult(boolean isPlayerAWinner, int scoreFirstPlayerWins, int scoreSecondPlayerWins) {
        isGameOver = true;
        this.isFirstPlayerWinner = isPlayerAWinner;
        updateTurnIndicators();
        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, true);
    }

    public void updateScore(int scoreFirstPlayerWins, int scoreSecondPlayerWins, boolean showScore) {
        this.scoreFirstPlayerWins = scoreFirstPlayerWins;
        this.scoreSecondPlayerWins = scoreSecondPlayerWins;
        this.showScore = showScore;

        if (statsPanel != null) {
            statsPanel.updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, showScore);
        }
    }

    public void updateThinkingTime(
            long firstPlayerAverageNanos,
            long firstPlayerMaxNanos,
            double firstPlayerAverageGameThinkingNanos,
            double firstPlayerAverageWallImpact,
            double firstPlayerFirstRoleWinRate,
            double firstPlayerSecondRoleWinRate,
            long secondPlayerAverageNanos,
            long secondPlayerMaxNanos,
            double secondPlayerAverageGameThinkingNanos,
            double secondPlayerAverageWallImpact,
            double secondPlayerFirstRoleWinRate,
            double secondPlayerSecondRoleWinRate) {
        if (statsPanel == null) {
            return;
        }

        statsPanel.updateThinkingTime(
                firstPlayerAverageNanos,
                firstPlayerMaxNanos,
                firstPlayerAverageGameThinkingNanos,
                firstPlayerAverageWallImpact,
                firstPlayerFirstRoleWinRate,
                firstPlayerSecondRoleWinRate,
                secondPlayerAverageNanos,
                secondPlayerMaxNanos,
                secondPlayerAverageGameThinkingNanos,
                secondPlayerAverageWallImpact,
                secondPlayerFirstRoleWinRate,
                secondPlayerSecondRoleWinRate);
    }

    public void redraw(List<Move> moves, int firstPlayerWalls, int secondPlayerWalls, boolean isFirstPlayerTurn) {
        isGameOver = false;
        this.isFirstPlayerTurn = isFirstPlayerTurn;
        boardPanel.redraw(moves);
        playersPanel.updateWalls(firstPlayerWalls, secondPlayerWalls);
        updateTurnIndicators();
    }

    public void draw(Move move) {
        boardPanel.draw(move);
        if (move.getType() == MoveType.WALL_PLACE) {
            playersPanel.updateWall(isBottomPlayer(move.getPlayer()), move.getPlayer().wallsLeft());
        }

        boolean firstPlayerMoved = isBottomPlayer(move.getPlayer());
        isFirstPlayerTurn = !firstPlayerMoved;
        updateTurnIndicators();
    }

    public void drawPerformanceMove(int moveCode, boolean isPlayerAMove, int wallsLeft, WallImpact wallImpact) {
        boardPanel.drawPerformanceMove(moveCode, isPlayerAMove, wallImpact);
        if (!GameState.isPawnMoveCode(moveCode)) {
            playersPanel.updateWall(isPlayerAMove, wallsLeft);
        }

        isFirstPlayerTurn = !isPlayerAMove;
        updateTurnIndicators();
    }

    public void drawPossiblePawnMoves(List<Move> moves) {
        boardPanel.drawPossiblePawnMoves(moves);
    }

    public void deletePossiblePawnMoves() {
        boardPanel.deletePossiblePawnMoves();
    }

    public void showIllegalMove() {
        boardPanel.showIllegalMove();
    }

    private Scene createScene() {
        boardPanel = new GameBoardPanel(boardClickHandler);
        controlsPanel = new ControlsPanel(
                undoHandler,
                newGameHandler,
                rematchHandler,
                exitHandler,
                fastMoveDelayHandler,
                this::applyTheme);
        playersPanel = new PlayersPanel(firstPlayerProfile, secondPlayerProfile, isFirstPlayerTurn);
        statsPanel = new StatsPanel(
                scoreFirstPlayerUsesFirstColor,
                scoreSecondPlayerUsesFirstColor,
                scoreFirstPlayerWins,
                scoreSecondPlayerWins,
                showScore);

        VBox leftColumn = new VBox(12, controlsPanel.view(), boardPanel.view());
        leftColumn.setAlignment(Pos.TOP_CENTER);
        leftColumn.setMinWidth(GameBoardPanel.FRAME_SIZE);
        leftColumn.setPrefWidth(GameBoardPanel.FRAME_SIZE);
        leftColumn.setMaxWidth(GameBoardPanel.FRAME_SIZE);

        VBox rightColumn = new VBox(12, playersPanel.view(), statsPanel.view());
        rightColumn.setAlignment(Pos.TOP_CENTER);
        rightColumn.setMinWidth(StatsPanel.WIDTH);
        rightColumn.setPrefWidth(StatsPanel.WIDTH);
        rightColumn.setMaxWidth(StatsPanel.WIDTH);

        HBox mainArea = new HBox(14, leftColumn, rightColumn);
        mainArea.setAlignment(Pos.CENTER);

        root = new BorderPane(mainArea);
        root.setPadding(new Insets(16));
        root.setStyle(GuiTheme.rootStyle());
        updateTurnIndicators();

        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private void applyTheme() {
        root.setStyle(GuiTheme.rootStyle());
        boardPanel.applyTheme(isFirstPlayerTurn);
        controlsPanel.applyTheme();
        playersPanel.applyTheme();
        statsPanel.applyTheme();
        updateTurnIndicators();
    }

    private void updateTurnIndicators() {
        if (playersPanel != null) {
            playersPanel.updateTurnState(isFirstPlayerTurn, isGameOver, isFirstPlayerWinner);
        }
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }
}
