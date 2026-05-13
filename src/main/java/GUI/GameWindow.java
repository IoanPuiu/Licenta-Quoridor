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
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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

public class GameWindow {

    private static final int BOARD_SIZE = 9;
    private static final int INITIAL_WALLS = 10;
    private static final int CELL_SIZE = 50;
    private static final int CELL_WITH_STROKE = 56;
    private static final int BOARD_CONTENT_SIZE = BOARD_SIZE * CELL_WITH_STROKE;
    private static final int BOARD_FRAME_PADDING = 12;
    private static final int BOARD_FRAME_SIZE = BOARD_CONTENT_SIZE + BOARD_FRAME_PADDING * 2;
    private static final int SCENE_WIDTH = 900;
    private static final int SCENE_HEIGHT = 760;

    private final Stage stage;
    private final GridPane gridPane;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final Runnable undoHandler;
    private final Runnable newGameHandler;
    private final Runnable rematchHandler;
    private final Runnable exitHandler;
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
    private Label playerTurn;
    private Button undoButton;
    private Button newGameButton;
    private Button rematchButton;
    private Button exitButton;
    private MenuButton themeButton;
    private VBox controlsCard;
    private VBox aiThinkingCard;
    private HBox aiThinkingColumns;
    private VBox aiThinkingMetricLabels;
    private HBox scoreCard;
    private Label controlsTitle;
    private Label aiThinkingTitle;
    private VBox firstPlayerAiThinkingSection;
    private VBox secondPlayerAiThinkingSection;
    private Label firstPlayerAiThinkingNameLabel;
    private Label secondPlayerAiThinkingNameLabel;
    private Label aiLastThinkingLabel;
    private Label aiAverageThinkingLabel;
    private Label aiMaxThinkingLabel;
    private Label firstPlayerAiLastThinkingValueLabel;
    private Label firstPlayerAiAverageThinkingValueLabel;
    private Label firstPlayerAiMaxThinkingValueLabel;
    private Label secondPlayerAiLastThinkingValueLabel;
    private Label secondPlayerAiAverageThinkingValueLabel;
    private Label secondPlayerAiMaxThinkingValueLabel;
    private HBox scoreNumbers;
    private Label firstPlayerScoreNameLabel;
    private Label secondPlayerScoreNameLabel;
    private Label firstPlayerScoreValueLabel;
    private Label secondPlayerScoreValueLabel;
    private Label scoreDashLabel;
    private boolean isFirstPlayerTurn;
    private boolean isFirstPlayerWinner;
    private boolean isGameOver;

