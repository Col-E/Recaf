package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.scripting.ScriptEngine;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

public class ScriptEditorPane extends BorderPane implements Cleanable {
    private final Logger logger = Logging.get(ScriptEditorPane.class);
    private final JavaArea javaArea;

    public ScriptEditorPane() {
        ProblemTracking tracking = new ProblemTracking();
        tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
        this.javaArea = new JavaArea(tracking);
        Node node = new VirtualizedScrollPane<>(javaArea);
        Node errorDisplay = new ErrorDisplay(javaArea, tracking);
        StackPane stack = new StackPane();
        StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
        StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
        stack.getChildren().add(node);
        stack.getChildren().add(errorDisplay);
        setCenter(stack);
        // Search support
        SearchBar.install(this, javaArea);
        // Bottom controls for quick config changes
        Node buttonBar = createButtonBar();
        setBottom(buttonBar);
    }

    private Node createButtonBar() {
        HBox box = new HBox();
        box.setPadding(new Insets(10));
        box.setSpacing(10);
        box.getStyleClass().add("button-container");
        box.setAlignment(Pos.CENTER_LEFT);

        Button executeButton = new Button("Execute");
        executeButton.setOnMouseClicked((mouseEvent) -> ScriptEngine.executeBsh(javaArea.getText()));

        box.getChildren().add(executeButton);

        return box;
    }

    @Override
    public void cleanup() {
        javaArea.cleanup();
    }
}
