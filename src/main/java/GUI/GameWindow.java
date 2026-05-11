package GUI;

import javafx.animation.PauseTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Move;
import model.MoveType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GameWindow {

    private static final int BOARD_SIZE = 9;
    private static final int INITIAL_WALLS = 10;
    private static final int CELL_SIZE = 50;
    private static final int CELL_WITH_STROKE = 56;
    private static final int SCENE_WIDTH = 508;
    private static final int SCENE_HEIGHT = 600;

    private final Stage stage;
    private final GridPane gridPane;
    private final BiConsumer<Integer, Integer> boardClickHandler;
    private final List<Circle> possiblePawnMoves;

    private Circle firstPlayerPawn;
    private Circle secondPlayerPawn;
    private Label firstPlayerWallsLabel;
    private Label secondPlayerWallsLabel;
    private Label playerTurn;

    public GameWindow(BiConsumer<Integer, Integer> boardClickHandler) {
        this.stage = new Stage();
        this.gridPane = new GridPane();
        this.boardClickHandler = boardClickHandler;
        this.possiblePawnMoves = new ArrayList<>();
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.show();
    }

    public void close() {
        stage.close();
    }

    private Scene createScene() {
        createStatusLabels();
        createBoard();
        createPawns();

        VBox content = new VBox(5, secondPlayerWallsLabel, gridPane, firstPlayerWallsLabel, playerTurn);
        content.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane(content);
        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private void createStatusLabels() {
        firstPlayerWallsLabel = createWallsLabel(Color.CYAN);
        secondPlayerWallsLabel = createWallsLabel(Color.ORANGE);
        playerTurn = new Label("First Player's turn");
    }

    private Label createWallsLabel(Color color) {
        Label label = new Label("Walls left: " + INITIAL_WALLS);
        label.setTextFill(color);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        return label;
    }

    private void createBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE, Color.BLUE);
                cell.setStroke(Color.WHITE);
                cell.setStrokeWidth(6);
                gridPane.add(cell, col, row);
            }
        }

        gridPane.setOnMouseClicked(event ->
                boardClickHandler.accept((int) event.getX(), (int) event.getY()));
    }

    private void createPawns() {
        firstPlayerPawn = createPawn(Color.CYAN);
        secondPlayerPawn = createPawn(Color.ORANGE);

        gridPane.add(firstPlayerPawn, 4, 8);
        gridPane.add(secondPlayerPawn, 4, 0);
    }

    private Circle createPawn(Color color) {
        Circle pawn = new Circle(10, color);
        GridPane.setHalignment(pawn, HPos.CENTER);
        GridPane.setValignment(pawn, VPos.CENTER);
        return pawn;
    }

    public void draw(Move move) {
        if (move.getType() == MoveType.PAWN_MOVE) {
            drawPawn(move);
        } else {
            drawWall(move);
        }

        playerTurn.setText(move.getPlayer().getColor() == Color.CYAN
                ? "Second Player's turn"
                : "First Player's turn");
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
            firstPlayerWallsLabel.setText("Walls left: " + playerWalls);
        } else {
            secondPlayerWallsLabel.setText("Walls left: " + playerWalls);
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
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(6);

        Pane linePane = new Pane(line);
        gridPane.add(linePane, 0, 0, BOARD_SIZE, BOARD_SIZE);
    }

    public void drawPossiblePawnMoves(List<Move> moves) {
        for (Move move : moves) {
            Circle smallCircle = new Circle(5, move.getPlayer().getColor());
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
        illegalMoveLabel.setTextFill(Color.RED);
        illegalMoveLabel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        illegalMoveLabel.setAlignment(Pos.CENTER);
        illegalMoveLabel.setFont(new Font("Arial", 30));

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
