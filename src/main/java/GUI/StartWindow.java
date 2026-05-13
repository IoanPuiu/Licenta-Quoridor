package GUI;

import AI.MiniMax.MoveOrdering;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import SlowModel.PlayerProfile;

import java.util.ArrayList;
import java.util.List;

public class StartWindow {

    private static final int SCENE_WIDTH = 980;
    private static final int SCENE_HEIGHT = 760;
    private static final int MAX_AUTO_REMATCHES = 100;
    private static final int AUTO_REMATCH_INCREMENT = 10;
    private static final int MTCS_EASY_DEPTH = 10_000;
    private static final int MTCS_MEDIUM_DEPTH = 30_000;
    private static final int MTCS_HARD_DEPTH = 60_000;

    private final Stage stage;
    private final StartGameHandler startGameHandler;
    private final List<PlayerProfile> pickedPlayers = new ArrayList<>();
    private final List<VBox> categoryCards = new ArrayList<>();
    private final List<Label> sectionLabels = new ArrayList<>();
    private final List<Label> mutedLabels = new ArrayList<>();
    private final List<Button> pickButtons = new ArrayList<>();
    private final List<ComboBox<?>> comboBoxes = new ArrayList<>();
    private final List<ToggleButton> segmentButtons = new ArrayList<>();

    private VBox content;
    private VBox setupPanel;
    private VBox currentSelectionPanel;
    private Label title;
    private Label currentSelectionTitle;
    private Label firstSelectionLabel;
    private Label secondSelectionLabel;
    private Label autoRematchesLabel;
    private TextField humanName;
    private ComboBox<Integer> minimaxDepth;
    private ComboBox<Integer> mtcsDepth;
    private ToggleGroup minimaxMoveOrderingGroup;
    private HBox minimaxMoveOrderingSegments;
    private Spinner<Integer> autoRematches;
    private Button humanPickButton;
    private Button minimaxPickButton;
    private Button mtcsPickButton;
    private Button gymPickButton;
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
        humanName = createPlayerNameField("Human name");
        minimaxDepth = createDepthComboBox(2, 3);
        mtcsDepth = createDepthComboBox(MTCS_EASY_DEPTH, MTCS_MEDIUM_DEPTH, MTCS_HARD_DEPTH);
        minimaxMoveOrderingGroup = new ToggleGroup();
        minimaxMoveOrderingSegments = createMoveOrderingSegments();
        autoRematches = createAutoRematchesSpinner();

        humanPickButton = createPickButton("Pick");
        minimaxPickButton = createPickButton("Pick");
        mtcsPickButton = createPickButton("Pick");
        gymPickButton = createPickButton("Pick");
        startButton = new Button("Start Match");
        startButton.setDisable(true);
        startButton.setMaxWidth(Double.MAX_VALUE);
        GuiTheme.stylePrimaryButton(startButton);

        humanPickButton.setDisable(true);
        humanName.textProperty().addListener((obs, oldValue, newValue) ->
                humanPickButton.setDisable(newValue == null || newValue.isBlank()));

        humanPickButton.setOnAction(event -> pickPlayer(PlayerProfile.human(humanName.getText())));
        minimaxPickButton.setOnAction(event -> pickPlayer(PlayerProfile.minimax(
                minimaxDepth.getValue(),
                selectedMoveOrdering())));
        mtcsPickButton.setOnAction(event -> pickPlayer(PlayerProfile.mtcs(mtcsDepth.getValue())));
        gymPickButton.setOnAction(event -> pickPlayer(PlayerProfile.gymPython()));
        startButton.setOnAction(event -> startSelectedMatch());

        title = new Label("Quoridor");
        GuiTheme.styleWindowTitle(title, 46);

        setupPanel = new VBox(14, createCategoryGrid());
        setupPanel.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(setupPanel, Priority.ALWAYS);

        currentSelectionPanel = createCurrentSelectionPanel();
        currentSelectionPanel.setPrefWidth(280);
        currentSelectionPanel.setMaxWidth(280);

        HBox body = new HBox(18, setupPanel, currentSelectionPanel);
        body.setAlignment(Pos.TOP_CENTER);

