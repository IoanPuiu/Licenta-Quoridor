package GUI;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Locale;

final class StatsPanel {
    static final int WIDTH = 360;

    private static final int ROW_COUNT = 8;
    private static final int CONTENT_HEIGHT = GameBoardPanel.FRAME_SIZE - GuiTheme.PANEL_TITLE_HEIGHT;
    private static final double CONTENT_SIDE_PADDING = GuiTheme.PANEL_CONTENT_PADDING;
    private static final double CONTENT_TOP_PADDING = GuiTheme.PANEL_CONTENT_PADDING;
    private static final double CONTENT_BOTTOM_PADDING = GuiTheme.PANEL_CONTENT_PADDING + 6;
    private static final double CELL_GAP = GuiTheme.PANEL_ELEMENT_GAP;
    private static final double GRID_WIDTH = WIDTH - CONTENT_SIDE_PADDING * 2;
    private static final double GRID_HEIGHT = CONTENT_HEIGHT - CONTENT_TOP_PADDING - CONTENT_BOTTOM_PADDING;
    private static final double METRIC_COLUMN_WIDTH = 144;
    private static final double PLAYER_COLUMN_WIDTH =
            (GRID_WIDTH - CELL_GAP * 2 - METRIC_COLUMN_WIDTH) / 2;
    private static final double ROW_HEIGHT =
            (GRID_HEIGHT - CELL_GAP * (ROW_COUNT - 1)) / ROW_COUNT;

    private final boolean scoreFirstPlayerUsesFirstColor;
    private final boolean scoreSecondPlayerUsesFirstColor;
    private final VBox view;
    private final Label titleLabel;
    private final Label statsMetricHeaderLabel;
    private final Label scoreMetricLabel;
    private final Label firstRoleWinRateMetricLabel;
    private final Label secondRoleWinRateMetricLabel;
    private final Label averageMoveMetricLabel;
    private final Label longestMoveMetricLabel;
    private final Label averageGameThinkingMetricLabel;
    private final Label averageWallImpactMetricLabel;
    private final Label firstPlayerPawnHeaderLabel;
    private final Label secondPlayerPawnHeaderLabel;
    private final Label firstPlayerScoreValueLabel;
    private final Label secondPlayerScoreValueLabel;
    private final Label firstPlayerFirstRoleWinRateValueLabel;
    private final Label secondPlayerFirstRoleWinRateValueLabel;
    private final Label firstPlayerSecondRoleWinRateValueLabel;
    private final Label secondPlayerSecondRoleWinRateValueLabel;
    private final Label firstPlayerAverageMoveValueLabel;
    private final Label secondPlayerAverageMoveValueLabel;
    private final Label firstPlayerLongestMoveValueLabel;
    private final Label secondPlayerLongestMoveValueLabel;
    private final Label firstPlayerAverageGameThinkingValueLabel;
    private final Label secondPlayerAverageGameThinkingValueLabel;
    private final Label firstPlayerAverageWallImpactValueLabel;
    private final Label secondPlayerAverageWallImpactValueLabel;

    StatsPanel(
            boolean scoreFirstPlayerUsesFirstColor,
            boolean scoreSecondPlayerUsesFirstColor,
            int scoreFirstPlayerWins,
            int scoreSecondPlayerWins,
            boolean showScore) {
        this.scoreFirstPlayerUsesFirstColor = scoreFirstPlayerUsesFirstColor;
        this.scoreSecondPlayerUsesFirstColor = scoreSecondPlayerUsesFirstColor;
        this.titleLabel = new Label("Stats");
        this.statsMetricHeaderLabel = createStatsMetricLabel("Statistic");
        this.scoreMetricLabel = createStatsMetricLabel("Score");
        this.firstRoleWinRateMetricLabel = createStatsMetricLabel("Win rate first");
        this.secondRoleWinRateMetricLabel = createStatsMetricLabel("Win rate second");
        this.averageMoveMetricLabel = createStatsMetricLabel("Avg move");
        this.longestMoveMetricLabel = createStatsMetricLabel("Longest move");
        this.averageGameThinkingMetricLabel = createStatsMetricLabel("Avg total/game");
        this.averageWallImpactMetricLabel = createStatsMetricLabel("Avg wall impact");
        this.firstPlayerPawnHeaderLabel = createPawnHeaderLabel(scoreFirstPlayerColor());
        this.secondPlayerPawnHeaderLabel = createPawnHeaderLabel(scoreSecondPlayerColor());
        this.firstPlayerScoreValueLabel = createScoreValueLabel(scoreFirstPlayerColor());
        this.secondPlayerScoreValueLabel = createScoreValueLabel(scoreSecondPlayerColor());
        this.firstPlayerFirstRoleWinRateValueLabel = createStatsValueLabel();
        this.secondPlayerFirstRoleWinRateValueLabel = createStatsValueLabel();
        this.firstPlayerSecondRoleWinRateValueLabel = createStatsValueLabel();
        this.secondPlayerSecondRoleWinRateValueLabel = createStatsValueLabel();
        this.firstPlayerAverageMoveValueLabel = createStatsValueLabel();
        this.secondPlayerAverageMoveValueLabel = createStatsValueLabel();
        this.firstPlayerLongestMoveValueLabel = createStatsValueLabel();
        this.secondPlayerLongestMoveValueLabel = createStatsValueLabel();
        this.firstPlayerAverageGameThinkingValueLabel = createStatsValueLabel();
        this.secondPlayerAverageGameThinkingValueLabel = createStatsValueLabel();
        this.firstPlayerAverageWallImpactValueLabel = createStatsValueLabel();
        this.secondPlayerAverageWallImpactValueLabel = createStatsValueLabel();

        StackPane statsContent = createStatsContent(createStatsGrid());
        view = new VBox(titleLabel, statsContent);
        view.setAlignment(Pos.TOP_CENTER);
        view.setMinSize(WIDTH, GameBoardPanel.FRAME_SIZE);
        view.setPrefSize(WIDTH, GameBoardPanel.FRAME_SIZE);
        view.setMaxSize(WIDTH, GameBoardPanel.FRAME_SIZE);

        updateScore(scoreFirstPlayerWins, scoreSecondPlayerWins, showScore);
        updateThinkingTime(
                0,
                0,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                0,
                0,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN);
        applyTheme();
    }

