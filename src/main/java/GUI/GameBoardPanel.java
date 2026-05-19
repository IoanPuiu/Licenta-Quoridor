package GUI;

import PerformanceModel.GameState;
import PerformanceModel.WallImpact;
import SlowModel.Move;
import SlowModel.MoveType;
import SlowModel.Player;
import javafx.animation.PauseTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

final class GameBoardPanel {
    static final int BOARD_SIZE = 9;
    static final int CELL_SIZE = 50;
    static final int CELL_WITH_STROKE = 56;
    static final int BOARD_CONTENT_SIZE = BOARD_SIZE * CELL_WITH_STROKE;
    static final int BOARD_FRAME_PADDING = 12;
    static final int FRAME_SIZE = BOARD_CONTENT_SIZE + BOARD_FRAME_PADDING * 2;

    private final GridPane gridPane;
    private final StackPane view;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final List<Circle> possiblePawnMoves;
    private final List<Rectangle> boardCells;
    private final List<PlacedWallLine> wallLines;
    private Circle firstPlayerPawn;
    private Circle secondPlayerPawn;

    GameBoardPanel(BiConsumer<Integer, Integer> boardClickHandler) {
        this.boardClickHandler = boardClickHandler;
        this.gridPane = new GridPane();
        this.possiblePawnMoves = new ArrayList<>();
        this.boardCells = new ArrayList<>();
        this.wallLines = new ArrayList<>();

        createBoard();
        createPawns();

        view = new StackPane(gridPane);
        view.setPadding(new Insets(BOARD_FRAME_PADDING));
        view.setMinSize(FRAME_SIZE, FRAME_SIZE);
        view.setPrefSize(FRAME_SIZE, FRAME_SIZE);
        view.setMaxSize(FRAME_SIZE, FRAME_SIZE);
        view.setStyle(GuiTheme.boardFrameStyle());
    }

    StackPane view() {
        return view;
    }

    void redraw(List<Move> moves) {
        deletePossiblePawnMoves();
        gridPane.getChildren().clear();
        boardCells.clear();
        wallLines.clear();

        createBoard();
        createPawns();

        for (Move move : moves) {
            draw(move);
        }
    }

    void draw(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            drawPawn(move);
        } else {
            drawWall(move);
        }
    }

    void drawPerformanceMove(int moveCode, boolean isPlayerAMove, WallImpact wallImpact) {
        if (GameState.isPawnMoveCode(moveCode)) {
            drawPerformancePawn(moveCode, isPlayerAMove);
        } else {
            drawPerformanceWall(moveCode, isPlayerAMove, wallImpact);
        }
    }

    void drawPossiblePawnMoves(List<Move> moves) {
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

    void deletePossiblePawnMoves() {
        for (Circle circle : possiblePawnMoves) {
            gridPane.getChildren().remove(circle);
        }
        possiblePawnMoves.clear();
    }

    void showIllegalMove() {
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

    void applyTheme(boolean isFirstPlayerTurn) {
        view.setStyle(GuiTheme.boardFrameStyle());
        gridPane.setStyle(GuiTheme.boardStyle());

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

        for (PlacedWallLine wallLine : wallLines) {
            wallLine.line().setStroke(playerColor(wallLine.isFirstPlayer()));
        }

        Color possibleMoveColor = isFirstPlayerTurn ? GuiTheme.playerOne() : GuiTheme.playerTwo();
        for (Circle circle : possiblePawnMoves) {
            circle.setFill(possibleMoveColor.deriveColor(0, 0.9, 1.2, 0.55));
        }
    }

    private void createBoard() {
        gridPane.setStyle(GuiTheme.boardStyle());
        gridPane.setMinSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setPrefSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
        gridPane.setMaxSize(BOARD_CONTENT_SIZE, BOARD_CONTENT_SIZE);
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
        int x1 = move.isHorizontal()
                ? move.getTargetCol() * CELL_WITH_STROKE + 15
                : move.getTargetCol() * CELL_WITH_STROKE + 56;
        int y1 = move.isHorizontal()
                ? move.getTargetRow() * CELL_WITH_STROKE + 56
                : move.getTargetRow() * CELL_WITH_STROKE + 15;
        int x2 = move.isHorizontal() ? x1 + 80 : x1;
        int y2 = move.isHorizontal() ? y1 : y1 + 80;

        drawLine(x1, x2, y1, y2, isBottomPlayer(move.getPlayer()), move.getWallImpact());
    }

    private void drawPerformanceWall(int moveCode, boolean isPlayerAMove, WallImpact wallImpact) {
        int row = GameState.decodeWallRow(moveCode);
        int col = GameState.decodeWallCol(moveCode);
        boolean isHorizontal = GameState.decodeWallIsHorizontal(moveCode);
        int x1 = isHorizontal ? col * CELL_WITH_STROKE + 15 : col * CELL_WITH_STROKE + 56;
        int y1 = isHorizontal ? row * CELL_WITH_STROKE + 56 : row * CELL_WITH_STROKE + 15;
        int x2 = isHorizontal ? x1 + 80 : x1;
        int y2 = isHorizontal ? y1 : y1 + 80;

        drawLine(x1, x2, y1, y2, isPlayerAMove, wallImpact);
    }

    private void drawLine(int x1, int x2, int y1, int y2, boolean isFirstPlayer, WallImpact wallImpact) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(playerColor(isFirstPlayer));
        line.setStrokeWidth(8);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        wallLines.add(new PlacedWallLine(line, isFirstPlayer));
        installWallImpactTooltip(line, wallImpact);

        Pane linePane = new Pane(line);
        linePane.setPickOnBounds(false);
        gridPane.add(linePane, 0, 0, BOARD_SIZE, BOARD_SIZE);
    }

    private void installWallImpactTooltip(Line line, WallImpact wallImpact) {
        Tooltip tooltip = new Tooltip(wallImpact.displayText());
        tooltip.setShowDelay(Duration.seconds(1));
        tooltip.setHideDelay(Duration.ZERO);
        Tooltip.install(line, tooltip);
    }

    private Color playerColor(boolean isFirstPlayer) {
        return isFirstPlayer ? GuiTheme.playerOne() : GuiTheme.playerTwo();
    }

    private boolean isBottomPlayer(Player player) {
        return player.getFinishRow() == 0;
    }

    private record PlacedWallLine(Line line, boolean isFirstPlayer) {
    }
}
