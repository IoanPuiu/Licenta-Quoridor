package GUI;

import SlowModel.PlayerProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

final class PlayersPanel {
    private static final int INITIAL_WALLS = 10;
    static final int HEIGHT = 148;
    private static final int CONTENT_HEIGHT = HEIGHT - GuiTheme.PANEL_TITLE_HEIGHT;
    private static final int CONTENT_PADDING = GuiTheme.PANEL_CONTENT_PADDING;
    private static final int CARD_GAP = GuiTheme.PANEL_ELEMENT_GAP;
    private static final int CARD_WIDTH = (StatsPanel.WIDTH - CONTENT_PADDING * 2 - CARD_GAP) / 2;
    private static final int CARD_HEIGHT = CONTENT_HEIGHT - CONTENT_PADDING * 2;

    private final VBox view;
    private final Label titleLabel;
    private final HBox cardsRow;
    private final VBox firstPlayerCard;
    private final VBox secondPlayerCard;
    private final Label firstPlayerTitleLabel;
    private final Label secondPlayerTitleLabel;
    private final Label firstPlayerSettingsLabel;
    private final Label secondPlayerSettingsLabel;
    private final Circle firstPlayerMarker;
    private final Circle secondPlayerMarker;
    private final Label firstPlayerWallsLabel;
    private final Label secondPlayerWallsLabel;
    private boolean isFirstPlayerTurn;
    private boolean isFirstPlayerWinner;
    private boolean isGameOver;

    PlayersPanel(
            PlayerProfile firstPlayerProfile,
            PlayerProfile secondPlayerProfile,
            boolean isFirstPlayerStarting) {
        this.isFirstPlayerTurn = isFirstPlayerStarting;
        this.isGameOver = false;
        this.titleLabel = new Label("Players");
        this.firstPlayerMarker = new Circle(6, GuiTheme.playerOne());
        this.secondPlayerMarker = new Circle(6, GuiTheme.playerTwo());
        this.firstPlayerTitleLabel = createPlayerTitleLabel(
                firstPlayerProfile.cardTitle("First Player"));
        this.secondPlayerTitleLabel = createPlayerTitleLabel(
                secondPlayerProfile.cardTitle("Second Player"));
        this.firstPlayerSettingsLabel = createSettingsLabel(
                firstPlayerProfile.cardSettingsSummary("First Player"),
                firstPlayerProfile.cardSettingsDetails("First Player"));
        this.secondPlayerSettingsLabel = createSettingsLabel(
                secondPlayerProfile.cardSettingsSummary("Second Player"),
                secondPlayerProfile.cardSettingsDetails("Second Player"));
        this.firstPlayerWallsLabel = createWallsLabel(GuiTheme.playerOne());
        this.secondPlayerWallsLabel = createWallsLabel(GuiTheme.playerTwo());
        this.firstPlayerCard = createPlayerCard(
                firstPlayerMarker,
                firstPlayerTitleLabel,
                firstPlayerSettingsLabel,
                firstPlayerWallsLabel,
                GuiTheme.playerOne());
        this.secondPlayerCard = createPlayerCard(
                secondPlayerMarker,
                secondPlayerTitleLabel,
                secondPlayerSettingsLabel,
                secondPlayerWallsLabel,
                GuiTheme.playerTwo());

        cardsRow = new HBox(CARD_GAP, firstPlayerCard, secondPlayerCard);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(CONTENT_PADDING));
        cardsRow.setMinHeight(CONTENT_HEIGHT);
        cardsRow.setPrefHeight(CONTENT_HEIGHT);
        cardsRow.setMaxHeight(CONTENT_HEIGHT);

