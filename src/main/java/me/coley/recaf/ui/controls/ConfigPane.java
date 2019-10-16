package me.coley.recaf.ui.controls;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import me.coley.recaf.config.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.editor.*;

import java.util.*;
import java.util.function.Function;

public class ConfigPane extends BorderPane {
	private final Map<String, Function<FieldWrapper, Node>> editorOverrides = new HashMap<>();
	private final GridPane grid = new GridPane();

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Display config.
	 */
	public ConfigPane(GuiController controller, ConfDisplay config) {
		setupLayout();
		editorOverrides.put("display.language", LanguageCombo::new);
		editorOverrides.put("display.style", v -> new StyleCombo(controller, v));
		setupConfigControls(config);
	}

	private void setupConfigControls(Config config) {
		int row = 0;
		for(FieldWrapper field : config.getConfigFields()) {
			// Skip hidden values
			if(field.hidden())
				continue;
			// Label
			Label name = new Label(field.name());
			Label desc = new Label(field.description());
			name.getStyleClass().add("h1");
			desc.getStyleClass().add("faint");
			VBox label = new VBox(name, desc);
			// Editor
			Node editor = editor(field);
			// GridPane.setValignment(editor, VPos.CENTER);
			// Add value
			grid.add(label, 0, row);
			grid.add(editor, 1, row);
			row++;
		}
	}

	private Node editor(FieldWrapper field) {
		// Check for override editor
		if (editorOverrides.containsKey(field.key()))
			return editorOverrides.get(field.key()).apply(field);
		// Create default editor
		return new TextField("TODO");
	}

	private void setupLayout() {
		setCenter(grid);
		setPadding(new Insets(5, 10, 5, 10));
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(70);
		column2.setPercentWidth(30);
		column2.setFillWidth(true);
		column2.setHgrow(Priority.ALWAYS);
		column2.setHalignment(HPos.RIGHT);
		grid.getColumnConstraints().addAll(column1, column2);
		grid.setVgap(5.0);
	}
}