    VBox view() {
        return view;
    }

    void updateScore(int scoreFirstPlayerWins, int scoreSecondPlayerWins, boolean showScore) {
        firstPlayerScoreValueLabel.setText(String.valueOf(scoreFirstPlayerWins));
        secondPlayerScoreValueLabel.setText(String.valueOf(scoreSecondPlayerWins));
        setStatsRowVisible(showScore, scoreMetricLabel, firstPlayerScoreValueLabel, secondPlayerScoreValueLabel);
    }

    void updateThinkingTime(
            long firstPlayerAverageNanos,
            long firstPlayerMaxNanos,
            double firstPlayerAverageGameThinkingNanos,
            double firstPlayerAverageWallImpact,
            double firstPlayerFirstRoleWinRate,
            double firstPlayerSecondRoleWinRate,
            long secondPlayerAverageNanos,
            long secondPlayerMaxNanos,
            double secondPlayerAverageGameThinkingNanos,
            double secondPlayerAverageWallImpact,
            double secondPlayerFirstRoleWinRate,
            double secondPlayerSecondRoleWinRate) {
        firstPlayerFirstRoleWinRateValueLabel.setText(formatWinRate(firstPlayerFirstRoleWinRate));
        firstPlayerSecondRoleWinRateValueLabel.setText(formatWinRate(firstPlayerSecondRoleWinRate));
        firstPlayerAverageMoveValueLabel.setText(formatThinkingTime(firstPlayerAverageNanos));
        firstPlayerLongestMoveValueLabel.setText(formatThinkingTime(firstPlayerMaxNanos));
        firstPlayerAverageGameThinkingValueLabel.setText(formatThinkingTime(firstPlayerAverageGameThinkingNanos));
        firstPlayerAverageWallImpactValueLabel.setText(formatImpact(firstPlayerAverageWallImpact));
        secondPlayerFirstRoleWinRateValueLabel.setText(formatWinRate(secondPlayerFirstRoleWinRate));
        secondPlayerSecondRoleWinRateValueLabel.setText(formatWinRate(secondPlayerSecondRoleWinRate));
        secondPlayerAverageMoveValueLabel.setText(formatThinkingTime(secondPlayerAverageNanos));
        secondPlayerLongestMoveValueLabel.setText(formatThinkingTime(secondPlayerMaxNanos));
        secondPlayerAverageGameThinkingValueLabel.setText(formatThinkingTime(secondPlayerAverageGameThinkingNanos));
        secondPlayerAverageWallImpactValueLabel.setText(formatImpact(secondPlayerAverageWallImpact));
    }

