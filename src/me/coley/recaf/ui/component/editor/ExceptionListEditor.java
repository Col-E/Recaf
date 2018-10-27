package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Editor for editing exceptions list, as {@code List<String>}.
 * 
 * @author Matt
 */
public class ExceptionListEditor extends AbstractListEditor<String, TextField, List<String>> {
	public ExceptionListEditor(Item item) {
		super(item, "ui.bean.method.exceptions.name", 300, 500);
	}

	@Override
	protected TextField create(ListView<String> view) {
		TextField text = new TextField();
		text.setOnAction((e) -> add(text, view));
		return text;
	}

	@Override
	protected String getValue(TextField control) {
		return control.textProperty().getValue();
	}

	@Override
	protected void reset(TextField control) {
		control.textProperty().setValue("");
	}
}