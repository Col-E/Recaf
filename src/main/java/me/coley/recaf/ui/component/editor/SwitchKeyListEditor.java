package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Editor for editing keys <i>(in switch opcodes)</i>, as {@code List<Integer>}.
 * 
 * @author Matt
 */
public class SwitchKeyListEditor extends AbstractListEditor<Integer, TextField, List<Integer>> {
	public SwitchKeyListEditor(Item item) {
		super(item, "ui.bean.opcode.keys.name", 300, 500);
	}

	@Override
	protected TextField create(ListView<Integer> view) {
		return new TextField();
	}

	@Override
	protected Integer getValue(TextField control) {
		try {
			return Integer.parseInt(control.textProperty().get());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	protected void reset(TextField control) {
		control.textProperty().setValue("");
	}
}