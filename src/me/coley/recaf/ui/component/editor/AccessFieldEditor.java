package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.Node;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.ui.component.AccessButton;

/**
 * Editor for editing access flags for fields.
 * 
 * @author Matt
 */
public class AccessFieldEditor extends StagedCustomEditor<Integer> {
	public AccessFieldEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		return new AccessButton(AccessFlag.Type.FIELD, getValue().intValue()) {
			@Override
			public void setAccess(int access) {
				super.setAccess(access);
				setValue(Integer.valueOf(access));
			}
		};
	}
}