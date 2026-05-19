package GUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

final class ControlsPanel {
    private static final int CONTENT_HEIGHT = PlayersPanel.HEIGHT - GuiTheme.PANEL_TITLE_HEIGHT;
    private static final int CONTENT_PADDING = 9;
    private static final int CONTROL_GAP = 8;
    private static final int STACK_HEIGHT = CONTENT_HEIGHT - CONTENT_PADDING * 2;
    private static final int STACK_BUTTON_HEIGHT = (STACK_HEIGHT - CONTROL_GAP) / 2;
    private static final int SQUARE_BUTTON_SIZE = STACK_HEIGHT;
    private static final int STACK_BUTTON_WIDTH =
            (GameBoardPanel.FRAME_SIZE - CONTENT_PADDING * 2 - CONTROL_GAP * 3 - SQUARE_BUTTON_SIZE * 2) / 2;

    private final Runnable undoHandler;
    private final Runnable newGameHandler;
    private final Runnable rematchHandler;
    private final Runnable exitHandler;
    private final Consumer<Boolean> fastMoveDelayHandler;
    private final Runnable themeChangedHandler;
    private final VBox view;
    private final Label titleLabel;
    private final Button undoButton;
    private final Button newGameButton;
    private final Button rematchButton;
    private final Button exitButton;
    private final MenuButton themeButton;
    private final ToggleButton fastMoveDelaySwitch;
    private final HBox controlsRow;
    private final VBox themeControls;
    private final VBox matchControls;
    private final Label fastModeLabel;
    private final Label slowModeLabel;
    private final StackPane fastMoveDelayTrack;
    private final Circle fastMoveDelayThumb;

    ControlsPanel(
            Runnable undoHandler,
            Runnable newGameHandler,
            Runnable rematchHandler,
            Runnable exitHandler,
            Consumer<Boolean> fastMoveDelayHandler,
            Runnable themeChangedHandler) {
        this.undoHandler = undoHandler;
        this.newGameHandler = newGameHandler;
        this.rematchHandler = rematchHandler;
        this.exitHandler = exitHandler;
        this.fastMoveDelayHandler = fastMoveDelayHandler;
        this.themeChangedHandler = themeChangedHandler;
        this.titleLabel = new Label("Menu");
        this.undoButton = createUndoButton();
        this.newGameButton = createNewGameButton();
        this.rematchButton = createRematchButton();
        this.exitButton = createExitButton();
        this.themeButton = GuiTheme.createThemeMenu(this::handleThemeChanged, false);
        themeButton.setAlignment(Pos.CENTER);
        themeButton.setContentDisplay(ContentDisplay.CENTER);
        setFixedWidth(themeButton, STACK_BUTTON_WIDTH);
        this.fastModeLabel = new Label("Fast");
        this.slowModeLabel = new Label("Slow");
        this.fastMoveDelayThumb = new Circle(8);
        this.fastMoveDelayTrack = new StackPane(fastMoveDelayThumb);
        this.fastMoveDelaySwitch = createFastMoveDelaySwitch();
        this.themeControls = createControlStack(themeButton, fastMoveDelaySwitch);
        this.matchControls = createControlStack(newGameButton, rematchButton);

        this.controlsRow = new HBox(
                CONTROL_GAP,
                themeControls,
                undoButton,
                matchControls,
                exitButton);
        controlsRow.setAlignment(Pos.CENTER);
        controlsRow.setPadding(new Insets(CONTENT_PADDING));
        controlsRow.setMinHeight(contentHeight());
        controlsRow.setPrefHeight(contentHeight());
        controlsRow.setMaxHeight(contentHeight());

        view = new VBox(titleLabel, controlsRow);
        view.setAlignment(Pos.CENTER);
        view.setMinWidth(GameBoardPanel.FRAME_SIZE);
        view.setPrefWidth(GameBoardPanel.FRAME_SIZE);
        view.setMaxWidth(GameBoardPanel.FRAME_SIZE);
        view.setMinHeight(PlayersPanel.HEIGHT);
        view.setPrefHeight(PlayersPanel.HEIGHT);
        view.setMaxHeight(PlayersPanel.HEIGHT);
        applyTheme();
    }

    VBox view() {
        return view;
    }

    void setUndoAvailable(boolean undoAvailable) {
        undoButton.setDisable(!undoAvailable);
    }

