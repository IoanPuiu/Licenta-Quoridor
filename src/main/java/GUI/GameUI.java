package GUI;

import javafx.application.Application;
import javafx.stage.Stage;
import model.Game;
import model.Move;
import model.Player;

import java.util.List;
import java.util.concurrent.Semaphore;

public class GameUI extends Application {

    private final Semaphore clickSemaphore = new Semaphore(0);

    private volatile int boardX;
    private volatile int boardY;
    private GameWindow gameWindow;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        showStartWindow(primaryStage);
    }

    private void showStartWindow(Stage stage) {
        StartWindow startWindow = new StartWindow(stage, this::startGame);
        startWindow.show();
    }

    private void startGame(boolean isFirstPlayerAI, boolean isSecondPlayerAI) {
        resetInputState();

        gameWindow = new GameWindow(this::storeClickCoordinates);
        gameWindow.show();

        new Game(this, isFirstPlayerAI, isSecondPlayerAI);
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

    public void drawPossiblePawnMoves(List<Move> moves) {
        gameWindow.drawPossiblePawnMoves(moves);
    }

    public void deletePossiblePawnMoves() {
        gameWindow.deletePossiblePawnMoves();
    }

    public void endGame(Player player) {
        EndGameWindow endGameWindow = new EndGameWindow(player, this::restartGame);
        endGameWindow.show();
    }

    private void restartGame() {
        if (gameWindow != null) {
            gameWindow.close();
            gameWindow = null;
        }

        showStartWindow(new Stage());
    }

    public void illegalMove() {
        gameWindow.showIllegalMove();
    }
}
