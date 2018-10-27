package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.InnerClassNode;

import javafx.scene.control.*;
import me.coley.recaf.ui.component.constructor.InnerClassNodeConstructor;
import me.coley.recaf.util.Icons;

/**
 * Editor for editing inner-classes list, as {@code List<InnerClassNode>}.
 * 
 * @author Matt
 */
public class InnerClassListEditor extends AbstractListEditor<InnerClassNode, InnerClassNodeConstructor, List<InnerClassNode>> {

	public InnerClassListEditor(Item item) {
		super(item, "ui.bean.class.innerclasses.name", 530, 400);
	}

	@Override
	protected void setupView(ListView<InnerClassNode> view) {
		view.setCellFactory(cell -> new ListCell<InnerClassNode>() {
			@Override
			protected void updateItem(InnerClassNode node, boolean empty) {
				super.updateItem(node, empty);
				if (empty || node == null) {
					setGraphic(null);
					setText(null);
				} else {
					setGraphic(Icons.getMember(node.access, true));
					if (node.innerName == null || node.innerName.isEmpty()) {
						setText(node.name.substring(node.name.lastIndexOf("/") + 1));
					} else {
						setText(node.innerName);
					}
				}
			}
		});
	}

	@Override
	protected InnerClassNodeConstructor create(ListView<InnerClassNode> view) {
		return new InnerClassNodeConstructor();
	}

	@Override
	protected InnerClassNode getValue(InnerClassNodeConstructor control) {
		return control.get();
	}

	@Override
	protected void reset(InnerClassNodeConstructor control) {
		control.reset();
	}
}