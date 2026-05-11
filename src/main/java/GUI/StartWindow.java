package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.BiConsumer;

public class StartWindow {

    private static final int SCENE_WIDTH = 500;
    private static final int SCENE_HEIGHT = 500;

    private final Stage stage;
    private final BiConsumer<Boolean, Boolean> startGameHandler;

    public StartWindow(Stage stage, BiConsumer<Boolean, Boolean> startGameHandler) {
        this.stage = stage;
        this.startGameHandler = startGameHandler;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.show();
    }

    private Scene createScene() {
        ComboBox<String> firstPlayer = createPlayerComboBox("Choose First Player Intelligence");
        ComboBox<String> secondPlayer = createPlayerComboBox("Choose Second Player Intelligence");

        Button startButton = new Button("Start");
        startButton.setDisable(true);

        firstPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                startButton.setDisable(isInvalidSelection(firstPlayer, secondPlayer)));
        secondPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                startButton.setDisable(isInvalidSelection(firstPlayer, secondPlayer)));

        startButton.setOnAction(event -> {
            boolean isFirstPlayerAI = isAI(firstPlayer);
            boolean isSecondPlayerAI = isAI(secondPlayer);

            stage.close();
            startGameHandler.accept(isFirstPlayerAI, isSecondPlayerAI);
        });

        VBox content = new VBox(10, firstPlayer, secondPlayer, startButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));

        return new Scene(new BorderPane(content), SCENE_WIDTH, SCENE_HEIGHT);
    }

    private ComboBox<String> createPlayerComboBox(String promptText) {
        ComboBox<String> player = new ComboBox<>();
        player.getItems().addAll("AI", "Human");
        player.setPromptText(promptText);
        return player;
    }

    private boolean isInvalidSelection(ComboBox<String> firstPlayer, ComboBox<String> secondPlayer) {
        return firstPlayer.getValue() == null || secondPlayer.getValue() == null;
    }

    private boolean isAI(ComboBox<String> player) {
        return "AI".equals(player.getValue());
    }
}
