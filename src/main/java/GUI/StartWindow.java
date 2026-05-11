package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.PlayerType;

import java.util.function.BiConsumer;

public class StartWindow {

    private static final int SCENE_WIDTH = 500;
    private static final int SCENE_HEIGHT = 500;

    private final Stage stage;
    private final BiConsumer<PlayerType, PlayerType> startGameHandler;

    public StartWindow(Stage stage, BiConsumer<PlayerType, PlayerType> startGameHandler) {
        this.stage = stage;
        this.startGameHandler = startGameHandler;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.show();
    }

    private Scene createScene() {
        ComboBox<PlayerType> firstPlayer = createPlayerComboBox("Choose First Player");
        ComboBox<PlayerType> secondPlayer = createPlayerComboBox("Choose Second Player");

        Button startButton = new Button("Start");
        startButton.setDisable(true);

        firstPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                startButton.setDisable(isInvalidSelection(firstPlayer, secondPlayer)));
        secondPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                startButton.setDisable(isInvalidSelection(firstPlayer, secondPlayer)));

        startButton.setOnAction(event -> {
            stage.close();
            startGameHandler.accept(firstPlayer.getValue(), secondPlayer.getValue());
        });

        VBox content = new VBox(10, firstPlayer, secondPlayer, startButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        return new Scene(new BorderPane(content), SCENE_WIDTH, SCENE_HEIGHT);
    }

    private ComboBox<PlayerType> createPlayerComboBox(String promptText) {
        ComboBox<PlayerType> player = new ComboBox<>();
        player.getItems().addAll(PlayerType.values());
        player.setPromptText(promptText);
        return player;
    }

    private boolean isInvalidSelection(ComboBox<PlayerType> firstPlayer, ComboBox<PlayerType> secondPlayer) {
        return firstPlayer.getValue() == null || secondPlayer.getValue() == null;
    }
}
