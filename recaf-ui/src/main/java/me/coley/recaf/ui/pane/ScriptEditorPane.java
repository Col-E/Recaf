package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.Representation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.Undoable;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.scripting.ScriptEngine;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptEditorPane extends BorderPane implements Representation, Undoable, Cleanable {
    private final Logger logger = Logging.get(ScriptEditorPane.class);
    private final SyntaxArea bshArea;
    private File currentFile;
    private Tab tab;

    public ScriptEditorPane() {
        ProblemTracking tracking = new ProblemTracking();
        tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
        this.bshArea = new SyntaxArea(Languages.JAVA, tracking);
        Node node = new VirtualizedScrollPane<>(bshArea);
        Node errorDisplay = new ErrorDisplay(bshArea, tracking);
        StackPane stack = new StackPane();
        StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
        StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
        stack.getChildren().add(node);
        stack.getChildren().add(errorDisplay);
        setCenter(stack);
        // Search support
        SearchBar.install(this, bshArea);
        //Node buttonBar = createButtonBar();
        //setBottom(buttonBar);
        Configs.keybinds().installEditorKeys(this);
    }

    private Node createButtonBar() {
        HBox box = new HBox();
        box.setPadding(new Insets(10));
        box.setSpacing(10);
        box.getStyleClass().add("button-container");
        box.setAlignment(Pos.CENTER_LEFT);

        Button executeButton = new Button("Execute");
        executeButton.setOnMouseClicked((mouseEvent) -> execute());

        box.getChildren().add(executeButton);

        return box;
    }

    public boolean execute() {
        return ScriptEngine.executeBsh(bshArea.getText());
    }

    public File openFile() {
        File file = new FileChooser().showOpenDialog(RecafUI.getWindows().getMainWindow());

        if (file == null) {
            return null;
        }

        Path path = file.toPath();

        try {
            bshArea.setText(new String(Files.readAllBytes(path)));
            currentFile = file;
        } catch (IOException e) {
            logger.error("Failed to open script: {}", e.getLocalizedMessage());
            return null;
        }

        logger.info("Opened script {}", currentFile.getName());

        return file;
    }

    @Override
    public void cleanup() {
        bshArea.cleanup();
    }

    public void setTitle() {
        String tabTitle = Lang.get("menu.scripting.editor");
        if (currentFile != null) {
            tabTitle += " - " + currentFile.getName();
        }
        tab.setText(tabTitle);
    }

    @Override
    public SaveResult save() {
        // Not linked to a file on disk yet
        if (currentFile == null) {
            currentFile = new FileChooser().showSaveDialog(RecafUI.getWindows().getMainWindow());

            if (currentFile == null) {
                return SaveResult.FAILURE;
            }

            setTitle();
        }

        try {
            Files.write(currentFile.toPath(), bshArea.getText().getBytes());
        } catch (IOException e) {
            logger.error("Failed to save script: {}", e.getLocalizedMessage());
            return SaveResult.FAILURE;
        }

        logger.info("Saved script to {}", currentFile.getPath());

        return SaveResult.SUCCESS;
    }

    @Override
    public boolean supportsEditing() {
        return true;
    }

    @Override
    public Node getNodeRepresentation() {
        return this;
    }

    public Tab getTab() {
        return tab;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    @Override
    public void undo() {
        // TODO: Implement me
    }
}
