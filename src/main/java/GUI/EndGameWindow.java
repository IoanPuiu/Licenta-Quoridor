package GUI;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.Player;

public class EndGameWindow {

    private static final int SCENE_WIDTH = 400;
    private static final int SCENE_HEIGHT = 200;

    private final Stage stage;
    private final Player winner;
    private final Runnable newGameHandler;

    public EndGameWindow(Player winner, Runnable newGameHandler) {
        this.stage = new Stage();
        this.winner = winner;
        this.newGameHandler = newGameHandler;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Game Over");
        stage.show();
    }

    private Scene createScene() {
        Label winnerLabel = new Label(winner + " won!");
        winnerLabel.setFont(new Font("Arial", 40));
        winnerLabel.setTextFill(winner.getColor());

        Button newGameButton = new Button("Start New Game");
        newGameButton.setOnAction(event -> {
            stage.close();
            newGameHandler.run();
        });

        VBox content = new VBox(10, winnerLabel, newGameButton);
        content.setAlignment(Pos.CENTER);

        return new Scene(content, SCENE_WIDTH, SCENE_HEIGHT);
    }
}
