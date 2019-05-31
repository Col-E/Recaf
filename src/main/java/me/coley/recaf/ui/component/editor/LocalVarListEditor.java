package me.coley.recaf.ui.component.editor;

import java.util.Comparator;
import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import javafx.scene.control.*;
import me.coley.recaf.ui.component.constructor.*;

/**
 * Editor for editing local variable table as {@code List<LocalVariableNode>}.
 * 
 * @author Matt
 */
public class LocalVarListEditor extends AbstractListEditor<LocalVariableNode, LocalVariableNodeConstructor, List<LocalVariableNode>> {
	public LocalVarListEditor(Item item) {
		super(item, "ui.bean.method.localvariables.name", 800, 600);
	}

	@Override
	protected LocalVariableNodeConstructor create(ListView<LocalVariableNode> view) {
		MethodNode method = (MethodNode) item.getOwner();
		return new LocalVariableNodeConstructor(method, view);
	}

	@Override
	protected LocalVariableNode getValue(LocalVariableNodeConstructor control) {
		return control.get();
	}

	@Override
	protected void reset(LocalVariableNodeConstructor control) {
		control.reset();
	}

	@Override
	protected void setupView(ListView<LocalVariableNode> view) {
		List<LocalVariableNode> value = getValue();
		MethodNode method = (MethodNode) item.getOwner();
		if (value != null) {
			value.sort(new Comparator<LocalVariableNode>() {
				@Override
				public int compare(LocalVariableNode o1, LocalVariableNode o2) {
					return Integer.compare(o1.index, o2.index);
				}
			});
		}
		view.setCellFactory(cell -> new ListCell<LocalVariableNode>() {
			@Override
			protected void updateItem(LocalVariableNode node, boolean empty) {
				super.updateItem(node, empty);
				if (empty || node == null) {
					setGraphic(null);
				} else {
					setGraphic(new LocalVariableNodeConstructor(method, view, node));
				}
			}
		});
	}
}