    void applyTheme() {
        view.setStyle(GuiTheme.scoreboardStyle());
        GuiTheme.stylePanelTitle(titleLabel);

        List.of(
                statsMetricHeaderLabel,
                scoreMetricLabel,
                firstRoleWinRateMetricLabel,
                secondRoleWinRateMetricLabel,
                averageMoveMetricLabel,
                longestMoveMetricLabel,
                averageGameThinkingMetricLabel,
                averageWallImpactMetricLabel).forEach(label -> {
            label.setTextFill(GuiTheme.mutedText());
            label.setStyle(GuiTheme.statsMetricStyle());
        });
        List.of(
                firstPlayerFirstRoleWinRateValueLabel,
                secondPlayerFirstRoleWinRateValueLabel,
                firstPlayerSecondRoleWinRateValueLabel,
                secondPlayerSecondRoleWinRateValueLabel,
                firstPlayerAverageMoveValueLabel,
                secondPlayerAverageMoveValueLabel,
                firstPlayerLongestMoveValueLabel,
                secondPlayerLongestMoveValueLabel,
                firstPlayerAverageGameThinkingValueLabel,
                secondPlayerAverageGameThinkingValueLabel,
                firstPlayerAverageWallImpactValueLabel,
                secondPlayerAverageWallImpactValueLabel).forEach(label -> {
            label.setTextFill(GuiTheme.text());
            label.setStyle(GuiTheme.statsValueStyle());
        });

        updatePawnHeaderLabel(firstPlayerPawnHeaderLabel, scoreFirstPlayerColor());
        updatePawnHeaderLabel(secondPlayerPawnHeaderLabel, scoreSecondPlayerColor());
        firstPlayerScoreValueLabel.setTextFill(scoreFirstPlayerColor());
        secondPlayerScoreValueLabel.setTextFill(scoreSecondPlayerColor());
        firstPlayerScoreValueLabel.setStyle(GuiTheme.statsValueStyle());
        secondPlayerScoreValueLabel.setStyle(GuiTheme.statsValueStyle());
    }

    private GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(CELL_GAP);
        grid.setHgap(CELL_GAP);
        grid.setPadding(Insets.EMPTY);
        grid.setAlignment(Pos.CENTER);
        grid.setMinSize(GRID_WIDTH, GRID_HEIGHT);
        grid.setPrefSize(GRID_WIDTH, GRID_HEIGHT);
        grid.setMaxSize(GRID_WIDTH, GRID_HEIGHT);
        grid.setClip(new Rectangle(GRID_WIDTH, GRID_HEIGHT));
        grid.getColumnConstraints().addAll(
                fixedColumn(METRIC_COLUMN_WIDTH),
                fixedColumn(PLAYER_COLUMN_WIDTH),
                fixedColumn(PLAYER_COLUMN_WIDTH));
        for (int row = 0; row < ROW_COUNT; row++) {
            grid.getRowConstraints().add(fixedRow());
        }

