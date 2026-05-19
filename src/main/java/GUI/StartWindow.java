package GUI;

import AI.MCTS.MctsRolloutHeuristic;
import AI.MCTS.MctsSelectionHeuristic;
import AI.MiniMax.MoveOrdering;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import SlowModel.PlayerProfile;
import SlowModel.PlayerProfile.MctsVariant;

import java.util.ArrayList;
import java.util.List;

public class StartWindow {

    private static final int SCENE_WIDTH = 1120;
    private static final int SCENE_HEIGHT = 760;
    private static final int MAX_AUTO_REMATCHES = 100;
    private static final int AUTO_REMATCH_INCREMENT = 10;
    private static final int MCTS_EASY_DEPTH = 8_000;
    private static final int MCTS_MEDIUM_DEPTH = 16_000;
    private static final int MCTS_HARD_DEPTH = 32_000;
    private static final int MCTS_EXTREME_DEPTH = 64_000;
    private static final int MCTS_SHORT_ROLLOUT_MOVE_LIMIT = 16;
    private static final int MCTS_LONG_ROLLOUT_MOVE_LIMIT = 64;

    private final Stage stage;
    private final StartGameHandler startGameHandler;
    private final List<PlayerProfile> pickedPlayers = new ArrayList<>();
    private final List<VBox> categoryCards = new ArrayList<>();
    private final List<Label> sectionLabels = new ArrayList<>();
    private final List<Label> mutedLabels = new ArrayList<>();
    private final List<Button> pickButtons = new ArrayList<>();
    private final List<ToggleButton> segmentButtons = new ArrayList<>();

    private VBox content;
    private HBox setupPanel;
    private VBox currentSelectionPanel;
    private Label title;
    private Label currentSelectionTitle;
    private Label firstSelectionLabel;
    private Label secondSelectionLabel;
    private Label autoRematchesLabel;
    private TextField humanName;
    private ToggleGroup minimaxDepthGroup;
    private HBox minimaxDepthSegments;
    private ToggleGroup minimaxMoveOrderingGroup;
    private HBox minimaxMoveOrderingSegments;
    private ToggleGroup mctsDepthGroup;
    private HBox mctsDepthSegments;
    private ToggleGroup mctsVariantGroup;
    private HBox mctsVariantSegments;
    private ToggleGroup mctsSelectionHeuristicGroup;
    private HBox mctsSelectionHeuristicSegments;
    private ToggleGroup mctsRolloutHeuristicGroup;
    private HBox mctsRolloutHeuristicSegments;
    private ToggleGroup mctsRolloutMoveLimitGroup;
    private HBox mctsRolloutMoveLimitSegments;
    private VBox mctsPerformanceOptions;
    private Spinner<Integer> autoRematches;
    private Button humanPickButton;
    private Button minimaxPickButton;
    private Button mctsPickButton;
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
        minimaxDepthGroup = new ToggleGroup();
        minimaxDepthSegments = createOptionSegments(
                minimaxDepthGroup,
                Integer.valueOf(2),
                new Option<>(2, "D2", "MiniMax search depth 2."),
                new Option<>(3, "D3", "MiniMax search depth 3."));
        minimaxMoveOrderingGroup = new ToggleGroup();
        minimaxMoveOrderingSegments = createMoveOrderingSegments();
        mctsDepthGroup = new ToggleGroup();
        mctsDepthSegments = createOptionSegments(
                mctsDepthGroup,
                Integer.valueOf(MCTS_MEDIUM_DEPTH),
                new Option<>(MCTS_EASY_DEPTH, "8K", "8,000 MCTS iterations."),
                new Option<>(MCTS_MEDIUM_DEPTH, "16K", "16,000 MCTS iterations."),
                new Option<>(MCTS_HARD_DEPTH, "32K", "32,000 MCTS iterations."),
                new Option<>(MCTS_EXTREME_DEPTH, "64K", "64,000 MCTS iterations."));
        mctsVariantGroup = new ToggleGroup();
        mctsVariantSegments = createMctsVariantSegments();
        mctsSelectionHeuristicGroup = new ToggleGroup();
        mctsSelectionHeuristicSegments = createOptionSegments(
                mctsSelectionHeuristicGroup,
                PlayerProfile.DEFAULT_MCTS_SELECTION_HEURISTIC,
                new Option<>(
                        MctsSelectionHeuristic.WALLS_NEAR_PAWNS,
                        "Pawns",
                        MctsSelectionHeuristic.WALLS_NEAR_PAWNS.description()),
                new Option<>(
                        MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES,
                        "Pawns + walls + edges",
                        MctsSelectionHeuristic.WALLS_NEAR_PAWNS_EXISTING_WALLS_AND_EDGES.description()));
        mctsRolloutHeuristicGroup = new ToggleGroup();
        mctsRolloutHeuristicSegments = createOptionSegments(
                mctsRolloutHeuristicGroup,
                PlayerProfile.DEFAULT_MCTS_ROLLOUT_HEURISTIC,
                new Option<>(
                        MctsRolloutHeuristic.PAWN_MOVES,
                        "Pawns",
                        MctsRolloutHeuristic.PAWN_MOVES.description()),
                new Option<>(
                        MctsRolloutHeuristic.PAWN_MOVES_RANDOM_WALLS,
                        "Random walls",
                        MctsRolloutHeuristic.PAWN_MOVES_RANDOM_WALLS.description()),
                new Option<>(
                        MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS,
                        "Relevant walls",
                        MctsRolloutHeuristic.PAWN_MOVES_RELEVANT_WALLS.description()));
        mctsRolloutMoveLimitGroup = new ToggleGroup();
        mctsRolloutMoveLimitSegments = createOptionSegments(
                mctsRolloutMoveLimitGroup,
                Integer.valueOf(PlayerProfile.DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT),
                new Option<>(MCTS_SHORT_ROLLOUT_MOVE_LIMIT, "16", "Stop rollout after 16 moves."),
                new Option<>(PlayerProfile.DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT, "32", "Stop rollout after 32 moves."),
                new Option<>(MCTS_LONG_ROLLOUT_MOVE_LIMIT, "64", "Stop rollout after 64 moves."));
        autoRematches = createAutoRematchesSpinner();

