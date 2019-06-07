package me.coley.recaf.ui.component.editor;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import me.coley.event.Bus;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.component.ReflectiveClassNodeItem;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.ClassNode;

/**
 * String editor that also emits a ClassSuperUpdateEvent when the enter key is
 * pressed.
 * 
 * @author Matt
 */
public class ClassSuperNameEditor extends StagedCustomEditor<String> {
	public ClassSuperNameEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		ReflectiveClassNodeItem refItem = (ReflectiveClassNodeItem) item;
		ClassNode cn = refItem.getNode();
		TextField txtName = new TextField();
		txtName.setText(cn.superName);
		txtName.setOnAction(e -> rename(cn, txtName));
		return txtName;
	}

	private void rename(ClassNode node, TextField txtName) {
		String text = txtName.getText();
		if (!text.equals(node.superName)) {
			setValue(text);
			Bus.post(new ClassReloadEvent(node.name, node.name));
			Bus.post(new ClassHierarchyUpdateEvent());
		}
	}
}