package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.Node;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.ui.component.AccessButton;

/**
 * Editor for editing access flags for classes.
 * 
 * @author Matt
 */
public class AccessClassEditor<T extends Integer> extends StagedCustomEditor<T> {
	public AccessClassEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		return new AccessButton(AccessFlag.Type.CLASS, getValue().intValue()) {
			@SuppressWarnings("unchecked")
			@Override
			public void setAccess(int access) {
				super.setAccess(access);
				setValue((T) Integer.valueOf(access));
			}
		};
	}
}