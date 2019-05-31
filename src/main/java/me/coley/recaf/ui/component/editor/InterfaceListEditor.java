package me.coley.recaf.ui.component.editor;

import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import javafx.scene.control.*;

/**
 * Editor for editing interface list, as {@code List<String>}.
 * 
 * @author Matt
 */
public class InterfaceListEditor extends AbstractListEditor<String, TextField, List<String>> {
	public InterfaceListEditor(Item item) {
		super(item, "ui.bean.class.interfaces.name", 300, 500);
	}

	@Override
	protected TextField create(ListView<String> view) {
		TextField text = new TextField();
		text.setOnAction((e) -> add(text, view));
		return text;
	}

	@Override
	protected String getValue(TextField control) {
		return control.textProperty().get();
	}

	@Override
	protected void reset(TextField control) {
		control.textProperty().setValue("");
	}
}