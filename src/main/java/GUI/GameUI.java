package GUI;

import javafx.application.Application;
import javafx.stage.Stage;
import model.Game;
import model.Move;
import model.Player;
import model.PlayerProfile;

import java.util.List;
import java.util.concurrent.Semaphore;

public class GameUI extends Application {

    private final Semaphore clickSemaphore = new Semaphore(0);

    private volatile int boardX;
    private volatile int boardY;
    private GameWindow gameWindow;
    private Game currentGame;

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

    private void startGame(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile) {
        resetInputState();

        gameWindow = new GameWindow(
                this::storeClickCoordinates,
                this::undoLastHumanMove,
                this::restartGame,
                firstPlayerProfile.displayName("First Player"),
                secondPlayerProfile.displayName("Second Player"));
        gameWindow.show();

        currentGame = new Game(this, firstPlayerProfile, secondPlayerProfile);
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

    public void redrawGame(List<Move> moves, int firstPlayerWalls, int secondPlayerWalls, boolean isFirstPlayerTurn) {
        gameWindow.redraw(moves, firstPlayerWalls, secondPlayerWalls, isFirstPlayerTurn);
    }

    public void setUndoAvailable(boolean undoAvailable) {
        gameWindow.setUndoAvailable(undoAvailable);
    }

    public void endGame(Player player) {
        gameWindow.showGameResult(player);
    }

    private void restartGame() {
        if (gameWindow != null) {
            gameWindow.close();
            gameWindow = null;
        }
        currentGame = null;

        showStartWindow(new Stage());
    }

    public void illegalMove() {
        gameWindow.showIllegalMove();
    }
}