        humanPickButton = createPickButton("Pick");
        minimaxPickButton = createPickButton("Pick");
        mctsPickButton = createPickButton("Pick");
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
                selectedMinimaxDepth(),
                selectedMoveOrdering())));
        mctsPickButton.setOnAction(event -> pickPlayer(PlayerProfile.mcts(
                selectedMctsDepth(),
                selectedMctsVariant(),
                selectedMctsSelectionHeuristic(),
                selectedMctsRolloutHeuristic(),
                selectedMctsRolloutMoveLimit())));
        gymPickButton.setOnAction(event -> pickPlayer(PlayerProfile.gymPython()));
        startButton.setOnAction(event -> startSelectedMatch());

        title = new Label("Quoridor");
        GuiTheme.styleWindowTitle(title, 46);

        setupPanel = createSetupPanel();
        setupPanel.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(setupPanel, Priority.ALWAYS);

        currentSelectionPanel = createCurrentSelectionPanel();
        currentSelectionPanel.setPrefWidth(220);
        currentSelectionPanel.setMaxWidth(220);

        HBox body = new HBox(18, setupPanel, currentSelectionPanel);
        body.setAlignment(Pos.TOP_CENTER);

        content = new VBox(26, title, body);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(34));
        content.setStyle(GuiTheme.rootStyle());

        updateSelectionPanel();
        return new Scene(content, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private HBox createSetupPanel() {
        VBox sideOptions = new VBox(14, createHumanCard(), createMinimaxCard(), createGymCard());
        sideOptions.setPrefWidth(316);
        sideOptions.setMaxWidth(326);

        VBox mctsCard = createMctsCard();
        HBox.setHgrow(mctsCard, Priority.ALWAYS);

        HBox panel = new HBox(14, sideOptions, mctsCard);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private VBox createHumanCard() {
        return createCategoryCard(
                "Human",
                142,
                createFieldLabel("Name"),
                humanName,
                humanPickButton);
    }

    private VBox createMinimaxCard() {
        return createCategoryCard(
                "Minimax",
                188,
                createFieldLabel("Depth"),
                minimaxDepthSegments,
                createFieldLabel("Move ordering"),
                minimaxMoveOrderingSegments,
                minimaxPickButton);
    }

    private VBox createMctsCard() {
        mctsPerformanceOptions = createMctsPerformanceOptions();
        updateMctsPerformanceOptions();

        return createCategoryCard(
                "MCTS",
                470,
                createFieldLabel("Depth"),
                mctsDepthSegments,
                createFieldLabel("Variant"),
                mctsVariantSegments,
                mctsPerformanceOptions,
                createFieldLabel("Rollout Limit"),
                mctsRolloutMoveLimitSegments,
                mctsPickButton);
    }

    private VBox createGymCard() {
        return createCategoryCard(
                "Gym",
                112,
                createFieldLabel("No options yet"),
                gymPickButton);
    }

    private VBox createCategoryCard(String heading, double minHeight, Node... nodes) {
        Label titleLabel = new Label(heading);
        sectionLabels.add(titleLabel);
        GuiTheme.styleWindowTitle(titleLabel, 18);

        VBox card = new VBox(10);
        card.getChildren().add(titleLabel);
        card.getChildren().addAll(nodes);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setMinHeight(minHeight);
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

    @SafeVarargs
    private <T> HBox createOptionSegments(ToggleGroup group, T defaultValue, Option<T>... options) {
        HBox segments = new HBox(8);
        segments.setMaxWidth(Double.MAX_VALUE);

        ToggleButton defaultButton = null;
        for (Option<T> option : options) {
            ToggleButton button = createOptionButton(option.label(), option.tooltip(), option.value(), group);
            HBox.setHgrow(button, Priority.ALWAYS);
            segments.getChildren().add(button);

            if (option.value().equals(defaultValue)) {
                defaultButton = button;
            }
        }

        ToggleButton selectedDefaultButton = defaultButton == null
                ? (ToggleButton) segments.getChildren().get(0)
                : defaultButton;
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
        selectedDefaultButton.setSelected(true);
        return segments;
    }

    private ToggleButton createOptionButton(
            String text,
            String tooltipText,
            Object value,
            ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(value);
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(true);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setAlignment(Pos.CENTER);
        if (tooltipText != null && !tooltipText.isBlank()) {
            button.setTooltip(new Tooltip(tooltipText));
        }
        button.selectedProperty().addListener((obs, oldValue, newValue) ->
                GuiTheme.styleSegmentButton(button));
        GuiTheme.styleSegmentButton(button);
        segmentButtons.add(button);
        return button;
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

    private int selectedMinimaxDepth() {
        return selectedIntegerValue(minimaxDepthGroup, 2);
    }

    private HBox createMctsVariantSegments() {
        HBox segments = createOptionSegments(
                mctsVariantGroup,
                MctsVariant.V0,
                new Option<>(MctsVariant.V0, "V0", "Baseline MCTS with the original tree policy."),
                new Option<>(MctsVariant.PERFORMANCE, "Performance", "Optimized MCTS with progressive widening."));

        mctsVariantGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
            updateMctsPerformanceOptions();
        });
        return segments;
    }

    private VBox createMctsPerformanceOptions() {
        VBox options = new VBox(
                8,
                createFieldLabel("Selection Heuristic"),
                mctsSelectionHeuristicSegments,
                createFieldLabel("Rollout Heuristic"),
                mctsRolloutHeuristicSegments);
        options.setPadding(new Insets(4, 0, 2, 0));
        options.setMaxWidth(Double.MAX_VALUE);
        return options;
    }

    private void updateMctsPerformanceOptions() {
        if (mctsPerformanceOptions == null) {
            return;
        }

        boolean isPerformance = selectedMctsVariant() == MctsVariant.PERFORMANCE;
        mctsPerformanceOptions.setManaged(isPerformance);
        mctsPerformanceOptions.setVisible(isPerformance);
    }

    private MctsVariant selectedMctsVariant() {
        Toggle selectedToggle = mctsVariantGroup.getSelectedToggle();
        if (selectedToggle == null || !(selectedToggle.getUserData() instanceof MctsVariant variant)) {
            return MctsVariant.V0;
        }
        return variant;
    }

    private int selectedMctsDepth() {
        return selectedIntegerValue(mctsDepthGroup, MCTS_MEDIUM_DEPTH);
    }

    private MctsSelectionHeuristic selectedMctsSelectionHeuristic() {
        Toggle selectedToggle = mctsSelectionHeuristicGroup.getSelectedToggle();
        if (selectedToggle == null
                || !(selectedToggle.getUserData() instanceof MctsSelectionHeuristic heuristic)) {
            return PlayerProfile.DEFAULT_MCTS_SELECTION_HEURISTIC;
        }

        return heuristic;
    }

    private MctsRolloutHeuristic selectedMctsRolloutHeuristic() {
        Toggle selectedToggle = mctsRolloutHeuristicGroup.getSelectedToggle();
        if (selectedToggle == null || !(selectedToggle.getUserData() instanceof MctsRolloutHeuristic heuristic)) {
            return PlayerProfile.DEFAULT_MCTS_ROLLOUT_HEURISTIC;
        }

        return heuristic;
    }

    private int selectedMctsRolloutMoveLimit() {
        return selectedIntegerValue(
                mctsRolloutMoveLimitGroup,
                PlayerProfile.DEFAULT_MCTS_ROLLOUT_MOVE_LIMIT);
    }

    private int selectedIntegerValue(ToggleGroup group, int defaultValue) {
        Toggle selectedToggle = group.getSelectedToggle();
        if (selectedToggle == null || !(selectedToggle.getUserData() instanceof Integer value)) {
            return defaultValue;
        }

        return value;
    }

    private Spinner<Integer> createAutoRematchesSpinner() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                MAX_AUTO_REMATCHES,
                0,
                AUTO_REMATCH_INCREMENT));
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
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
        segmentButtons.forEach(GuiTheme::styleSegmentButton);
        GuiTheme.styleSpinner(autoRematches);
        pickButtons.forEach(GuiTheme::styleCompactButton);
        GuiTheme.stylePrimaryButton(startButton);
        GuiTheme.styleThemeButton(themeButton);
        updateMctsPerformanceOptions();
    }

    private record Option<T>(T value, String label, String tooltip) {
    }

    @FunctionalInterface
    public interface StartGameHandler {
        void startGame(PlayerProfile firstPlayerProfile, PlayerProfile secondPlayerProfile, int automaticRematches);
    }
}
