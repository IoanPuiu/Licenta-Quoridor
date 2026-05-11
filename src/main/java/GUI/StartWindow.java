package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.PlayerProfile;
import model.PlayerType;

import java.util.function.BiConsumer;

public class StartWindow {

    private static final int SCENE_WIDTH = 500;
    private static final int SCENE_HEIGHT = 570;

    private final Stage stage;
    private final BiConsumer<PlayerProfile, PlayerProfile> startGameHandler;
    private VBox content;
    private VBox setupPanel;
    private Label title;
    private Label firstPlayerLabel;
    private Label secondPlayerLabel;
    private ComboBox<PlayerType> firstPlayer;
    private ComboBox<PlayerType> secondPlayer;
    private TextField firstPlayerName;
    private TextField secondPlayerName;
    private Button startButton;

    public StartWindow(Stage stage, BiConsumer<PlayerProfile, PlayerProfile> startGameHandler) {
        this.stage = stage;
        this.startGameHandler = startGameHandler;
    }

    public void show() {
        stage.setScene(createScene());
        stage.setTitle("Quoridor");
        stage.show();
    }

    private Scene createScene() {
        firstPlayer = createPlayerComboBox("Choose First Player");
        secondPlayer = createPlayerComboBox("Choose Second Player");
        firstPlayerName = createPlayerNameField("First player name");
        secondPlayerName = createPlayerNameField("Second player name");

        startButton = new Button("Start Match");
        startButton.setDisable(true);
        startButton.setMaxWidth(Double.MAX_VALUE);
        GuiTheme.stylePrimaryButton(startButton);

        firstPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                updatePlayerNameField(firstPlayer, firstPlayerName));
        secondPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                updatePlayerNameField(secondPlayer, secondPlayerName));
        firstPlayerName.textProperty().addListener((obs, oldValue, newValue) -> updateStartButtonState());
        secondPlayerName.textProperty().addListener((obs, oldValue, newValue) -> updateStartButtonState());

        startButton.setOnAction(event -> {
            stage.close();
            startGameHandler.accept(createPlayerProfile(firstPlayer, firstPlayerName), createPlayerProfile(secondPlayer, secondPlayerName));
        });

        title = new Label("Quoridor");
        GuiTheme.styleWindowTitle(title, 42);

        firstPlayerLabel = createFieldLabel("First Player");
        secondPlayerLabel = createFieldLabel("Second Player");
        MenuButton themeButton = GuiTheme.createThemeMenu(this::applyTheme);
        themeButton.setMaxWidth(Double.MAX_VALUE);

        HBox actionRow = new HBox(10, startButton, themeButton);
        actionRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(startButton, Priority.ALWAYS);
        HBox.setHgrow(themeButton, Priority.ALWAYS);

        setupPanel = new VBox(12,
                firstPlayerLabel,
                firstPlayer,
                firstPlayerName,
                secondPlayerLabel,
                secondPlayer,
                secondPlayerName,
                actionRow);
        setupPanel.setAlignment(Pos.CENTER_LEFT);
        setupPanel.setPadding(new Insets(28));
        setupPanel.setMaxWidth(360);
        setupPanel.setStyle(GuiTheme.panelStyle());

        content = new VBox(24, title, setupPanel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(36));
        content.setStyle(GuiTheme.rootStyle());

        return new Scene(content, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private ComboBox<PlayerType> createPlayerComboBox(String promptText) {
        ComboBox<PlayerType> player = new ComboBox<>();
        player.getItems().addAll(PlayerType.values());
        player.setPromptText(promptText);
        GuiTheme.styleComboBox(player);
        return player;
    }

    private TextField createPlayerNameField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.setManaged(false);
        textField.setVisible(false);
        GuiTheme.styleTextField(textField);
        return textField;
    }

    private void applyTheme() {
        content.setStyle(GuiTheme.rootStyle());
        setupPanel.setStyle(GuiTheme.panelStyle());
        GuiTheme.styleWindowTitle(title, 42);
        GuiTheme.styleMutedLabel(firstPlayerLabel);
        GuiTheme.styleMutedLabel(secondPlayerLabel);
        GuiTheme.styleComboBox(firstPlayer);
        GuiTheme.styleComboBox(secondPlayer);
        GuiTheme.styleTextField(firstPlayerName);
        GuiTheme.styleTextField(secondPlayerName);
        GuiTheme.stylePrimaryButton(startButton);
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        return label;
    }

    private void updatePlayerNameField(ComboBox<PlayerType> playerType, TextField playerName) {
        boolean isHuman = playerType.getValue() == PlayerType.HUMAN;
        playerName.setManaged(isHuman);
        playerName.setVisible(isHuman);
        if (!isHuman) {
            playerName.clear();
        }
        updateStartButtonState();
    }

    private void updateStartButtonState() {
        startButton.setDisable(isInvalidSelection(firstPlayer, firstPlayerName)
                || isInvalidSelection(secondPlayer, secondPlayerName));
    }

    private boolean isInvalidSelection(ComboBox<PlayerType> playerType, TextField playerName) {
        return playerType.getValue() == null
                || playerType.getValue() == PlayerType.HUMAN && playerName.getText().isBlank();
    }

    private PlayerProfile createPlayerProfile(ComboBox<PlayerType> playerType, TextField playerName) {
        return new PlayerProfile(playerType.getValue(), playerName.getText());
    }
}
