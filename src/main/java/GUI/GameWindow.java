package GUI;

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
import model.Move;
import model.MoveType;
import model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GameWindow {

    private static final int BOARD_SIZE = 9;
    private static final int INITIAL_WALLS = 10;
    private static final int CELL_SIZE = 50;
    private static final int CELL_WITH_STROKE = 56;
    private static final int SCENE_WIDTH = 820;
    private static final int SCENE_HEIGHT = 650;

    private final Stage stage;
    private final GridPane gridPane;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final Runnable undoHandler;
    private final Runnable newGameHandler;
    private final List<Circle> possiblePawnMoves;
    private final List<Rectangle> boardCells;
    private final List<Line> wallLines;
    private final String firstPlayerDisplayName;
    private final String secondPlayerDisplayName;

    private BorderPane root;
    private StackPane boardFrame;
    private Label title;
    private Label playersTitle;
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
    private MenuButton themeButton;
    private VBox controlsCard;
    private VBox resultCard;
    private Label controlsTitle;
    private Label resultTitle;
    private Label winnerLabel;
    private boolean isFirstPlayerTurn;
    private boolean isFirstPlayerWinner;

    public GameWindow(
            BiConsumer<Integer, Integer> boardClickHandler,
            Runnable undoHandler,
            Runnable newGameHandler,
            String firstPlayerDisplayName,
            String secondPlayerDisplayName) {
        this.stage = new Stage();
        this.gridPane = new GridPane();
        this.boardClickHandler = boardClickHandler;
        this.undoHandler = undoHandler;
        this.newGameHandler = newGameHandler;
        this.firstPlayerDisplayName = firstPlayerDisplayName;
        this.secondPlayerDisplayName = secondPlayerDisplayName;
        this.possiblePawnMoves = new ArrayList<>();
        this.boardCells = new ArrayList<>();
        this.wallLines = new ArrayList<>();
        this.isFirstPlayerTurn = true;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public void setUndoAvailable(boolean undoAvailable) {
        undoButton.setDisable(!undoAvailable);
    }

    public void showGameResult(Player winner) {
        isFirstPlayerWinner = winner.getColor() == Color.CYAN;
        Color winnerColor = isFirstPlayerWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        winnerLabel.setText(winner + " won");
        winnerLabel.setTextFill(winnerColor);
        playerTurn.setText("Game over");
        GuiTheme.styleTurnLabel(playerTurn, winnerColor);
        resultCard.setVisible(true);
        resultCard.setManaged(true);
    }

    private Scene createScene() {
        createStatusLabels();
        createBoard();
        createPawns();

        title = new Label("Quoridor");
        GuiTheme.styleWindowTitle(title, 28);

        themeButton = GuiTheme.createThemeMenu(this::applyTheme);
        undoButton = new Button("Undo");
        undoButton.setDisable(true);
        undoButton.setOnAction(event -> undoHandler.run());
        GuiTheme.styleCompactButton(undoButton);

        HBox header = new HBox(18, title, playerTurn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        boardFrame = new StackPane(gridPane);
        boardFrame.setPadding(new Insets(12));
        boardFrame.setStyle(GuiTheme.boardFrameStyle());

        VBox sidebar = createSidebar();

        root = new BorderPane();
        root.setTop(header);
        root.setCenter(boardFrame);
        root.setRight(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 0, 0, 18));
        root.setPadding(new Insets(22));
        root.setStyle(GuiTheme.rootStyle());

        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private void createStatusLabels() {
        firstPlayerWallsLabel = createWallsLabel(GuiTheme.playerOne());
        secondPlayerWallsLabel = createWallsLabel(GuiTheme.playerTwo());
        playerTurn = new Label(firstPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, GuiTheme.playerOne());
    }

    private Label createWallsLabel(Color color) {
        Label label = new Label(INITIAL_WALLS + " walls");
        label.setTextFill(color);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 24));
        return label;
    }

    private VBox createSidebar() {
        playersTitle = new Label("Players");
        GuiTheme.styleWindowTitle(playersTitle, 20);

        VBox sidebar = new VBox(14,
                playersTitle,
                createSecondPlayerCard(),
                createFirstPlayerCard(),
                createControlsCard(),
                createResultCard());
        sidebar.setAlignment(Pos.TOP_LEFT);
        sidebar.setMinWidth(180);
        return sidebar;
    }

    private VBox createResultCard() {
        resultTitle = new Label("Result");
        resultTitle.setTextFill(GuiTheme.text());
        resultTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        winnerLabel = new Label();
        winnerLabel.setWrapText(true);
        winnerLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 18));

        newGameButton = new Button("New Game");
        newGameButton.setMaxWidth(Double.MAX_VALUE);
        newGameButton.setOnAction(event -> newGameHandler.run());
        GuiTheme.styleCompactButton(newGameButton);

        resultCard = new VBox(10, resultTitle, winnerLabel, newGameButton);
        resultCard.setPadding(new Insets(16));
        resultCard.setMinWidth(180);
        resultCard.setStyle(GuiTheme.panelStyle());
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        return resultCard;
    }

    private VBox createControlsCard() {
        controlsTitle = new Label("Controls");
        controlsTitle.setTextFill(GuiTheme.text());
        controlsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        undoButton.setMaxWidth(Double.MAX_VALUE);
        themeButton.setMaxWidth(Double.MAX_VALUE);

        controlsCard = new VBox(10, controlsTitle, undoButton, themeButton);
        controlsCard.setPadding(new Insets(16));
        controlsCard.setMinWidth(180);
        controlsCard.setStyle(GuiTheme.panelStyle());
        return controlsCard;
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

        GuiTheme.styleWindowTitle(title, 28);
        GuiTheme.styleWindowTitle(playersTitle, 20);
        Color turnColor = isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        if (resultCard.isVisible()) {
            turnColor = isFirstPlayerWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        }
        GuiTheme.styleTurnLabel(playerTurn, turnColor);
        GuiTheme.styleCompactButton(undoButton);
        GuiTheme.styleCompactButton(newGameButton);
        GuiTheme.styleThemeButton(themeButton);

        firstPlayerWallsLabel.setTextFill(GuiTheme.playerOne());
        secondPlayerWallsLabel.setTextFill(GuiTheme.playerTwo());
        firstPlayerNameLabel.setTextFill(GuiTheme.text());
        secondPlayerNameLabel.setTextFill(GuiTheme.text());
        controlsTitle.setTextFill(GuiTheme.text());
        resultTitle.setTextFill(GuiTheme.text());
        if (resultCard.isVisible()) {
            winnerLabel.setTextFill(isFirstPlayerWinner ? GuiTheme.playerOne() : GuiTheme.playerTwo());
        }

        firstPlayerCard.setStyle(GuiTheme.playerCardStyle(GuiTheme.playerOne()));
        secondPlayerCard.setStyle(GuiTheme.playerCardStyle(GuiTheme.playerTwo()));
        controlsCard.setStyle(GuiTheme.panelStyle());
        resultCard.setStyle(GuiTheme.panelStyle());
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
        resultCard.setVisible(false);
        resultCard.setManaged(false);
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

        boolean firstPlayerMoved = move.getPlayer().getColor() == Color.CYAN;
        isFirstPlayerTurn = !firstPlayerMoved;
        playerTurn.setText(isFirstPlayerTurn ? firstPlayerDisplayName + "'s turn" : secondPlayerDisplayName + "'s turn");
        GuiTheme.styleTurnLabel(playerTurn, isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo());
    }

    private void drawPawn(Move move) {
        Circle currentPlayerPawn = move.getPlayer().getColor() == Color.CYAN
                ? firstPlayerPawn
                : secondPlayerPawn;

        gridPane.getChildren().remove(currentPlayerPawn);
        gridPane.add(currentPlayerPawn, move.getTargetCol(), move.getTargetRow());
    }

    private void drawWall(Move move) {
        Color playerColor = move.getPlayer().getColor();
        int playerWalls = move.getPlayer().wallsLeft();

        if (playerColor == Color.CYAN) {
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
            Color moveColor = move.getPlayer().getColor() == Color.CYAN
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
}
