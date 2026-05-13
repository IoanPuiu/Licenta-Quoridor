package GUI;

import PerformanceModel.GameState;
import javafx.animation.PauseTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import SlowModel.Move;
import SlowModel.MoveType;
import SlowModel.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GameWindow {

    private static final int BOARD_SIZE = 9;
    private static final int INITIAL_WALLS = 10;
    private static final int CELL_SIZE = 50;
    private static final int CELL_WITH_STROKE = 56;
    private static final int BOARD_CONTENT_SIZE = BOARD_SIZE * CELL_WITH_STROKE;
    private static final int BOARD_FRAME_PADDING = 12;
    private static final int BOARD_FRAME_SIZE = BOARD_CONTENT_SIZE + BOARD_FRAME_PADDING * 2;
    private static final int SCENE_WIDTH = 900;
    private static final int SCENE_HEIGHT = 820;
    private static final int STATS_METRIC_COLUMN_WIDTH = 138;
    private static final int STATS_PLAYER_COLUMN_WIDTH = 162;

    private final Stage stage;
    private final GridPane gridPane;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final Runnable undoHandler;
    private final Runnable newGameHandler;
    private final Runnable rematchHandler;
    private final Runnable exitHandler;
    private final Consumer<Boolean> fastMoveDelayHandler;
    private final List<Circle> possiblePawnMoves;
    private final List<Rectangle> boardCells;
    private final List<Line> wallLines;
    private final String firstPlayerDisplayName;
    private final String secondPlayerDisplayName;
    private final String scoreFirstPlayerDisplayName;
    private final String scoreSecondPlayerDisplayName;
    private final boolean scoreFirstPlayerUsesFirstColor;
    private final boolean scoreSecondPlayerUsesFirstColor;
    private int scoreFirstPlayerWins;
    private int scoreSecondPlayerWins;
    private boolean showScore;

    private BorderPane root;
    private StackPane boardFrame;
    private VBox firstPlayerCard;
    private VBox secondPlayerCard;
    private Label firstPlayerNameLabel;
    private Label secondPlayerNameLabel;
    private Circle firstPlayerMarker;
    private Circle secondPlayerMarker;
    private Circle firstPlayerPawn;
    private Circle secondPlayerPawn;
    private Label firstPlayerWallsLabel;
    private Label secondPlayerWallsLabel;
    private HBox firstPlayerTurnRow;
    private HBox secondPlayerTurnRow;
    private Polygon firstPlayerTurnSymbol;
    private Polygon secondPlayerTurnSymbol;
    private Label firstPlayerTurnLabel;
    private Label secondPlayerTurnLabel;
    private Button undoButton;
    private Button newGameButton;
    private Button rematchButton;
    private Button exitButton;
    private MenuButton themeButton;
    private ToggleButton fastMoveDelaySwitch;
    private Label fastModeLabel;
    private Label slowModeLabel;
    private StackPane fastMoveDelayTrack;
    private Circle fastMoveDelayThumb;
    private VBox controlsCard;
    private VBox matchStatsCard;
    private GridPane matchStatsGrid;
    private Label controlsTitle;
    private Label statsMetricHeaderLabel;
    private Label scoreMetricLabel;
    private Label averageMoveMetricLabel;
    private Label longestMoveMetricLabel;
    private Label averageGameThinkingMetricLabel;
    private Label averageWallImpactMetricLabel;
    private Label firstPlayerScoreNameLabel;
    private Label secondPlayerScoreNameLabel;
    private Label firstPlayerScoreValueLabel;
    private Label secondPlayerScoreValueLabel;
    private Label firstPlayerAverageMoveValueLabel;
    private Label secondPlayerAverageMoveValueLabel;
    private Label firstPlayerLongestMoveValueLabel;
    private Label secondPlayerLongestMoveValueLabel;
    private Label firstPlayerAverageGameThinkingValueLabel;
    private Label secondPlayerAverageGameThinkingValueLabel;
    private Label firstPlayerAverageWallImpactValueLabel;
    private Label secondPlayerAverageWallImpactValueLabel;
    private boolean isFirstPlayerTurn;
    private boolean isFirstPlayerWinner;
    private boolean isGameOver;

    public GameWindow(
            BiConsumer<Integer, Integer> boardClickHandler,
            Runnable undoHandler,
            Runnable newGameHandler,
            Runnable rematchHandler,
            Runnable exitHandler,
            Consumer<Boolean> fastMoveDelayHandler,
            String firstPlayerDisplayName,
            String secondPlayerDisplayName,
            String scoreFirstPlayerDisplayName,
            String scoreSecondPlayerDisplayName,
            boolean scoreFirstPlayerUsesFirstColor,
            boolean scoreSecondPlayerUsesFirstColor,
            int scoreFirstPlayerWins,
            int scoreSecondPlayerWins,
            boolean showScore,
            boolean isFirstPlayerStarting) {
        this.stage = new Stage();
        this.gridPane = new GridPane();
        this.boardClickHandler = boardClickHandler;
        this.undoHandler = undoHandler;
        this.newGameHandler = newGameHandler;
        this.rematchHandler = rematchHandler;
        this.exitHandler = exitHandler;
        this.fastMoveDelayHandler = fastMoveDelayHandler;
        this.firstPlayerDisplayName = firstPlayerDisplayName;
        this.secondPlayerDisplayName = secondPlayerDisplayName;
        this.scoreFirstPlayerDisplayName = scoreFirstPlayerDisplayName;
        this.scoreSecondPlayerDisplayName = scoreSecondPlayerDisplayName;
        this.scoreFirstPlayerUsesFirstColor = scoreFirstPlayerUsesFirstColor;
        this.scoreSecondPlayerUsesFirstColor = scoreSecondPlayerUsesFirstColor;
        this.scoreFirstPlayerWins = scoreFirstPlayerWins;
        this.scoreSecondPlayerWins = scoreSecondPlayerWins;
        this.showScore = showScore;
        this.possiblePawnMoves = new ArrayList<>();
        this.boardCells = new ArrayList<>();
        this.wallLines = new ArrayList<>();
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
        undoButton.setDisable(!undoAvailable);
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

        if (firstPlayerScoreValueLabel != null && secondPlayerScoreValueLabel != null) {
            firstPlayerScoreValueLabel.setText(String.valueOf(scoreFirstPlayerWins));
            secondPlayerScoreValueLabel.setText(String.valueOf(scoreSecondPlayerWins));
        }

        setStatsRowVisible(showScore, scoreMetricLabel, firstPlayerScoreValueLabel, secondPlayerScoreValueLabel);
    }

    public void updateThinkingTime(
            long firstPlayerAverageNanos,
            long firstPlayerMaxNanos,
            double firstPlayerAverageGameThinkingNanos,
            double firstPlayerAverageWallImpact,
            long secondPlayerAverageNanos,
            long secondPlayerMaxNanos,
            double secondPlayerAverageGameThinkingNanos,
            double secondPlayerAverageWallImpact) {
        if (firstPlayerAverageMoveValueLabel == null || secondPlayerAverageMoveValueLabel == null) {
            return;
        }

        firstPlayerAverageMoveValueLabel.setText(formatThinkingTime(firstPlayerAverageNanos));
        firstPlayerLongestMoveValueLabel.setText(formatThinkingTime(firstPlayerMaxNanos));
        firstPlayerAverageGameThinkingValueLabel.setText(formatThinkingTime(firstPlayerAverageGameThinkingNanos));
        firstPlayerAverageWallImpactValueLabel.setText(formatImpact(firstPlayerAverageWallImpact));
        secondPlayerAverageMoveValueLabel.setText(formatThinkingTime(secondPlayerAverageNanos));
        secondPlayerLongestMoveValueLabel.setText(formatThinkingTime(secondPlayerMaxNanos));
        secondPlayerAverageGameThinkingValueLabel.setText(formatThinkingTime(secondPlayerAverageGameThinkingNanos));
        secondPlayerAverageWallImpactValueLabel.setText(formatImpact(secondPlayerAverageWallImpact));
    }

    private Scene createScene() {
        createStatusLabels();
        createBoard();
        createPawns();

        themeButton = GuiTheme.createThemeMenu(this::applyTheme);
        undoButton = new Button("Undo");
        undoButton.setDisable(true);
        undoButton.setOnAction(event -> undoHandler.run());
        GuiTheme.styleUndoButton(undoButton);

        newGameButton = new Button("New Game");
        newGameButton.setOnAction(event -> newGameHandler.run());
        GuiTheme.styleCompactButton(newGameButton);

        rematchButton = new Button("Rematch");
        rematchButton.setOnAction(event -> rematchHandler.run());
        GuiTheme.styleCompactButton(rematchButton);

        exitButton = new Button("Exit");
        exitButton.setOnAction(event -> exitHandler.run());
        GuiTheme.styleDangerCompactButton(exitButton);

        fastMoveDelaySwitch = new ToggleButton();
        fastMoveDelaySwitch.setSelected(false);
        fastMoveDelaySwitch.setGraphic(createFastMoveDelaySwitchGraphic());
        fastMoveDelaySwitch.setAccessibleText("Fast");
        fastMoveDelaySwitch.setOnAction(event -> updateFastMoveDelaySwitch());
        GuiTheme.styleSpeedSwitch(
                fastMoveDelaySwitch,
                fastModeLabel,
                slowModeLabel,
                fastMoveDelayTrack,
                fastMoveDelayThumb);

        gridPane.setMinSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setPrefSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setMaxSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);

        boardFrame = new StackPane(gridPane);
        boardFrame.setPadding(new Insets(BOARD_FRAME_PADDING));
        boardFrame.setMinSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setPrefSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setMaxSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setStyle(GuiTheme.boardFrameStyle());

        VBox boardArea = new VBox(12, createMatchStatsCard(), boardFrame);
        boardArea.setAlignment(Pos.CENTER);

        VBox sidebar = createSidebar();

        root = new BorderPane();
        root.setCenter(boardArea);
        root.setRight(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 0, 0, 18));
        root.setPadding(new Insets(22));
        root.setStyle(GuiTheme.rootStyle());

        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private void createStatusLabels() {
        firstPlayerWallsLabel = createWallsLabel(GuiTheme.playerOne());
        secondPlayerWallsLabel = createWallsLabel(GuiTheme.playerTwo());
    }

    private Label createWallsLabel(Color color) {
        Label label = new Label(INITIAL_WALLS + " walls");
        label.setTextFill(color);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 24));
        return label;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(14,
                createSecondPlayerCard(),
                createFirstPlayerCard(),
                createControlsCard());
        sidebar.setAlignment(Pos.TOP_LEFT);
        sidebar.setMinWidth(250);
        return sidebar;
    }

    private VBox createMatchStatsCard() {
        matchStatsGrid = createMatchStatsGrid();
        matchStatsCard = new VBox(matchStatsGrid);
        matchStatsCard.setAlignment(Pos.CENTER);
        matchStatsCard.setPadding(new Insets(10, 16, 10, 16));
        matchStatsCard.setMinWidth(BOARD_FRAME_SIZE);
        matchStatsCard.setMaxWidth(BOARD_FRAME_SIZE);
        matchStatsCard.setStyle(GuiTheme.scoreboardStyle());
        return matchStatsCard;
    }

    private GridPane createMatchStatsGrid() {
        statsMetricHeaderLabel = createStatsMetricLabel("Statistic");
        statsMetricHeaderLabel.setAlignment(Pos.CENTER);
        firstPlayerScoreNameLabel = createStatsHeaderLabel(
                shortPlayerName(scoreFirstPlayerDisplayName, "Player"),
                scoreFirstPlayerColor());
        secondPlayerScoreNameLabel = createStatsHeaderLabel(
                shortPlayerName(scoreSecondPlayerDisplayName, "Player"),
                scoreSecondPlayerColor());
        scoreMetricLabel = createStatsMetricLabel("Score");
        averageMoveMetricLabel = createStatsMetricLabel("Avg move");
        longestMoveMetricLabel = createStatsMetricLabel("Longest move");
        averageGameThinkingMetricLabel = createStatsMetricLabel("Avg total/game");
        averageWallImpactMetricLabel = createStatsMetricLabel("Avg wall impact");

        firstPlayerScoreValueLabel = createScoreValueLabel(scoreFirstPlayerColor());
        secondPlayerScoreValueLabel = createScoreValueLabel(scoreSecondPlayerColor());
        firstPlayerAverageMoveValueLabel = createStatsValueLabel();
        secondPlayerAverageMoveValueLabel = createStatsValueLabel();
        firstPlayerLongestMoveValueLabel = createStatsValueLabel();
        secondPlayerLongestMoveValueLabel = createStatsValueLabel();
        firstPlayerAverageGameThinkingValueLabel = createStatsValueLabel();
        secondPlayerAverageGameThinkingValueLabel = createStatsValueLabel();
        firstPlayerAverageWallImpactValueLabel = createStatsValueLabel();
        secondPlayerAverageWallImpactValueLabel = createStatsValueLabel();

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER);
        grid.setMinWidth(BOARD_FRAME_SIZE - 48);
        grid.setMaxWidth(BOARD_FRAME_SIZE - 48);
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(STATS_METRIC_COLUMN_WIDTH),
                new ColumnConstraints(STATS_PLAYER_COLUMN_WIDTH),
                new ColumnConstraints(STATS_PLAYER_COLUMN_WIDTH));

        addStatsCell(grid, statsMetricHeaderLabel, 0, 0, STATS_METRIC_COLUMN_WIDTH);
        addStatsCell(grid, firstPlayerScoreNameLabel, 1, 0, STATS_PLAYER_COLUMN_WIDTH);
        addStatsCell(grid, secondPlayerScoreNameLabel, 2, 0, STATS_PLAYER_COLUMN_WIDTH);
        addStatsRow(grid, 1, scoreMetricLabel, firstPlayerScoreValueLabel, secondPlayerScoreValueLabel);
        addStatsRow(grid, 2, averageMoveMetricLabel, firstPlayerAverageMoveValueLabel, secondPlayerAverageMoveValueLabel);
        addStatsRow(grid, 3, longestMoveMetricLabel, firstPlayerLongestMoveValueLabel, secondPlayerLongestMoveValueLabel);
        addStatsRow(
                grid,
                4,
                averageGameThinkingMetricLabel,
                firstPlayerAverageGameThinkingValueLabel,
                secondPlayerAverageGameThinkingValueLabel);
        addStatsRow(
                grid,
                5,
                averageWallImpactMetricLabel,
                firstPlayerAverageWallImpactValueLabel,
                secondPlayerAverageWallImpactValueLabel);

        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, showScore);
        updateThinkingTime(0, 0, Double.NaN, Double.NaN, 0, 0, Double.NaN, Double.NaN);
        return grid;
    }

    private void addStatsRow(GridPane grid, int row, Label metricLabel, Label firstPlayerValue, Label secondPlayerValue) {
        addStatsCell(grid, metricLabel, 0, row, STATS_METRIC_COLUMN_WIDTH);
        addStatsCell(grid, firstPlayerValue, 1, row, STATS_PLAYER_COLUMN_WIDTH);
        addStatsCell(grid, secondPlayerValue, 2, row, STATS_PLAYER_COLUMN_WIDTH);
    }

    private Color scoreFirstPlayerColor() {
        return scoreFirstPlayerUsesFirstColor ? GuiTheme.playerOne() : GuiTheme.playerTwo();
    }

    private Color scoreSecondPlayerColor() {
        return scoreSecondPlayerUsesFirstColor ? GuiTheme.playerOne() : GuiTheme.playerTwo();
    }

    private Label createScoreValueLabel(Color accentColor) {
        Label label = new Label();
        label.setTextFill(accentColor);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 16));
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(26);
        label.setStyle(GuiTheme.statsValueStyle());
        return label;
    }

    private VBox createControlsCard() {
        controlsTitle = new Label("Controls");
        controlsTitle.setTextFill(GuiTheme.text());
        controlsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        undoButton.setMaxWidth(Double.MAX_VALUE);
        newGameButton.setMaxWidth(Double.MAX_VALUE);
        rematchButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        themeButton.setMaxWidth(Double.MAX_VALUE);
        fastMoveDelaySwitch.setMaxWidth(Double.MAX_VALUE);

        controlsCard = new VBox(
                10,
                controlsTitle,
                undoButton,
                newGameButton,
                rematchButton,
                exitButton,
                themeButton,
                fastMoveDelaySwitch);
        controlsCard.setPadding(new Insets(16));
        controlsCard.setMinWidth(180);
        controlsCard.setStyle(GuiTheme.panelStyle());
        return controlsCard;
    }

    private Label createStatsHeaderLabel(String text, Color accentColor) {
        Label label = new Label(text);
        label.setTextFill(accentColor);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        label.setMinWidth(STATS_PLAYER_COLUMN_WIDTH);
        label.setPrefWidth(STATS_PLAYER_COLUMN_WIDTH);
        label.setMaxWidth(STATS_PLAYER_COLUMN_WIDTH);
        label.setMinHeight(28);
        label.setStyle(GuiTheme.scoreTeamStyle(accentColor));
        return label;
    }

    private HBox createFastMoveDelaySwitchGraphic() {
        fastModeLabel = new Label("Fast");
        slowModeLabel = new Label("Slow");
        fastMoveDelayThumb = new Circle(8);
        fastMoveDelayTrack = new StackPane(fastMoveDelayThumb);

        HBox switchGraphic = new HBox(8, fastModeLabel, fastMoveDelayTrack, slowModeLabel);
        switchGraphic.setAlignment(Pos.CENTER);
        switchGraphic.setMouseTransparent(true);
        return switchGraphic;
    }

    private Label createStatsMetricLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMinHeight(26);
        label.setPadding(new Insets(0, 8, 0, 8));
        label.setStyle(GuiTheme.statsMetricStyle());
        return label;
    }

    private void addStatsCell(GridPane grid, Label label, int col, int row, int width) {
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        GridPane.setHalignment(label, HPos.CENTER);
        grid.add(label, col, row);
    }

    private void setStatsRowVisible(boolean isVisible, Label metricLabel, Label firstPlayerValue, Label secondPlayerValue) {
        if (metricLabel == null || firstPlayerValue == null || secondPlayerValue == null) {
            return;
        }

        metricLabel.setVisible(isVisible);
        metricLabel.setManaged(isVisible);
        firstPlayerValue.setVisible(isVisible);
        firstPlayerValue.setManaged(isVisible);
        secondPlayerValue.setVisible(isVisible);
        secondPlayerValue.setManaged(isVisible);
    }

    private String shortPlayerName(String playerName, String prefix) {
        String normalizedName = playerName == null || playerName.isBlank() ? prefix : playerName.trim();
        String compactName = normalizedName
                .replaceAll("MiniMax D(\\d+) - Fast Move Ordering", "MM$1F")
                .replaceAll("MiniMax D(\\d+) - Precise Move Ordering", "MM$1P")
                .replaceAll("MiniMax D(\\d+) - No Move Ordering", "MM$1")
                .replace("MiniMax", "MM")
                .replace("Minimax", "MM")
                .replace("MTCS Easy", "MCTS10K")
                .replace("MTCS Medium", "MCTS30K")
                .replace("MTCS Hard", "MCTS60K")
                .replace("MTCS depth 10,000", "MCTS10K")
                .replace("MTCS depth 30,000", "MCTS30K")
                .replace("MTCS depth 60,000", "MCTS60K")
                .replace("Gym Python", "Gym");
        if (compactName.length() > 18) {
            compactName = compactName.substring(0, 17).trim() + ".";
        }
        return compactName;
    }

    private Label createStatsValueLabel() {
        Label label = new Label();
        label.setTextFill(GuiTheme.text());
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(26);
        label.setStyle(GuiTheme.statsValueStyle());
        return label;
    }

    private VBox createFirstPlayerCard() {
        firstPlayerMarker = new Circle(6, GuiTheme.playerOne());
        firstPlayerNameLabel = createPlayerNameLabel(firstPlayerDisplayName);
        firstPlayerTurnRow = createTurnStatusRow(GuiTheme.playerOne(), true);
        firstPlayerCard = createPlayerCard(
                firstPlayerMarker,
                firstPlayerNameLabel,
                firstPlayerWallsLabel,
                firstPlayerTurnRow,
                GuiTheme.playerOne());
        return firstPlayerCard;
    }

    private VBox createSecondPlayerCard() {
        secondPlayerMarker = new Circle(6, GuiTheme.playerTwo());
        secondPlayerNameLabel = createPlayerNameLabel(secondPlayerDisplayName);
        secondPlayerTurnRow = createTurnStatusRow(GuiTheme.playerTwo(), false);
        secondPlayerCard = createPlayerCard(
                secondPlayerMarker,
                secondPlayerNameLabel,
                secondPlayerWallsLabel,
                secondPlayerTurnRow,
                GuiTheme.playerTwo());
        return secondPlayerCard;
    }

    private Label createPlayerNameLabel(String playerName) {
        Label nameLabel = new Label(playerName);
        nameLabel.setTextFill(GuiTheme.text());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return nameLabel;
    }

    private HBox createTurnStatusRow(Color accentColor, boolean isFirstPlayer) {
        Polygon turnSymbol = new Polygon(0, 0, 0, 12, 11, 6);
        turnSymbol.setFill(accentColor);
        turnSymbol.setStroke(Color.WHITE);
        turnSymbol.setStrokeWidth(1.2);

        Label statusLabel = new Label("Turn");
        statusLabel.setTextFill(accentColor);
        statusLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));

        HBox statusRow = new HBox(7, turnSymbol, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setMinHeight(18);

        if (isFirstPlayer) {
            firstPlayerTurnSymbol = turnSymbol;
            firstPlayerTurnLabel = statusLabel;
        } else {
            secondPlayerTurnSymbol = turnSymbol;
            secondPlayerTurnLabel = statusLabel;
        }

        return statusRow;
    }

    private VBox createPlayerCard(Circle marker, Label nameLabel, Label wallsLabel, HBox turnRow, Color accentColor) {
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(2);

        HBox titleRow = new HBox(8, marker, nameLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, titleRow, wallsLabel, turnRow);
        card.setPadding(new Insets(16));
        card.setMinWidth(180);
        card.setStyle(GuiTheme.playerCardStyle(accentColor));
        updateTurnIndicators();
        return card;
    }

    private void updateTurnIndicators() {
        if (firstPlayerTurnRow == null || secondPlayerTurnRow == null) {
            return;
        }

        if (isGameOver) {
            updateTurnIndicator(
                    firstPlayerTurnRow,
                    firstPlayerTurnSymbol,
                    firstPlayerTurnLabel,
                    isFirstPlayerWinner,
                    "Winner",
                    GuiTheme.playerOne());
            updateTurnIndicator(
                    secondPlayerTurnRow,
                    secondPlayerTurnSymbol,
                    secondPlayerTurnLabel,
                    !isFirstPlayerWinner,
                    "Winner",
                    GuiTheme.playerTwo());
        } else {
            updateTurnIndicator(
                    firstPlayerTurnRow,
                    firstPlayerTurnSymbol,
                    firstPlayerTurnLabel,
                    isFirstPlayerTurn,
                    "Turn",
                    GuiTheme.playerOne());
            updateTurnIndicator(
                    secondPlayerTurnRow,
                    secondPlayerTurnSymbol,
                    secondPlayerTurnLabel,
                    !isFirstPlayerTurn,
                    "Turn",
                    GuiTheme.playerTwo());
        }

        if (firstPlayerCard != null) {
            firstPlayerCard.setStyle(isFirstPlayerHighlighted()
                    ? GuiTheme.activePlayerCardStyle(GuiTheme.playerOne())
                    : GuiTheme.playerCardStyle(GuiTheme.playerOne()));
        }
        if (secondPlayerCard != null) {
            secondPlayerCard.setStyle(isSecondPlayerHighlighted()
                    ? GuiTheme.activePlayerCardStyle(GuiTheme.playerTwo())
                    : GuiTheme.playerCardStyle(GuiTheme.playerTwo()));
        }
    }

    private void updateTurnIndicator(
            HBox turnRow,
            Polygon turnSymbol,
            Label turnLabel,
            boolean isVisible,
            String text,
            Color accentColor) {
        turnRow.setManaged(isVisible);
        turnRow.setVisible(isVisible);
        turnLabel.setText(text);
        turnLabel.setTextFill(accentColor);
        turnSymbol.setFill(accentColor);
    }

    private boolean isFirstPlayerHighlighted() {
        return isGameOver ? isFirstPlayerWinner : isFirstPlayerTurn;
    }

    private boolean isSecondPlayerHighlighted() {
        return isGameOver ? !isFirstPlayerWinner : !isFirstPlayerTurn;
    }

    private void updateFastMoveDelaySwitch() {
        boolean slowMode = fastMoveDelaySwitch.isSelected();
        fastMoveDelaySwitch.setAccessibleText(slowMode ? "Slow" : "Fast");
        GuiTheme.styleSpeedSwitch(
                fastMoveDelaySwitch,
                fastModeLabel,
                slowModeLabel,
                fastMoveDelayTrack,
                fastMoveDelayThumb);
        fastMoveDelayHandler.accept(slowMode);
    }

    private void createBoard() {
        gridPane.setStyle(GuiTheme.boardStyle());
        boardCells.clear();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Color cellColor = (row + col) % 2 == 0 ? GuiTheme.boardCell() : GuiTheme.boardCellAlt();
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE, cellColor);
                cell.setStroke(GuiTheme.boardGrid());
                cell.setStrokeWidth(6);
                cell.setArcWidth(6);
                cell.setArcHeight(6);
                boardCells.add(cell);
                gridPane.add(cell, col, row);
            }
        }

        gridPane.setOnMouseClicked(event ->
                boardClickHandler.accept((int) event.getX(), (int) event.getY()));
    }

    private void createPawns() {
        firstPlayerPawn = createPawn(GuiTheme.playerOne());
        secondPlayerPawn = createPawn(GuiTheme.playerTwo());

        gridPane.add(firstPlayerPawn, 4, 8);
        gridPane.add(secondPlayerPawn, 4, 0);
    }

    private Circle createPawn(Color color) {
        Circle pawn = new Circle(12, color);
        pawn.setStroke(Color.WHITE);
        pawn.setStrokeWidth(3);
        pawn.setEffect(GuiTheme.softShadow(color));
        GridPane.setHalignment(pawn, HPos.CENTER);
        GridPane.setValignment(pawn, VPos.CENTER);
        return pawn;
    }

    private void applyTheme() {
        root.setStyle(GuiTheme.rootStyle());
        boardFrame.setStyle(GuiTheme.boardFrameStyle());
        gridPane.setStyle(GuiTheme.boardStyle());

        GuiTheme.styleUndoButton(undoButton);
        GuiTheme.styleCompactButton(newGameButton);
        GuiTheme.styleCompactButton(rematchButton);
        GuiTheme.styleDangerCompactButton(exitButton);
        GuiTheme.styleThemeButton(themeButton);
        GuiTheme.styleSpeedSwitch(
                fastMoveDelaySwitch,
                fastModeLabel,
                slowModeLabel,
                fastMoveDelayTrack,
                fastMoveDelayThumb);

        firstPlayerWallsLabel.setTextFill(GuiTheme.playerOne());
        secondPlayerWallsLabel.setTextFill(GuiTheme.playerTwo());
        firstPlayerNameLabel.setTextFill(GuiTheme.text());
        secondPlayerNameLabel.setTextFill(GuiTheme.text());
        controlsTitle.setTextFill(GuiTheme.text());
        List.of(
                statsMetricHeaderLabel,
                scoreMetricLabel,
                averageMoveMetricLabel,
                longestMoveMetricLabel,
                averageGameThinkingMetricLabel,
                averageWallImpactMetricLabel).forEach(label -> {
            label.setTextFill(GuiTheme.mutedText());
            label.setStyle(GuiTheme.statsMetricStyle());
        });
        List.of(
                firstPlayerAverageMoveValueLabel,
                secondPlayerAverageMoveValueLabel,
                firstPlayerLongestMoveValueLabel,
                secondPlayerLongestMoveValueLabel,
                firstPlayerAverageGameThinkingValueLabel,
                secondPlayerAverageGameThinkingValueLabel,
                firstPlayerAverageWallImpactValueLabel,
                secondPlayerAverageWallImpactValueLabel).forEach(label -> {
            label.setTextFill(GuiTheme.text());
            label.setStyle(GuiTheme.statsValueStyle());
        });
        firstPlayerScoreNameLabel.setTextFill(scoreFirstPlayerColor());
        secondPlayerScoreNameLabel.setTextFill(scoreSecondPlayerColor());
        firstPlayerScoreNameLabel.setStyle(GuiTheme.scoreTeamStyle(scoreFirstPlayerColor()));
        secondPlayerScoreNameLabel.setStyle(GuiTheme.scoreTeamStyle(scoreSecondPlayerColor()));
        firstPlayerScoreValueLabel.setTextFill(scoreFirstPlayerColor());
        secondPlayerScoreValueLabel.setTextFill(scoreSecondPlayerColor());
        firstPlayerScoreValueLabel.setStyle(GuiTheme.statsValueStyle());
        secondPlayerScoreValueLabel.setStyle(GuiTheme.statsValueStyle());

        controlsCard.setStyle(GuiTheme.panelStyle());
        matchStatsCard.setStyle(GuiTheme.scoreboardStyle());
        firstPlayerMarker.setFill(GuiTheme.playerOne());
        secondPlayerMarker.setFill(GuiTheme.playerTwo());
        firstPlayerTurnSymbol.setStroke(Color.WHITE);
        secondPlayerTurnSymbol.setStroke(Color.WHITE);

        firstPlayerPawn.setFill(GuiTheme.playerOne());
        firstPlayerPawn.setEffect(GuiTheme.softShadow(GuiTheme.playerOne()));
        secondPlayerPawn.setFill(GuiTheme.playerTwo());
        secondPlayerPawn.setEffect(GuiTheme.softShadow(GuiTheme.playerTwo()));

        updateTurnIndicators();

        for (int index = 0; index < boardCells.size(); index++) {
            Rectangle cell = boardCells.get(index);
            int row = index / BOARD_SIZE;
            int col = index % BOARD_SIZE;
            cell.setFill((row + col) % 2 == 0 ? GuiTheme.boardCell() : GuiTheme.boardCellAlt());
            cell.setStroke(GuiTheme.boardGrid());
        }

        for (Line wallLine : wallLines) {
            wallLine.setStroke(GuiTheme.wall());
        }

        Color possibleMoveColor = isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        for (Circle circle : possiblePawnMoves) {
            circle.setFill(possibleMoveColor.deriveColor(0, 0.9, 1.2, 0.55));
        }
    }

    public void redraw(List<Move> moves, int firstPlayerWalls, int secondPlayerWalls, boolean isFirstPlayerTurn) {
        deletePossiblePawnMoves();
        isGameOver = false;
        gridPane.getChildren().clear();
        boardCells.clear();
        wallLines.clear();
        this.isFirstPlayerTurn = isFirstPlayerTurn;

        createBoard();
        createPawns();
        firstPlayerWallsLabel.setText(firstPlayerWalls + " walls");
        secondPlayerWallsLabel.setText(secondPlayerWalls + " walls");

        for (Move move : moves) {
            if (move.getType() == MoveType.PAWN_MOVE) {
                drawPawn(move);
            } else {
                drawWall(move);
            }
        }

        updateTurnIndicators();
    }

    public void draw(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            drawPawn(move);
        } else {
            drawWall(move);
        }

        boolean firstPlayerMoved = isBottomPlayer(move.getPlayer());
        isFirstPlayerTurn = !firstPlayerMoved;
        updateTurnIndicators();
    }

    public void drawPerformanceMove(int moveCode, boolean isPlayerAMove, int wallsLeft) {
        if (GameState.isPawnMoveCode(moveCode)) {
            drawPerformancePawn(moveCode, isPlayerAMove);
        } else {
            drawPerformanceWall(moveCode, isPlayerAMove, wallsLeft);
        }

        isFirstPlayerTurn = !isPlayerAMove;
        updateTurnIndicators();
    }

    private void drawPawn(Move move) {
        Circle currentPlayerPawn = isBottomPlayer(move.getPlayer())
                ? firstPlayerPawn
                : secondPlayerPawn;

        gridPane.getChildren().remove(currentPlayerPawn);
        gridPane.add(currentPlayerPawn, move.getTargetCol(), move.getTargetRow());
    }

    private void drawPerformancePawn(int moveCode, boolean isPlayerAMove) {
        Circle currentPlayerPawn = isPlayerAMove ? firstPlayerPawn : secondPlayerPawn;

        gridPane.getChildren().remove(currentPlayerPawn);
        gridPane.add(
                currentPlayerPawn,
                GameState.decodePawnMoveCol(moveCode),
                GameState.decodePawnMoveRow(moveCode));
    }

    private void drawWall(Move move) {
        int playerWalls = move.getPlayer().wallsLeft();

        if (isBottomPlayer(move.getPlayer())) {
            firstPlayerWallsLabel.setText(playerWalls + " walls");
        } else {
            secondPlayerWallsLabel.setText(playerWalls + " walls");
        }

        int x1 = move.isHorizontal()
                ? move.getTargetCol() * CELL_WITH_STROKE + 15
                : move.getTargetCol() * CELL_WITH_STROKE + 56;
        int y1 = move.isHorizontal()
                ? move.getTargetRow() * CELL_WITH_STROKE + 56
                : move.getTargetRow() * CELL_WITH_STROKE + 15;
        int x2 = move.isHorizontal() ? x1 + 80 : x1;
        int y2 = move.isHorizontal() ? y1 : y1 + 80;

        drawLine(x1, x2, y1, y2);
    }

    private void drawPerformanceWall(int moveCode, boolean isPlayerAMove, int wallsLeft) {
        if (isPlayerAMove) {
            firstPlayerWallsLabel.setText(wallsLeft + " walls");
        } else {
            secondPlayerWallsLabel.setText(wallsLeft + " walls");
        }

        int row = GameState.decodeWallRow(moveCode);
        int col = GameState.decodeWallCol(moveCode);
        boolean isHorizontal = GameState.decodeWallIsHorizontal(moveCode);
        int x1 = isHorizontal ? col * CELL_WITH_STROKE + 15 : col * CELL_WITH_STROKE + 56;
        int y1 = isHorizontal ? row * CELL_WITH_STROKE + 56 : row * CELL_WITH_STROKE + 15;
        int x2 = isHorizontal ? x1 + 80 : x1;
        int y2 = isHorizontal ? y1 : y1 + 80;

        drawLine(x1, x2, y1, y2);
    }

    private void drawLine(int x1, int x2, int y1, int y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(GuiTheme.wall());
        line.setStrokeWidth(8);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        wallLines.add(line);

        Pane linePane = new Pane(line);
        linePane.setMouseTransparent(true);
        gridPane.add(linePane, 0, 0, BOARD_SIZE, BOARD_SIZE);
    }

    public void drawPossiblePawnMoves(List<Move> moves) {
        for (Move move : moves) {
            Color moveColor = isBottomPlayer(move.getPlayer())
                    ? GuiTheme.playerOne()
                    : GuiTheme.playerTwo();
            Circle smallCircle = new Circle(6, moveColor.deriveColor(0, 0.9, 1.2, 0.55));
            smallCircle.setStroke(Color.WHITE);
            smallCircle.setStrokeWidth(2);
            smallCircle.setMouseTransparent(true);
            GridPane.setHalignment(smallCircle, HPos.CENTER);
            GridPane.setValignment(smallCircle, VPos.CENTER);
            possiblePawnMoves.add(smallCircle);
            gridPane.add(smallCircle, move.getTargetCol(), move.getTargetRow());
        }
    }

    public void deletePossiblePawnMoves() {
        for (Circle circle : possiblePawnMoves) {
            gridPane.getChildren().remove(circle);
        }
        possiblePawnMoves.clear();
    }

    public void showIllegalMove() {
        Label illegalMoveLabel = new Label("Illegal move");
        illegalMoveLabel.setTextFill(Color.WHITE);
        illegalMoveLabel.setBackground(new Background(new BackgroundFill(GuiTheme.danger(), new CornerRadii(8), Insets.EMPTY)));
        illegalMoveLabel.setAlignment(Pos.CENTER);
        illegalMoveLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 30));
        illegalMoveLabel.setPadding(new Insets(16, 24, 16, 24));

        GridPane.setHalignment(illegalMoveLabel, HPos.CENTER);
        GridPane.setValignment(illegalMoveLabel, VPos.CENTER);
        GridPane.setConstraints(illegalMoveLabel, 0, 0, BOARD_SIZE, BOARD_SIZE);
        GridPane.setFillWidth(illegalMoveLabel, true);
        GridPane.setFillHeight(illegalMoveLabel, true);
        gridPane.getChildren().add(illegalMoveLabel);

        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(event -> gridPane.getChildren().remove(illegalMoveLabel));
        pause.play();
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }

    private String formatThinkingTime(double thinkingTimeNanos) {
        if (Double.isNaN(thinkingTimeNanos) || thinkingTimeNanos <= 0) {
            return "--";
        }

        double milliseconds = thinkingTimeNanos / 1_000_000.0;
        if (milliseconds < 10) {
            return String.format(Locale.US, "%.1f ms", milliseconds);
        }
        if (milliseconds < 1000) {
            return String.format(Locale.US, "%.0f ms", milliseconds);
        }
        return String.format(Locale.US, "%.2f s", milliseconds / 1000.0);
    }

    private String formatImpact(double impact) {
        if (Double.isNaN(impact)) {
            return "--";
        }
        if (Math.abs(impact) < 0.05) {
            return "0.0";
        }
        return String.format(Locale.US, "%+.1f", impact);
    }
}
