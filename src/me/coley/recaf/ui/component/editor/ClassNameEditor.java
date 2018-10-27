package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.ClassNode;

import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.event.Bus;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.ui.component.*;

/**
	 * String editor that also emits a ClassRenameEvent when the enter key is
	 * pressed.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public class ClassNameEditor<T extends String> extends StagedCustomEditor<T> {
		public ClassNameEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			ReflectiveClassNodeItem refItem = (ReflectiveClassNodeItem) item;
			ClassNode cn = refItem.getNode();
			TextField txtName = new TextField();
			txtName.setText(cn.name);
			txtName.setOnAction(e -> rename(cn, txtName));
			// This works for when focus is lost, but I'm not sure if thats user
			// friendly...
			// If you type anything in and click anywhere else (or close the
			// tab) it will
			// do the rename action.
			//@formatter:off
			/*
			txtName.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> focused, Boolean oldVal, Boolean newVal) {
					if (!newVal) {
						rename(cn, txtName);
					}
				}
			});
			*/
			//@formatter:on
			return txtName;
		}

		private void rename(ClassNode node, TextField txtName) {
			String text = txtName.getText();
			if (!txtName.isDisabled() && !text.equals(node.name)) {
				Bus.post(new ClassRenameEvent(node, node.name, text));
				// use disable property to prevent-double send
				txtName.setDisable(true);
			}
		}
	}