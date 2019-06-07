package me.coley.recaf.ui.component.editor;

import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import me.coley.event.Bus;
import me.coley.recaf.event.ClassHierarchyUpdateEvent;
import org.controlsfx.control.PropertySheet.Item;

import java.util.List;

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
		text.setOnAction((e) -> {
			add(text, view);
			Bus.post(new ClassHierarchyUpdateEvent());
		});
		return text;
	}

	@Override
	protected String getValue(TextField control) {
		return control.textProperty().get();
	}

	@Override
	public void setValue(List<String> value) {
		super.setValue(value);
		Bus.post(new ClassHierarchyUpdateEvent());
	}

	@Override
	protected void reset(TextField control) {
		control.textProperty().setValue("");
	}
}