        view = new VBox(titleLabel, cardsRow);
        view.setAlignment(Pos.TOP_CENTER);
        view.setMinWidth(StatsPanel.WIDTH);
        view.setPrefWidth(StatsPanel.WIDTH);
        view.setMaxWidth(StatsPanel.WIDTH);
        view.setMinHeight(HEIGHT);
        view.setPrefHeight(HEIGHT);
        view.setMaxHeight(HEIGHT);
        applyTheme();
    }

    VBox view() {
        return view;
    }

    void updateWalls(int firstPlayerWalls, int secondPlayerWalls) {
        firstPlayerWallsLabel.setText(firstPlayerWalls + " walls");
        secondPlayerWallsLabel.setText(secondPlayerWalls + " walls");
    }

    void updateWall(boolean isFirstPlayer, int wallsLeft) {
        if (isFirstPlayer) {
            firstPlayerWallsLabel.setText(wallsLeft + " walls");
        } else {
            secondPlayerWallsLabel.setText(wallsLeft + " walls");
        }
    }

    void updateTurnState(boolean isFirstPlayerTurn, boolean isGameOver, boolean isFirstPlayerWinner) {
        this.isFirstPlayerTurn = isFirstPlayerTurn;
        this.isGameOver = isGameOver;
        this.isFirstPlayerWinner = isFirstPlayerWinner;
        updateTurnIndicators();
    }

    void applyTheme() {
        view.setStyle(GuiTheme.panelStyle());
        GuiTheme.stylePanelTitle(titleLabel);
        cardsRow.setMinHeight(CONTENT_HEIGHT);
        cardsRow.setPrefHeight(CONTENT_HEIGHT);
        cardsRow.setMaxHeight(CONTENT_HEIGHT);
        firstPlayerWallsLabel.setTextFill(GuiTheme.playerOne());
        secondPlayerWallsLabel.setTextFill(GuiTheme.playerTwo());
        firstPlayerTitleLabel.setTextFill(GuiTheme.text());
        secondPlayerTitleLabel.setTextFill(GuiTheme.text());
        firstPlayerSettingsLabel.setTextFill(GuiTheme.mutedText());
        secondPlayerSettingsLabel.setTextFill(GuiTheme.mutedText());
        firstPlayerMarker.setFill(GuiTheme.playerOne());
        secondPlayerMarker.setFill(GuiTheme.playerTwo());
        updateTurnIndicators();
    }

    private Label createWallsLabel(Color color) {
        Label label = new Label(INITIAL_WALLS + " walls");
        label.setTextFill(color);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 16));
        return label;
    }

    private Label createPlayerTitleLabel(String playerName) {
        Label titleLabel = new Label(playerName);
        titleLabel.setTextFill(GuiTheme.text());
        titleLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 12));
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(112);
        return titleLabel;
    }

    private Label createSettingsLabel(String summary, String details) {
        Label label = new Label(summary);
        label.setTextFill(GuiTheme.mutedText());
        label.setFont(Font.font("Arial", FontWeight.BOLD, 10.5));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.LEFT);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(CARD_WIDTH - 16);
        label.setTooltip(new Tooltip(details));
        return label;
    }

    private VBox createPlayerCard(
            Circle marker,
            Label titleLabel,
            Label settingsLabel,
            Label wallsLabel,
            Color accentColor) {
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(2);

        HBox titleRow = new HBox(9, marker, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(4, titleRow, settingsLabel, wallsLabel);
        card.setPadding(new Insets(7, 8, 7, 8));
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.setStyle(GuiTheme.playerCardStyle(accentColor));
        return card;
    }

    private void updateTurnIndicators() {
        firstPlayerCard.setStyle(isFirstPlayerHighlighted()
                ? GuiTheme.activePlayerCardStyle(GuiTheme.playerOne())
                : GuiTheme.playerCardStyle(GuiTheme.playerOne()));
        secondPlayerCard.setStyle(isSecondPlayerHighlighted()
                ? GuiTheme.activePlayerCardStyle(GuiTheme.playerTwo())
                : GuiTheme.playerCardStyle(GuiTheme.playerTwo()));
    }

    private boolean isFirstPlayerHighlighted() {
        return isGameOver ? isFirstPlayerWinner : isFirstPlayerTurn;
    }

    private boolean isSecondPlayerHighlighted() {
        return isGameOver ? !isFirstPlayerWinner : !isFirstPlayerTurn;
    }

}
