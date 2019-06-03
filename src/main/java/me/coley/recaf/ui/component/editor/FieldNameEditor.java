package me.coley.recaf.ui.component.editor;

import me.coley.recaf.event.ClassReloadEvent;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.event.Bus;
import me.coley.recaf.event.FieldRenameEvent;
import me.coley.recaf.ui.component.*;

/**
 * Editor for field names, emits a rename event when the rename is applied.
 * 
 * @author Matt
 */
public class FieldNameEditor extends StagedCustomEditor<String> {
	public FieldNameEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		ReflectiveFieldNodeItem refItem = (ReflectiveFieldNodeItem) item;
		ClassNode cn = refItem.getNode();
		FieldNode fn = (FieldNode) refItem.getOwner();
		TextField txtName = new TextField();
		txtName.setText(fn.name);
		txtName.setOnAction(e -> rename(cn, fn, txtName));
		return txtName;
	}

	private void rename(ClassNode owner, FieldNode field, TextField txtName) {
		String text = txtName.getText();
		if (!txtName.isDisabled() && !text.equals(field.name)) {
			Bus.post(new FieldRenameEvent(owner, field, field.name, text));
			Bus.post(new ClassReloadEvent(owner.name));
			// use disable property to prevent-double send
			txtName.setDisable(true);
		}
	}
}