        addStatsCell(grid, statsMetricHeaderLabel, 0, 0, METRIC_COLUMN_WIDTH);
        addStatsCell(grid, firstPlayerPawnHeaderLabel, 1, 0, PLAYER_COLUMN_WIDTH);
        addStatsCell(grid, secondPlayerPawnHeaderLabel, 2, 0, PLAYER_COLUMN_WIDTH);
        addStatsRow(grid, 1, scoreMetricLabel, firstPlayerScoreValueLabel, secondPlayerScoreValueLabel);
        addStatsRow(grid, 2, averageMoveMetricLabel, firstPlayerAverageMoveValueLabel, secondPlayerAverageMoveValueLabel);
        addStatsRow(grid, 3, longestMoveMetricLabel, firstPlayerLongestMoveValueLabel, secondPlayerLongestMoveValueLabel);
        addStatsRow(
                grid,
                4,
                averageGameThinkingMetricLabel,
                firstPlayerAverageGameThinkingValueLabel,
                secondPlayerAverageGameThinkingValueLabel);
        addStatsRow(
                grid,
                5,
                averageWallImpactMetricLabel,
                firstPlayerAverageWallImpactValueLabel,
                secondPlayerAverageWallImpactValueLabel);
        addStatsRow(
                grid,
                6,
                firstRoleWinRateMetricLabel,
                firstPlayerFirstRoleWinRateValueLabel,
                secondPlayerFirstRoleWinRateValueLabel);
        addStatsRow(
                grid,
                7,
                secondRoleWinRateMetricLabel,
                firstPlayerSecondRoleWinRateValueLabel,
                secondPlayerSecondRoleWinRateValueLabel);
        return grid;
    }

    private StackPane createStatsContent(GridPane statsGrid) {
        StackPane content = new StackPane(statsGrid);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(
                CONTENT_TOP_PADDING,
                CONTENT_SIDE_PADDING,
                CONTENT_BOTTOM_PADDING,
                CONTENT_SIDE_PADDING));
        content.setMinSize(WIDTH, CONTENT_HEIGHT);
        content.setPrefSize(WIDTH, CONTENT_HEIGHT);
        content.setMaxSize(WIDTH, CONTENT_HEIGHT);
        content.setClip(new Rectangle(WIDTH, CONTENT_HEIGHT));
        return content;
    }

    private ColumnConstraints fixedColumn(double width) {
        ColumnConstraints column = new ColumnConstraints(width);
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);
        return column;
    }

    private RowConstraints fixedRow() {
        RowConstraints row = new RowConstraints(ROW_HEIGHT);
        row.setMinHeight(ROW_HEIGHT);
        row.setPrefHeight(ROW_HEIGHT);
        row.setMaxHeight(ROW_HEIGHT);
        return row;
    }

    private void addStatsRow(GridPane grid, int row, Label metricLabel, Label firstPlayerValue, Label secondPlayerValue) {
        addStatsCell(grid, metricLabel, 0, row, METRIC_COLUMN_WIDTH);
        addStatsCell(grid, firstPlayerValue, 1, row, PLAYER_COLUMN_WIDTH);
        addStatsCell(grid, secondPlayerValue, 2, row, PLAYER_COLUMN_WIDTH);
    }

    private void addStatsCell(GridPane grid, Label label, int col, int row, double width) {
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setMinHeight(ROW_HEIGHT);
        label.setPrefHeight(ROW_HEIGHT);
        label.setMaxHeight(ROW_HEIGHT);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setWrapText(false);
        GridPane.setFillWidth(label, true);
        GridPane.setFillHeight(label, true);
        GridPane.setHalignment(label, HPos.CENTER);
        grid.add(label, col, row);
    }

    private void setStatsRowVisible(boolean isVisible, Label metricLabel, Label firstPlayerValue, Label secondPlayerValue) {
        metricLabel.setVisible(isVisible);
        metricLabel.setManaged(isVisible);
        firstPlayerValue.setVisible(isVisible);
        firstPlayerValue.setManaged(isVisible);
        secondPlayerValue.setVisible(isVisible);
        secondPlayerValue.setManaged(isVisible);
    }

    private Color scoreFirstPlayerColor() {
        return scoreFirstPlayerUsesFirstColor ? GuiTheme.playerOne() : GuiTheme.playerTwo();
    }

    private Color scoreSecondPlayerColor() {
        return scoreSecondPlayerUsesFirstColor ? GuiTheme.playerOne() : GuiTheme.playerTwo();
    }

    private Label createPawnHeaderLabel(Color accentColor) {
        Label label = new Label("", createPawnMarker(accentColor));
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setWrapText(false);
        label.setMinWidth(PLAYER_COLUMN_WIDTH);
        label.setPrefWidth(PLAYER_COLUMN_WIDTH);
        label.setMaxWidth(PLAYER_COLUMN_WIDTH);
        label.setMinHeight(28);
        label.setStyle(GuiTheme.scoreTeamStyle(accentColor));
        return label;
    }

    private void updatePawnHeaderLabel(Label label, Color accentColor) {
        label.setText("");
        label.setGraphic(createPawnMarker(accentColor));
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setStyle(GuiTheme.scoreTeamStyle(accentColor));
    }

    private Circle createPawnMarker(Color accentColor) {
        Circle marker = new Circle(7, accentColor);
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(2);
        marker.setEffect(GuiTheme.softShadow(accentColor));
        return marker;
    }

    private Label createStatsMetricLabel(String text) {
        Label label = new Label(text);
        GuiTheme.styleMutedLabel(label);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMinHeight(26);
        label.setPadding(new Insets(0, 8, 0, 8));
        label.setStyle(GuiTheme.statsMetricStyle());
        return label;
    }

    private Label createScoreValueLabel(Color accentColor) {
        Label label = new Label();
        label.setTextFill(accentColor);
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 16));
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(26);
        label.setStyle(GuiTheme.statsValueStyle());
        return label;
    }

    private Label createStatsValueLabel() {
        Label label = new Label();
        label.setTextFill(GuiTheme.text());
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setMinHeight(26);
        label.setStyle(GuiTheme.statsValueStyle());
        return label;
    }

    private String formatThinkingTime(double thinkingTimeNanos) {
        if (Double.isNaN(thinkingTimeNanos) || thinkingTimeNanos <= 0) {
            return "--";
        }

        double milliseconds = thinkingTimeNanos / 1_000_000.0;
        if (milliseconds < 10) {
            return String.format(Locale.US, "%.1f ms", milliseconds);
        }
        if (milliseconds < 1000) {
            return String.format(Locale.US, "%.0f ms", milliseconds);
        }
        return String.format(Locale.US, "%.2f s", milliseconds / 1000.0);
    }

    private String formatImpact(double impact) {
        if (Double.isNaN(impact)) {
            return "--";
        }
        if (Math.abs(impact) < 0.05) {
            return "0.0";
        }
        return String.format(Locale.US, "%+.1f", impact);
    }

    private String formatWinRate(double winRate) {
        if (Double.isNaN(winRate)) {
            return "--";
        }
        return String.format(Locale.US, "%.0f%%", winRate);
    }
}
