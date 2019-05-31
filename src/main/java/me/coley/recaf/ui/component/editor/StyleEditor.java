package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import me.coley.recaf.util.SelfReference;

/**
 * Editor for editing access flags for classes.
 * 
 * @author Matt
 */
public class StyleEditor extends StagedCustomEditor<String> {
	public StyleEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		String value = getValue();
		try {
			ComboBox<String> combo = new ComboBox<>();
			List<String> styles = SelfReference.get().getStyles();
			combo.getItems().addAll(styles);
			combo.getSelectionModel().select(value);
			combo.getSelectionModel().selectedItemProperty().addListener((ob, o, n)->{
				setValue(n);
			});
			return combo;
		} catch (Exception e) {}
		TextField text = new TextField(value);
		text.textProperty().addListener((ob, o, n)->{
			setValue(n);
		});
		return text;
	}
}