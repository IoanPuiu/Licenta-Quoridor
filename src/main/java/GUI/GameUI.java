package GUI;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import model.*;

public class GameUI extends Application {

    private GridPane gridPane;
    private Circle firstPlayerPawn;
    private Circle secondPlayerPawn;
    private int firstPlayerWalls;
    private int secondPlayerWalls;
    private Label firstPlayerWallsLabel;
    private Label secondPlayerWallsLabel;
    private Label playerTurn;
    private volatile int boardX;
    private volatile int boardY;

    private final Semaphore clickSemaphore = new Semaphore(0);
    private Stage gameStage;
    private List<Circle> possiblePawnMoves;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        firstPlayerWalls = 10;
        secondPlayerWalls = 10;
        possiblePawnMoves = new ArrayList<>();

        ComboBox<String> firstPlayer = new ComboBox<>();
        firstPlayer.getItems().addAll("AI", "Human");
        firstPlayer.setPromptText("Choose First Player Intelligence");

        ComboBox<String> secondPlayer = new ComboBox<>();
        secondPlayer.getItems().addAll("AI", "Human");
        secondPlayer.setPromptText("Choose Second Player Intelligence");

        boolean isFirstPlayerAI = Objects.equals(firstPlayer.getSelectionModel().getSelectedItem(), "AI");
        boolean isSecondPlayerAI = Objects.equals(secondPlayer.getSelectionModel().getSelectedItem(), "AI");

        Button startButton = new Button("Start");
        startButton.setDisable(true);

        firstPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> startButton.setDisable(isInvalidSelection(newSelection, secondPlayer.getValue())));
        secondPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> startButton.setDisable(isInvalidSelection(newSelection, firstPlayer.getValue())));

        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(firstPlayer, secondPlayer, startButton);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(30));

        firstPlayerWallsLabel = new Label("Walls left: " + firstPlayerWalls);
        firstPlayerWallsLabel.setTextFill(Color.CYAN);
        firstPlayerWallsLabel.setFont(Font.font("null", FontWeight.BOLD, 20));
        firstPlayerWallsLabel.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        secondPlayerWallsLabel = new Label("Walls left: " + secondPlayerWalls);
        secondPlayerWallsLabel.setTextFill(Color.ORANGE);
        secondPlayerWallsLabel.setFont(Font.font("null", FontWeight.BOLD, 20));

        playerTurn = new Label("First Player's turn");

        gridPane = new GridPane();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Rectangle rect = new Rectangle(50, 50, Color.BLUE);
                rect.setStroke(Color.WHITE);
                rect.setStrokeWidth(6);
                gridPane.add(rect, j, i);
            }
        }

        gridPane.setOnMouseClicked(event -> {
            boardX = (int) event.getX();
            boardY = (int) event.getY();
            clickSemaphore.release();
        });


        startButton.setOnAction(event -> {
            primaryStage.close();
            gameStage = new Stage();
            BorderPane root = new BorderPane(gridPane);

            VBox centerVBox = new VBox(secondPlayerWallsLabel, gridPane, firstPlayerWallsLabel, playerTurn);
            centerVBox.setAlignment(Pos.CENTER);
            centerVBox.setSpacing(5);
            root.setCenter(centerVBox);

            firstPlayerPawn = new Circle(10, Color.CYAN);
            GridPane.setHalignment(firstPlayerPawn, HPos.CENTER);
            GridPane.setValignment(firstPlayerPawn, VPos.CENTER);
            gridPane.add(firstPlayerPawn, 4, 8);

            secondPlayerPawn = new Circle(10, Color.ORANGE);
            GridPane.setHalignment(secondPlayerPawn, HPos.CENTER);
            GridPane.setValignment(secondPlayerPawn, VPos.CENTER);
            gridPane.add(secondPlayerPawn, 4, 0);

            Scene scene = new Scene(root, 508, 600);
            gameStage.setScene(scene);
            gameStage.setTitle("Quoridor");
            gameStage.show();

            new Game(this, isFirstPlayerAI, isSecondPlayerAI);
        });

        BorderPane root = new BorderPane(vBox);
        Scene scene = new Scene(root, 500, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Quoridor");
        primaryStage.show();
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
        if (move.getType() == MoveType.PAWN_MOVE) {
            drawPawn(move);
        } else drawWall(move);
        playerTurn.setText(move.getPlayer().getColor() == Color.CYAN ? "Second Player's turn" : "First Player's turn");
    }

    private void drawWall(Move move) {

        Color playerColor = move.getPlayer().getColor();
        int playerWalls = move.getPlayer().wallsLeft();
        if (playerColor == Color.CYAN) {
            firstPlayerWalls = playerWalls;
            firstPlayerWallsLabel.setText("Walls left: " + firstPlayerWalls);
        } else {
            secondPlayerWalls = playerWalls;
            secondPlayerWallsLabel.setText("Walls left: " + secondPlayerWalls);
        }

        int cellWidth = 56;

        int x1 = move.isHorizontal() ? move.getTargetCol() * cellWidth + 15 : move.getTargetCol() * cellWidth + 56;
        int y1 = move.isHorizontal() ? move.getTargetRow() * cellWidth + 56 : move.getTargetRow() * cellWidth + 15;
        int x2 = move.isHorizontal() ? x1 + 80 : x1;
        int y2 = move.isHorizontal() ? y1 : y1 + 80;

        drawLine(x1, x2, y1, y2);
    }

    private void drawLine(int x1, int x2, int y1, int y2) {
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(x1, y1, x2, y2);
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(6);
        Pane linePane = new Pane();
        linePane.getChildren().add(line);
        gridPane.add(linePane, 0, 0, 9, 9);
    }

    private void drawPawn(Move move) {
        Circle currentPlayerPawn;
        currentPlayerPawn = move.getPlayer().getColor() == Color.CYAN ? firstPlayerPawn : secondPlayerPawn;
        gridPane.getChildren().remove(currentPlayerPawn);
        gridPane.add(currentPlayerPawn, move.getTargetCol(), move.getTargetRow());
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

    private boolean isInvalidSelection(String player1, String player2) {
        return player1 == null || player2 == null;
    }

    public void endGame(Player player) {

        Stage endGameStage = new Stage();

        Label winnerLabel = new Label(player + " won!");
        winnerLabel.setFont(new Font("Arial", 40));
        winnerLabel.setTextFill(player.getColor());

        Button newGameButton = new Button("Start New Game");
        newGameButton.setOnAction(event -> {
            gameStage.close();
            endGameStage.close();
            Stage newGameStage = new Stage();
            try {
                this.start(newGameStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(winnerLabel, newGameButton);

        Scene endScene = new Scene(vBox, 400, 200);
        endGameStage.setScene(endScene);
        endGameStage.setTitle("Game Over");
        endGameStage.show();
    }

    public void illegalMove() {
        Label illegalMoveLabel = new Label("Illegal move");
        illegalMoveLabel.setTextFill(Color.RED);
        illegalMoveLabel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        illegalMoveLabel.setAlignment(Pos.CENTER);
        illegalMoveLabel.setFont(new Font("Arial", 30));
        GridPane.setHalignment(illegalMoveLabel, HPos.CENTER);
        GridPane.setValignment(illegalMoveLabel, VPos.CENTER);
        GridPane.setConstraints(illegalMoveLabel, 0, 0, 9, 9);
        GridPane.setFillWidth(illegalMoveLabel, true);
        GridPane.setFillHeight(illegalMoveLabel, true);
        gridPane.getChildren().add(illegalMoveLabel);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> gridPane.getChildren().remove(illegalMoveLabel));
        pause.play();
    }

}
