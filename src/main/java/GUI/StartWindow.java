package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.PlayerProfile;
import model.PlayerType;

public class StartWindow {

    private static final int SCENE_WIDTH = 500;
    private static final int SCENE_HEIGHT = 650;

    private final Stage stage;
    private final StartGameHandler startGameHandler;
    private VBox content;
    private VBox setupPanel;
    private Label title;
    private Label firstPlayerLabel;
    private Label secondPlayerLabel;
    private Label autoRematchesLabel;
    private ComboBox<PlayerType> firstPlayer;
    private ComboBox<PlayerType> secondPlayer;
    private TextField firstPlayerName;
    private TextField secondPlayerName;
    private Spinner<Integer> autoRematches;
    private Button startButton;
    private MenuButton themeButton;

    public StartWindow(Stage stage, StartGameHandler startGameHandler) {
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
        autoRematches = createAutoRematchesSpinner();

        startButton = new Button("Start Match");
        startButton.setDisable(true);
        startButton.setMaxWidth(Double.MAX_VALUE);
        GuiTheme.stylePrimaryButton(startButton);

        firstPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                updatePlayerSelection(firstPlayer, firstPlayerName));
        secondPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                updatePlayerSelection(secondPlayer, secondPlayerName));
        firstPlayerName.textProperty().addListener((obs, oldValue, newValue) -> updateStartButtonState());
        secondPlayerName.textProperty().addListener((obs, oldValue, newValue) -> updateStartButtonState());

        startButton.setOnAction(event -> {
            stage.close();
            startGameHandler.startGame(
                    createPlayerProfile(firstPlayer, firstPlayerName),
                    createPlayerProfile(secondPlayer, secondPlayerName),
                    automaticRematchCount());
        });

        title = new Label("Quoridor");
        GuiTheme.styleWindowTitle(title, 42);

        firstPlayerLabel = createFieldLabel("First Player");
        secondPlayerLabel = createFieldLabel("Second Player");
        autoRematchesLabel = createFieldLabel("Auto rematches");
        updateAutoRematchControls();

        themeButton = GuiTheme.createThemeMenu(this::applyTheme);
        themeButton.setMaxWidth(Double.MAX_VALUE);

        VBox actionArea = new VBox(10, startButton, themeButton);
        actionArea.setAlignment(Pos.CENTER);
        actionArea.setPadding(new Insets(8, 0, 0, 0));

        setupPanel = new VBox(12,
                firstPlayerLabel,
                firstPlayer,
                firstPlayerName,
                secondPlayerLabel,
                secondPlayer,
                secondPlayerName,
                autoRematchesLabel,
                autoRematches,
                actionArea);
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

    private Spinner<Integer> createAutoRematchesSpinner() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinner.setEditable(true);
        GuiTheme.styleSpinner(spinner);
        return spinner;
    }

    private void applyTheme() {
        content.setStyle(GuiTheme.rootStyle());
        setupPanel.setStyle(GuiTheme.panelStyle());
        GuiTheme.styleWindowTitle(title, 42);
        GuiTheme.styleMutedLabel(firstPlayerLabel);
        GuiTheme.styleMutedLabel(secondPlayerLabel);
        GuiTheme.styleMutedLabel(autoRematchesLabel);
        GuiTheme.styleComboBox(firstPlayer);
        GuiTheme.styleComboBox(secondPlayer);
        GuiTheme.styleTextField(firstPlayerName);
        GuiTheme.styleTextField(secondPlayerName);
        GuiTheme.styleSpinner(autoRematches);
        GuiTheme.stylePrimaryButton(startButton);
        GuiTheme.styleThemeButton(themeButton);
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        return label;
    }

    private void updatePlayerSelection(ComboBox<PlayerType> playerType, TextField playerName) {
        updatePlayerNameField(playerType, playerName);
        updateAutoRematchControls();
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

    private void updateAutoRematchControls() {
        boolean isAiVsAi = isAI(firstPlayer) && isAI(secondPlayer);
        autoRematchesLabel.setManaged(isAiVsAi);
        autoRematchesLabel.setVisible(isAiVsAi);
        autoRematches.setManaged(isAiVsAi);
        autoRematches.setVisible(isAiVsAi);
        if (!isAiVsAi) {
            autoRematches.getValueFactory().setValue(0);
        }
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

    private int automaticRematchCount() {
        if (!isAI(firstPlayer) || !isAI(secondPlayer)) {
            return 0;
        }

        try {
            int value = Integer.parseInt(autoRematches.getEditor().getText().trim());
            value = Math.max(0, Math.min(100, value));
            autoRematches.getValueFactory().setValue(value);
            return value;
        } catch (NumberFormatException e) {
            return autoRematches.getValue();
        }
    }

    private boolean isAI(ComboBox<PlayerType> playerType) {
        return playerType.getValue() != null && playerType.getValue().isAI();
    }

    @FunctionalInterface
    public interface StartGameHandler {
        void startGame(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile, int automaticRematches);
    }
}