        content = new VBox(26, title, body);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(34));
        content.setStyle(GuiTheme.rootStyle());

        updateSelectionPanel();
        return new Scene(content, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private GridPane createCategoryGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        leftColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        rightColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftColumn, rightColumn);

        grid.add(createHumanCard(), 0, 0);
        grid.add(createMinimaxCard(), 1, 0);
        grid.add(createMtcsCard(), 0, 1);
        grid.add(createGymCard(), 1, 1);
        return grid;
    }

    private VBox createHumanCard() {
        return createCategoryCard(
                "Human",
                createFieldLabel("Name"),
                humanName,
                humanPickButton);
    }

    private VBox createMinimaxCard() {
        return createCategoryCard(
                "Minimax",
                createFieldLabel("Depth"),
                minimaxDepth,
                createFieldLabel("Move ordering"),
                minimaxMoveOrderingSegments,
                minimaxPickButton);
    }

    private VBox createMtcsCard() {
        return createCategoryCard(
                "MTCS",
                createFieldLabel("Depth"),
                mtcsDepth,
                mtcsPickButton);
    }

    private VBox createGymCard() {
        return createCategoryCard(
                "Gym",
                createFieldLabel("No options yet"),
                gymPickButton);
    }

    private VBox createCategoryCard(String heading, Node... nodes) {
        Label titleLabel = new Label(heading);
        sectionLabels.add(titleLabel);
        GuiTheme.styleWindowTitle(titleLabel, 18);

        VBox card = new VBox(10);
        card.getChildren().add(titleLabel);
        card.getChildren().addAll(nodes);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setMinHeight(220);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(GuiTheme.panelStyle());
        categoryCards.add(card);
        return card;
    }

    private VBox createCurrentSelectionPanel() {
        currentSelectionTitle = new Label("Current picks");
        sectionLabels.add(currentSelectionTitle);
        GuiTheme.styleWindowTitle(currentSelectionTitle, 18);

        firstSelectionLabel = createSelectionLabel();
        secondSelectionLabel = createSelectionLabel();
        autoRematchesLabel = createFieldLabel("Auto rematches");

        themeButton = GuiTheme.createThemeMenu(this::applyTheme);
        themeButton.setMaxWidth(Double.MAX_VALUE);

        VBox actionArea = new VBox(10, startButton, themeButton);
        actionArea.setAlignment(Pos.CENTER);
        actionArea.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(
                12,
                currentSelectionTitle,
                firstSelectionLabel,
                secondSelectionLabel,
                autoRematchesLabel,
                autoRematches,
                actionArea);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(20));
        panel.setStyle(GuiTheme.panelStyle());
        return panel;
    }

    private Label createSelectionLabel() {
        Label label = new Label();
        label.setWrapText(true);
        GuiTheme.styleMutedLabel(label);
        return label;
    }

    private Button createPickButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        GuiTheme.styleCompactButton(button);
        pickButtons.add(button);
        return button;
    }

    private TextField createPlayerNameField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        GuiTheme.styleTextField(textField);
        return textField;
    }

    private ComboBox<Integer> createDepthComboBox(Integer... values) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(values);
        comboBox.getSelectionModel().selectFirst();
        GuiTheme.styleComboBox(comboBox);
        comboBoxes.add(comboBox);
        return comboBox;
    }

    private HBox createMoveOrderingSegments() {
        HBox segments = new HBox(6);
        segments.setMaxWidth(Double.MAX_VALUE);

        ToggleButton noOrderingButton = null;
        for (MoveOrdering ordering : MoveOrdering.values()) {
            ToggleButton button = new ToggleButton(segmentText(ordering));
            button.setUserData(ordering);
            button.setToggleGroup(minimaxMoveOrderingGroup);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAccessibleText(ordering.label());
            button.selectedProperty().addListener((obs, oldValue, newValue) ->
                    GuiTheme.styleSegmentButton(button));
            HBox.setHgrow(button, Priority.ALWAYS);
            GuiTheme.styleSegmentButton(button);
            segmentButtons.add(button);
            segments.getChildren().add(button);

            if (ordering == MoveOrdering.NONE) {
                noOrderingButton = button;
            }
        }

        ToggleButton defaultButton = noOrderingButton == null
                ? (ToggleButton) segments.getChildren().get(0)
                : noOrderingButton;
        minimaxMoveOrderingGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
        defaultButton.setSelected(true);
        return segments;
    }

    private String segmentText(MoveOrdering ordering) {
        return switch (ordering) {
            case NONE -> "No";
            case FAST -> "Fast";
            case PRECISE -> "Precise";
        };
    }

    private MoveOrdering selectedMoveOrdering() {
        Toggle selectedToggle = minimaxMoveOrderingGroup.getSelectedToggle();
        if (selectedToggle == null || !(selectedToggle.getUserData() instanceof MoveOrdering ordering)) {
            return MoveOrdering.NONE;
        }
        return ordering;
    }

    private Spinner<Integer> createAutoRematchesSpinner() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                MAX_AUTO_REMATCHES,
                0,
                AUTO_REMATCH_INCREMENT));
        spinner.setEditable(true);
        GuiTheme.styleSpinner(spinner);
        return spinner;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        mutedLabels.add(label);
        return label;
    }

    private void pickPlayer(PlayerProfile playerProfile) {
        pickedPlayers.add(playerProfile);
        if (pickedPlayers.size() > 2) {
            pickedPlayers.remove(0);
        }
        updateSelectionPanel();
    }

    private void updateSelectionPanel() {
        firstSelectionLabel.setText(selectionText(0, "Player 1: pick an intelligence"));
        secondSelectionLabel.setText(selectionText(1, "Player 2: pick an intelligence"));
        startButton.setDisable(pickedPlayers.size() < 2);
        updateAutoRematchControls();
    }

    private String selectionText(int index, String emptyText) {
        if (pickedPlayers.size() <= index) {
            return emptyText;
        }
        return "Player " + (index + 1) + ": " + pickedPlayers.get(index).selectionSummary("Human");
    }

    private void updateAutoRematchControls() {
        boolean isAiVsAi = pickedPlayers.size() == 2
                && pickedPlayers.get(0).playerType().isAI()
                && pickedPlayers.get(1).playerType().isAI();
        autoRematchesLabel.setManaged(isAiVsAi);
        autoRematchesLabel.setVisible(isAiVsAi);
        autoRematches.setManaged(isAiVsAi);
        autoRematches.setVisible(isAiVsAi);
        if (!isAiVsAi) {
            autoRematches.getValueFactory().setValue(0);
        }
    }

    private void startSelectedMatch() {
        if (pickedPlayers.size() < 2) {
            return;
        }

        stage.close();
        startGameHandler.startGame(
                pickedPlayers.get(0),
                pickedPlayers.get(1),
                automaticRematchCount());
    }

    private int automaticRematchCount() {
        if (pickedPlayers.size() < 2
                || !pickedPlayers.get(0).playerType().isAI()
                || !pickedPlayers.get(1).playerType().isAI()) {
            return 0;
        }

        try {
            int value = Integer.parseInt(autoRematches.getEditor().getText().trim());
            value = normalizeRematchCount(value);
            autoRematches.getValueFactory().setValue(value);
            return value;
        } catch (NumberFormatException e) {
            return normalizeRematchCount(autoRematches.getValue());
        }
    }

    private int normalizeRematchCount(int value) {
        int clampedValue = Math.max(0, Math.min(MAX_AUTO_REMATCHES, value));
        return Math.round((float) clampedValue / AUTO_REMATCH_INCREMENT) * AUTO_REMATCH_INCREMENT;
    }

    private void applyTheme() {
        content.setStyle(GuiTheme.rootStyle());
        categoryCards.forEach(card -> card.setStyle(GuiTheme.panelStyle()));
        currentSelectionPanel.setStyle(GuiTheme.panelStyle());
        GuiTheme.styleWindowTitle(title, 46);
        sectionLabels.forEach(label -> GuiTheme.styleWindowTitle(label, 18));
        mutedLabels.forEach(GuiTheme::styleMutedLabel);
        GuiTheme.styleMutedLabel(firstSelectionLabel);
        GuiTheme.styleMutedLabel(secondSelectionLabel);
        GuiTheme.styleTextField(humanName);
        comboBoxes.forEach(GuiTheme::styleComboBox);
        segmentButtons.forEach(GuiTheme::styleSegmentButton);
        GuiTheme.styleSpinner(autoRematches);
        pickButtons.forEach(GuiTheme::styleCompactButton);
        GuiTheme.stylePrimaryButton(startButton);
        GuiTheme.styleThemeButton(themeButton);
    }

    @FunctionalInterface
    public interface StartGameHandler {
        void startGame(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile, int automaticRematches);
    }
}
