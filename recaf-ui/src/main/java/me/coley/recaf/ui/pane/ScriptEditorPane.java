package me.coley.recaf.ui.pane;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
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
import me.coley.recaf.scripting.ScriptEngine;
import me.coley.recaf.scripting.ScriptResult;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.Representation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.Undoable;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Editor for scripts to be run via {@link ScriptEngine}.
 *
 * @author Wolfie / win32kbase
 * @author Matt Coley
 */
public class ScriptEditorPane extends BorderPane implements Representation, Undoable, Cleanable {
	private static final Logger logger = Logging.get(ScriptEditorPane.class);
	private final ProblemTracking tracking = new ProblemTracking();
	private final SyntaxArea bshArea;
	private File currentFile;
	private Tab tab;

	/**
	 * New editor pane.
	 */
	public ScriptEditorPane() {
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
		Node buttonBar = createButtonBar();
		setBottom(buttonBar);
		Configs.keybinds().installEditorKeys(this);
	}

	private Node createButtonBar() {
		HBox box = new HBox();
		box.getStyleClass().add("button-container");
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_RIGHT);
		Button executeButton = new Button("Execute");
		Button saveButton = new Button("Save");
		executeButton.setMinWidth(75);
		saveButton.setMinWidth(75);
		executeButton.setGraphic(Icons.getIconView(Icons.PLAY));
		saveButton.setGraphic(Icons.getIconView(Icons.SAVE));
		executeButton.setOnMouseClicked(e -> {
			tracking.clearOfType(ProblemOrigin.JAVA_COMPILE);
			ScriptResult result = execute();
			if (result.wasSuccess()) {
				Animations.animateSuccess(getNodeRepresentation(), 1000);
			} else {
				Animations.animateFailure(getNodeRepresentation(), 1000);
				handleBshError(result);
			}
		});
		saveButton.setOnMouseClicked(e -> {
			SaveResult result = save();
			if (result == SaveResult.SUCCESS) {
				Animations.animateSuccess(getNodeRepresentation(), 1000);
			} else if (result == SaveResult.FAILURE) {
				Animations.animateFailure(getNodeRepresentation(), 1000);
			}
		});
		box.getChildren().addAll(executeButton, saveButton);
		return box;
	}

	public ScriptResult execute() {
		return ScriptEngine.execute(bshArea.getText());
	}

	public File openFile() {
		File file = new FileChooser().showOpenDialog(RecafUI.getWindows().getMainWindow());
		if (file == null)
			return null;

		try {
			Path path = file.toPath();
			bshArea.setText(new String(Files.readAllBytes(path)));
			currentFile = file;
		} catch (IOException e) {
			logger.error("Failed to open script: {}", e.getLocalizedMessage());
			return null;
		}

		logger.info("Opened script {}", currentFile.getName());
		return file;
	}

	private void handleBshError(ScriptResult result) {
		int line;
		String message = result.getException().getMessage();
		if (result.wasScriptParseFailure()) {
			ParseException error = result.getExceptionAsParse();
			if (error.currentToken != null) {
				line = error.currentToken.beginLine;
			} else if (message.contains("at line ")) {
				String lineText = message.substring(message.indexOf("line ") + 5, message.indexOf(","));
				if (lineText.matches("\\d+"))
					line = Integer.parseInt(lineText);
				else
					line = -1;
			} else {
				line = -1;
			}
		} else if (result.wasScriptTargetFailure()) {
			TargetError target = result.getExceptionAsTarget();
			if (target.getTarget() instanceof NullPointerException) {
				message = "Could not resolve reference \"" + target.getErrorText() + "\"";
			}
			line = target.getErrorLineNumber();
		} else if (result.wasScriptFailure()) {
			EvalError error = result.getExceptionAsEval();
			if (message.contains("not found")) {
				message = message.substring(message.lastIndexOf(" : ") + 3);
			}
			line = error.getErrorLineNumber();
		} else {
			// Non BSH error
			line = -1;
		}
		tracking.addProblem(line,
				new ProblemInfo(ProblemOrigin.JAVA_COMPILE, ProblemLevel.ERROR, line, message));
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
