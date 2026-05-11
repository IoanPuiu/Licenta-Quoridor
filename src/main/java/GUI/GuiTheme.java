package GUI;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

final class GuiTheme {

    private static ThemeOption activeTheme = ThemeOption.FOREST_COURT;

    private GuiTheme() {
    }

    static Color text() {
        return activeTheme.palette.text();
    }

    static Color mutedText() {
        return activeTheme.palette.mutedText();
    }

    static Color boardCell() {
        return activeTheme.palette.boardCell();
    }

    static Color boardCellAlt() {
        return activeTheme.palette.boardCellAlt();
    }

    static Color boardGrid() {
        return activeTheme.palette.boardGrid();
    }

    static Color boardFrame() {
        return activeTheme.palette.boardFrame();
    }

    static Color wall() {
        return activeTheme.palette.wall();
    }

    static Color playerOne() {
        return activeTheme.palette.playerOne();
    }

    static Color playerTwo() {
        return activeTheme.palette.playerTwo();
    }

    static Color danger() {
        return activeTheme.palette.danger();
    }

    static String rootStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, "
                + hex(activeTheme.palette.rootTop()) + ", "
                + hex(activeTheme.palette.rootBottom()) + ");";
    }

    static String panelStyle() {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 8;
                -fx-border-color: %s;
                -fx-border-radius: 8;
                -fx-border-width: 1;
                -fx-effect: dropshadow(gaussian, rgba(38, 48, 44, 0.16), 18, 0.15, 0, 6);
                """.formatted(hex(activeTheme.palette.panel()), hex(activeTheme.palette.panelBorder()));
    }

    static String playerCardStyle(Color accentColor) {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 8;
                -fx-border-color: %s;
                -fx-border-radius: 8;
                -fx-border-width: 0 0 0 4;
                -fx-effect: dropshadow(gaussian, rgba(38, 48, 44, 0.14), 14, 0.12, 0, 5);
                """.formatted(hex(activeTheme.palette.panel()), hex(accentColor));
    }

    static String comboBoxStyle() {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 6;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-border-width: 1;
                -fx-font-size: 14px;
                -fx-text-fill: %s;
                """.formatted(
                hex(activeTheme.palette.inputBackground()),
                hex(activeTheme.palette.inputBorder()),
                hex(activeTheme.palette.text()));
    }

    static String boardFrameStyle() {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 8;
                -fx-border-color: %s;
                -fx-border-radius: 8;
                -fx-border-width: 2;
                -fx-effect: dropshadow(gaussian, rgba(38, 48, 44, 0.22), 22, 0.18, 0, 8);
                """.formatted(hex(activeTheme.palette.boardFrame()), hex(activeTheme.palette.boardFrameBorder()));
    }

    static String boardStyle() {
        return "-fx-background-color: " + hex(activeTheme.palette.boardFrame()) + ";";
    }

    static MenuButton createThemeMenu(Runnable themeChangedHandler) {
        MenuButton themeButton = new MenuButton("Theme: " + activeTheme.label);
        for (ThemeOption theme : ThemeOption.values()) {
            MenuItem item = new MenuItem(theme.label);
            item.setOnAction(event -> {
                activeTheme = theme;
                themeButton.setText("Theme: " + activeTheme.label);
                styleThemeButton(themeButton);
                if (themeChangedHandler != null) {
                    themeChangedHandler.run();
                }
            });
            themeButton.getItems().add(item);
        }
        styleThemeButton(themeButton);
        return themeButton;
    }

    static void stylePrimaryButton(Button button) {
        button.setMinHeight(42);
        button.setStyle(primaryButtonStyle(activeTheme.palette.primaryButton()));
        button.setOnMouseEntered(event -> {
            if (!button.isDisabled()) {
                button.setStyle(primaryButtonStyle(activeTheme.palette.primaryButtonHover()));
            }
        });
        button.setOnMouseExited(event -> button.setStyle(primaryButtonStyle(activeTheme.palette.primaryButton())));
    }

    static void styleCompactButton(Button button) {
        button.setMinHeight(38);
        button.setStyle(compactButtonStyle(activeTheme.palette.primaryButton()));
        button.setOnMouseEntered(event -> {
            if (!button.isDisabled()) {
                button.setStyle(compactButtonStyle(activeTheme.palette.primaryButtonHover()));
            }
        });
        button.setOnMouseExited(event -> button.setStyle(compactButtonStyle(activeTheme.palette.primaryButton())));
    }

    static void styleThemeButton(MenuButton button) {
        button.setMinHeight(38);
        button.setStyle(themeButtonStyle(activeTheme.palette.themeButton()));
        button.setOnMouseEntered(event -> button.setStyle(themeButtonStyle(activeTheme.palette.themeButtonHover())));
        button.setOnMouseExited(event -> button.setStyle(themeButtonStyle(activeTheme.palette.themeButton())));
    }

    static void styleComboBox(ComboBox<?> comboBox) {
        comboBox.setPrefWidth(280);
        comboBox.setMinHeight(38);
        comboBox.setStyle(comboBoxStyle());
    }

    static void styleTextField(TextField textField) {
        textField.setPrefWidth(280);
        textField.setMinHeight(38);
        textField.setStyle(comboBoxStyle());
    }

    static void styleWindowTitle(Label label, int size) {
        label.setTextFill(text());
        label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, size));
    }

    static void styleMutedLabel(Label label) {
        label.setTextFill(mutedText());
        label.setFont(Font.font("Arial", FontWeight.BOLD, 13));
    }

    static void styleTurnLabel(Label label, Color color) {
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setPadding(new Insets(7, 14, 7, 14));
        label.setBackground(new Background(new BackgroundFill(color, new CornerRadii(20), Insets.EMPTY)));
    }

    static DropShadow softShadow(Color color) {
        DropShadow shadow = new DropShadow();
        shadow.setColor(color.deriveColor(0, 1, 1, 0.35));
        shadow.setRadius(12);
        shadow.setOffsetY(3);
        return shadow;
    }

    private static String primaryButtonStyle(Color backgroundColor) {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 6;
                -fx-border-radius: 6;
                -fx-text-fill: white;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-padding: 10 22 10 22;
                -fx-cursor: hand;
                """.formatted(hex(backgroundColor));
    }

    private static String compactButtonStyle(Color backgroundColor) {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 6;
                -fx-border-radius: 6;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-padding: 8 14 8 14;
                -fx-cursor: hand;
                """.formatted(hex(backgroundColor));
    }

    private static String themeButtonStyle(Color backgroundColor) {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 6;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-border-width: 1;
                -fx-text-fill: %s;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-padding: 8 14 8 14;
                -fx-cursor: hand;
                """.formatted(
                hex(backgroundColor),
                hex(activeTheme.palette.inputBorder()),
                hex(activeTheme.palette.text()));
    }

    private static String hex(Color color) {
        return "#%02X%02X%02X".formatted(
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    private enum ThemeOption {
        FOREST_COURT("Forest Court", new ThemePalette(
                Color.web("#EAF1EC"),
                Color.web("#F7F0E5"),
                Color.web("#FFFDF7"),
                Color.web("#D7C9AF"),
                Color.web("#F6F1E8"),
                Color.web("#C9B99D"),
                Color.web("#24312F"),
                Color.web("#65736F"),
                Color.web("#3B7A68"),
                Color.web("#418873"),
                Color.web("#E7D1A2"),
                Color.web("#7B4F32"),
                Color.web("#4E3321"),
                Color.web("#241A14"),
                Color.web("#7C3AED"),
                Color.web("#F43F5E"),
                Color.web("#2F7D68"),
                Color.web("#256B59"),
                Color.web("#EFE7D8"),
                Color.web("#E6D7BE"),
                Color.web("#D64545"))),
        SUNSET_GARDEN("Sunset Garden", new ThemePalette(
                Color.web("#FFF1E8"),
                Color.web("#EAF4F0"),
                Color.web("#FFF9F2"),
                Color.web("#E7BCA7"),
                Color.web("#F8E6D8"),
                Color.web("#D99A83"),
                Color.web("#332825"),
                Color.web("#78645E"),
                Color.web("#C7674B"),
                Color.web("#D77E5D"),
                Color.web("#F4D7A1"),
                Color.web("#6C4A3F"),
                Color.web("#4A3029"),
                Color.web("#2D1B17"),
                Color.web("#2563EB"),
                Color.web("#16A34A"),
                Color.web("#B85C47"),
                Color.web("#9E4D3D"),
                Color.web("#F5E2D4"),
                Color.web("#EBCDBB"),
                Color.web("#B4233A"))),
        COASTAL_MIST("Coastal Mist", new ThemePalette(
                Color.web("#E9F6F8"),
                Color.web("#F6F1E6"),
                Color.web("#FEFCF6"),
                Color.web("#B7D3D2"),
                Color.web("#EFF7F5"),
                Color.web("#98BFBD"),
                Color.web("#213435"),
                Color.web("#5F7474"),
                Color.web("#2B8C8C"),
                Color.web("#38A3A5"),
                Color.web("#D7E7C2"),
                Color.web("#486B66"),
                Color.web("#2E4643"),
                Color.web("#20312E"),
                Color.web("#7C3AED"),
                Color.web("#F59E0B"),
                Color.web("#237A78"),
                Color.web("#1D6564"),
                Color.web("#E2F0EE"),
                Color.web("#CBE1DE"),
                Color.web("#C2413B")));

        private final String label;
        private final ThemePalette palette;

        ThemeOption(String label, ThemePalette palette) {
            this.label = label;
            this.palette = palette;
        }
    }

    private record ThemePalette(
            Color rootTop,
            Color rootBottom,
            Color panel,
            Color panelBorder,
            Color inputBackground,
            Color inputBorder,
            Color text,
            Color mutedText,
            Color boardCell,
            Color boardCellAlt,
            Color boardGrid,
            Color boardFrame,
            Color boardFrameBorder,
            Color wall,
            Color playerOne,
            Color playerTwo,
            Color primaryButton,
            Color primaryButtonHover,
            Color themeButton,
            Color themeButtonHover,
            Color danger) {
    }
}