    public GameWindow(
            BiConsumer<Integer, Integer> boardClickHandler,
            Runnable undoHandler,
            Runnable newGameHandler,
            Runnable rematchHandler,
            Runnable exitHandler,
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
        Color winnerColor = isFirstPlayerWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        playerTurn.setText("Game over - " + playerDisplayName(winner) + " won");
        GuiTheme.styleTurnLabel(playerTurn, winnerColor);
        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, true);
    }

    public void showPerformanceGameResult(boolean isPlayerAWinner, int scoreFirstPlayerWins, int scoreSecondPlayerWins) {
        isGameOver = true;
        this.isFirstPlayerWinner = isPlayerAWinner;
        Color winnerColor = isPlayerAWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        playerTurn.setText("Game over - " + playerDisplayName(isPlayerAWinner) + " won");
        GuiTheme.styleTurnLabel(playerTurn, winnerColor);
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

        if (scoreCard != null) {
            scoreCard.setVisible(showScore);
            scoreCard.setManaged(showScore);
        }
    }

    public void updateThinkingTime(
            long firstPlayerLastMoveNanos,
            long firstPlayerAverageNanos,
            long firstPlayerMaxNanos,
            long secondPlayerLastMoveNanos,
            long secondPlayerAverageNanos,
            long secondPlayerMaxNanos) {
        if (firstPlayerAiLastThinkingValueLabel == null || secondPlayerAiLastThinkingValueLabel == null) {
            return;
        }

        firstPlayerAiLastThinkingValueLabel.setText(formatThinkingTime(firstPlayerLastMoveNanos));
        firstPlayerAiAverageThinkingValueLabel.setText(formatThinkingTime(firstPlayerAverageNanos));
        firstPlayerAiMaxThinkingValueLabel.setText(formatThinkingTime(firstPlayerMaxNanos));
        secondPlayerAiLastThinkingValueLabel.setText(formatThinkingTime(secondPlayerLastMoveNanos));
        secondPlayerAiAverageThinkingValueLabel.setText(formatThinkingTime(secondPlayerAverageNanos));
        secondPlayerAiMaxThinkingValueLabel.setText(formatThinkingTime(secondPlayerMaxNanos));
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

        gridPane.setMinSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setPrefSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setMaxSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);

        boardFrame = new StackPane(gridPane);
        boardFrame.setPadding(new Insets(BOARD_FRAME_PADDING));
        boardFrame.setMinSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setPrefSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setMaxSize(BOARD_FRAME_SIZE, BOARD_FRAME_SIZE);
        boardFrame.setStyle(GuiTheme.boardFrameStyle());

        VBox boardArea = new VBox(12, playerTurn, createScoreCard(), boardFrame);
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
        playerTurn = new Label(isFirstPlayerTurn ? firstPlayerDisplayName + "'s turn" : secondPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo());
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
                createControlsCard(),
                createAiThinkingCard());
        sidebar.setAlignment(Pos.TOP_LEFT);
        sidebar.setMinWidth(250);
        return sidebar;
    }

    private HBox createScoreCard() {
        firstPlayerScoreNameLabel = createScoreNameLabel(scoreFirstPlayerDisplayName, Pos.CENTER_RIGHT);
        secondPlayerScoreNameLabel = createScoreNameLabel(scoreSecondPlayerDisplayName, Pos.CENTER_LEFT);
        firstPlayerScoreValueLabel = createScoreValueLabel(scoreFirstPlayerColor());
        secondPlayerScoreValueLabel = createScoreValueLabel(scoreSecondPlayerColor());
        scoreDashLabel = new Label("-");
        scoreDashLabel.setTextFill(GuiTheme.mutedText());
        scoreDashLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 24));

        scoreNumbers = new HBox(6, firstPlayerScoreValueLabel, scoreDashLabel, secondPlayerScoreValueLabel);
        scoreNumbers.setAlignment(Pos.CENTER);
        scoreNumbers.setMinWidth(110);
        scoreNumbers.setStyle(GuiTheme.scoreNumbersStyle());

        scoreCard = new HBox(14, firstPlayerScoreNameLabel, scoreNumbers, secondPlayerScoreNameLabel);
        scoreCard.setAlignment(Pos.CENTER);
        scoreCard.setPadding(new Insets(8, 16, 8, 16));
        scoreCard.setMinWidth(360);
        scoreCard.setMaxWidth(Double.MAX_VALUE);
        scoreCard.setStyle(GuiTheme.scoreboardStyle());
        HBox.setHgrow(firstPlayerScoreNameLabel, Priority.ALWAYS);
        HBox.setHgrow(secondPlayerScoreNameLabel, Priority.ALWAYS);
        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, showScore);
        return scoreCard;
    }

    private Label createScoreNameLabel(String playerName, Pos alignment) {
        Label label = new Label(playerName);
        label.setWrapText(true);
        label.setTextFill(GuiTheme.text());
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 14));
        label.setAlignment(alignment);
        label.setMinWidth(110);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
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
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 30));
        label.setAlignment(Pos.CENTER);
        label.setMinWidth(30);
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

        controlsCard = new VBox(10, controlsTitle, undoButton, newGameButton, rematchButton, exitButton, themeButton);
        controlsCard.setPadding(new Insets(16));
        controlsCard.setMinWidth(180);
        controlsCard.setStyle(GuiTheme.panelStyle());
        return controlsCard;
    }

    private VBox createAiThinkingCard() {
        aiThinkingTitle = new Label("Move Time");
        aiThinkingTitle.setTextFill(GuiTheme.text());
        aiThinkingTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        firstPlayerAiThinkingSection = createAiPlayerThinkingSection(
                firstPlayerDisplayName,
                GuiTheme.playerOne(),
                true);
        secondPlayerAiThinkingSection = createAiPlayerThinkingSection(
                secondPlayerDisplayName,
                GuiTheme.playerTwo(),
                false);

        aiThinkingMetricLabels = createAiThinkingMetricLabels();

        aiThinkingColumns = new HBox(10, secondPlayerAiThinkingSection, aiThinkingMetricLabels, firstPlayerAiThinkingSection);
        aiThinkingColumns.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(secondPlayerAiThinkingSection, Priority.ALWAYS);
        HBox.setHgrow(firstPlayerAiThinkingSection, Priority.ALWAYS);

        aiThinkingCard = new VBox(10,
                aiThinkingTitle,
                aiThinkingColumns);
        aiThinkingCard.setPadding(new Insets(16));
        aiThinkingCard.setMinWidth(250);
        aiThinkingCard.setStyle(GuiTheme.panelStyle());
        updateThinkingTime(0, 0, 0, 0, 0, 0);
        return aiThinkingCard;
    }

    private VBox createAiPlayerThinkingSection(String playerName, Color accentColor, boolean isFirstPlayer) {
        Label playerLabel = new Label(playerName);
        playerLabel.setTextFill(accentColor);
        playerLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        playerLabel.setWrapText(true);

        Label lastValueLabel = createAiThinkingValueLabel();
        Label averageValueLabel = createAiThinkingValueLabel();
        Label maxValueLabel = createAiThinkingValueLabel();

        if (isFirstPlayer) {
            firstPlayerAiThinkingNameLabel = playerLabel;
            firstPlayerAiLastThinkingValueLabel = lastValueLabel;
            firstPlayerAiAverageThinkingValueLabel = averageValueLabel;
            firstPlayerAiMaxThinkingValueLabel = maxValueLabel;
        } else {
            secondPlayerAiThinkingNameLabel = playerLabel;
            secondPlayerAiLastThinkingValueLabel = lastValueLabel;
            secondPlayerAiAverageThinkingValueLabel = averageValueLabel;
            secondPlayerAiMaxThinkingValueLabel = maxValueLabel;
        }

        VBox section = new VBox(6, playerLabel, lastValueLabel, averageValueLabel, maxValueLabel);
        section.setAlignment(Pos.TOP_CENTER);
        section.setMinWidth(76);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private VBox createAiThinkingMetricLabels() {
        Label spacer = new Label("");
        spacer.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        aiLastThinkingLabel = createAiThinkingMetricLabel("Last");
        aiAverageThinkingLabel = createAiThinkingMetricLabel("Avg");
        aiMaxThinkingLabel = createAiThinkingMetricLabel("Max");

        VBox labels = new VBox(6, spacer, aiLastThinkingLabel, aiAverageThinkingLabel, aiMaxThinkingLabel);
        labels.setAlignment(Pos.TOP_CENTER);
        labels.setMinWidth(34);
        return labels;
    }

    private Label createAiThinkingMetricLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private Label createAiThinkingValueLabel() {
        Label label = new Label();
        label.setTextFill(GuiTheme.text());
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 14));
        label.setAlignment(Pos.CENTER);
        label.setMinWidth(62);
        return label;
    }

    private VBox createFirstPlayerCard() {
        firstPlayerMarker = new Circle(6, GuiTheme.playerOne());
        firstPlayerNameLabel = createPlayerNameLabel(firstPlayerDisplayName);
        firstPlayerCard = createPlayerCard(firstPlayerMarker, firstPlayerNameLabel, firstPlayerWallsLabel, GuiTheme.playerOne());
        return firstPlayerCard;
    }

    private VBox createSecondPlayerCard() {
        secondPlayerMarker = new Circle(6, GuiTheme.playerTwo());
        secondPlayerNameLabel = createPlayerNameLabel(secondPlayerDisplayName);
        secondPlayerCard = createPlayerCard(secondPlayerMarker, secondPlayerNameLabel, secondPlayerWallsLabel, GuiTheme.playerTwo());
        return secondPlayerCard;
    }

    private Label createPlayerNameLabel(String playerName) {
        Label nameLabel = new Label(playerName);
        nameLabel.setTextFill(GuiTheme.text());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return nameLabel;
    }

    private VBox createPlayerCard(Circle marker, Label nameLabel, Label wallsLabel, Color accentColor) {
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(2);

        HBox titleRow = new HBox(8, marker, nameLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, titleRow, wallsLabel);
        card.setPadding(new Insets(16));
        card.setMinWidth(180);
        card.setStyle(GuiTheme.playerCardStyle(accentColor));
        return card;
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

        Color turnColor = isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        if (isGameOver) {
            turnColor = isFirstPlayerWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        }
        GuiTheme.styleTurnLabel(playerTurn, turnColor);
        GuiTheme.styleUndoButton(undoButton);
        GuiTheme.styleCompactButton(newGameButton);
        GuiTheme.styleCompactButton(rematchButton);
        GuiTheme.styleDangerCompactButton(exitButton);
        GuiTheme.styleThemeButton(themeButton);

        firstPlayerWallsLabel.setTextFill(GuiTheme.playerOne());
        secondPlayerWallsLabel.setTextFill(GuiTheme.playerTwo());
        firstPlayerNameLabel.setTextFill(GuiTheme.text());
        secondPlayerNameLabel.setTextFill(GuiTheme.text());
        controlsTitle.setTextFill(GuiTheme.text());
        aiThinkingTitle.setTextFill(GuiTheme.text());
        firstPlayerAiThinkingNameLabel.setTextFill(GuiTheme.playerOne());
        secondPlayerAiThinkingNameLabel.setTextFill(GuiTheme.playerTwo());
        aiLastThinkingLabel.setTextFill(GuiTheme.mutedText());
        aiAverageThinkingLabel.setTextFill(GuiTheme.mutedText());
        aiMaxThinkingLabel.setTextFill(GuiTheme.mutedText());
        firstPlayerAiLastThinkingValueLabel.setTextFill(GuiTheme.text());
        firstPlayerAiAverageThinkingValueLabel.setTextFill(GuiTheme.text());
        firstPlayerAiMaxThinkingValueLabel.setTextFill(GuiTheme.text());
        secondPlayerAiLastThinkingValueLabel.setTextFill(GuiTheme.text());
        secondPlayerAiAverageThinkingValueLabel.setTextFill(GuiTheme.text());
        secondPlayerAiMaxThinkingValueLabel.setTextFill(GuiTheme.text());
        firstPlayerScoreNameLabel.setTextFill(GuiTheme.text());
        secondPlayerScoreNameLabel.setTextFill(GuiTheme.text());
        firstPlayerScoreValueLabel.setTextFill(scoreFirstPlayerColor());
        secondPlayerScoreValueLabel.setTextFill(scoreSecondPlayerColor());
        scoreDashLabel.setTextFill(GuiTheme.mutedText());
        scoreNumbers.setStyle(GuiTheme.scoreNumbersStyle());

        firstPlayerCard.setStyle(GuiTheme.playerCardStyle(GuiTheme.playerOne()));
        secondPlayerCard.setStyle(GuiTheme.playerCardStyle(GuiTheme.playerTwo()));
        controlsCard.setStyle(GuiTheme.panelStyle());
        aiThinkingCard.setStyle(GuiTheme.panelStyle());
        scoreCard.setStyle(GuiTheme.scoreboardStyle());
        firstPlayerMarker.setFill(GuiTheme.playerOne());
        secondPlayerMarker.setFill(GuiTheme.playerTwo());

        firstPlayerPawn.setFill(GuiTheme.playerOne());
        firstPlayerPawn.setEffect(GuiTheme.softShadow(GuiTheme.playerOne()));
        secondPlayerPawn.setFill(GuiTheme.playerTwo());
        secondPlayerPawn.setEffect(GuiTheme.softShadow(GuiTheme.playerTwo()));

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

        playerTurn.setText(isFirstPlayerTurn ? firstPlayerDisplayName + "'s turn" : secondPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo());
    }

    public void draw(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            drawPawn(move);
        } else {
            drawWall(move);
        }

        boolean firstPlayerMoved = isBottomPlayer(move.getPlayer());
        isFirstPlayerTurn = !firstPlayerMoved;
        playerTurn.setText(isFirstPlayerTurn ? firstPlayerDisplayName + "'s turn" : secondPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo());
    }

    public void drawPerformanceMove(int moveCode, boolean isPlayerAMove, int wallsLeft) {
        if (GameState.isPawnMoveCode(moveCode)) {
            drawPerformancePawn(moveCode, isPlayerAMove);
        } else {
            drawPerformanceWall(moveCode, isPlayerAMove, wallsLeft);
        }

        isFirstPlayerTurn = !isPlayerAMove;
        playerTurn.setText(isFirstPlayerTurn ? firstPlayerDisplayName + "'s turn" : secondPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo());
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

    private String playerDisplayName(Player player) {
        return isBottomPlayer(player) ? firstPlayerDisplayName : secondPlayerDisplayName;
    }

    private String playerDisplayName(boolean isPlayerA) {
        return isPlayerA ? firstPlayerDisplayName : secondPlayerDisplayName;
    }

    private String formatThinkingTime(long thinkingTimeNanos) {
        if (thinkingTimeNanos <= 0) {
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
}