    void applyTheme() {
        view.setStyle(GuiTheme.panelStyle());
        GuiTheme.stylePanelTitle(titleLabel);
        GuiTheme.styleToolbarUndoButton(undoButton);
        GuiTheme.styleToolbarButton(newGameButton);
        GuiTheme.styleToolbarButton(rematchButton);
        GuiTheme.styleToolbarDangerButton(exitButton);
        GuiTheme.styleToolbarThemeButton(themeButton);
        GuiTheme.styleToolbarSpeedSwitch(
                fastMoveDelaySwitch,
                fastModeLabel,
                slowModeLabel,
                fastMoveDelayTrack,
                fastMoveDelayThumb);
        controlsRow.setMinHeight(contentHeight());
        controlsRow.setPrefHeight(contentHeight());
        controlsRow.setMaxHeight(contentHeight());
        setFixedStack(themeControls);
        setFixedStack(matchControls);
        setFixedHeight(undoButton, SQUARE_BUTTON_SIZE);
        setFixedHeight(newGameButton, controlHeight());
        setFixedHeight(rematchButton, controlHeight());
        setFixedHeight(exitButton, SQUARE_BUTTON_SIZE);
        setFixedHeight(themeButton, controlHeight());
        setFixedHeight(fastMoveDelaySwitch, controlHeight());
    }

    private Button createUndoButton() {
        Button button = new Button("Undo");
        button.setDisable(true);
        button.setOnAction(event -> undoHandler.run());
        setFixedWidth(button, SQUARE_BUTTON_SIZE);
        return button;
    }

    private Button createNewGameButton() {
        Button button = new Button("New Game");
        button.setOnAction(event -> newGameHandler.run());
        setFixedWidth(button, STACK_BUTTON_WIDTH);
        return button;
    }

    private Button createRematchButton() {
        Button button = new Button("Rematch");
        button.setOnAction(event -> rematchHandler.run());
        setFixedWidth(button, STACK_BUTTON_WIDTH);
        return button;
    }

    private Button createExitButton() {
        Button button = new Button("Exit");
        button.setOnAction(event -> exitHandler.run());
        setFixedWidth(button, SQUARE_BUTTON_SIZE);
        return button;
    }

    private ToggleButton createFastMoveDelaySwitch() {
        ToggleButton button = new ToggleButton();
        button.setSelected(false);
        button.setGraphic(createFastMoveDelaySwitchGraphic());
        button.setAccessibleText("Fast");
        button.setOnAction(event -> updateFastMoveDelaySwitch());
        setFixedWidth(button, STACK_BUTTON_WIDTH);
        return button;
    }

    private HBox createFastMoveDelaySwitchGraphic() {
        HBox switchGraphic = new HBox(5, fastModeLabel, fastMoveDelayTrack, slowModeLabel);
        switchGraphic.setAlignment(Pos.CENTER);
        switchGraphic.setMouseTransparent(true);
        return switchGraphic;
    }

    private VBox createControlStack(javafx.scene.Node topControl, javafx.scene.Node bottomControl) {
        VBox stack = new VBox(CONTROL_GAP, topControl, bottomControl);
        stack.setAlignment(Pos.CENTER);
        setFixedStack(stack);
        return stack;
    }

    private void updateFastMoveDelaySwitch() {
        boolean slowMode = fastMoveDelaySwitch.isSelected();
        fastMoveDelaySwitch.setAccessibleText(slowMode ? "Slow" : "Fast");
        GuiTheme.styleToolbarSpeedSwitch(
                fastMoveDelaySwitch,
                fastModeLabel,
                slowModeLabel,
                fastMoveDelayTrack,
                fastMoveDelayThumb);
        fastMoveDelayHandler.accept(slowMode);
    }

    private void handleThemeChanged() {
        applyTheme();
        themeChangedHandler.run();
    }

    private void setFixedWidth(Button button, int width) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
    }

    private void setFixedWidth(MenuButton button, int width) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
    }

    private void setFixedWidth(ToggleButton button, int width) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
    }

    private void setFixedHeight(Button button, int height) {
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
    }

    private void setFixedHeight(MenuButton button, int height) {
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
    }

    private void setFixedHeight(ToggleButton button, int height) {
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
    }

    private void setFixedStack(VBox stack) {
        stack.setMinSize(STACK_BUTTON_WIDTH, STACK_HEIGHT);
        stack.setPrefSize(STACK_BUTTON_WIDTH, STACK_HEIGHT);
        stack.setMaxSize(STACK_BUTTON_WIDTH, STACK_HEIGHT);
    }

    private int contentHeight() {
        return CONTENT_HEIGHT;
    }

    private int controlHeight() {
        return STACK_BUTTON_HEIGHT;
    }
}
