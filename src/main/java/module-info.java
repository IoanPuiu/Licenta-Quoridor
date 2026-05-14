module GUI {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;

    opens GUI to javafx.fxml;
    exports GUI to javafx.graphics;
    exports SlowModel;
    exports AI;
    exports AI.MTCS;
}