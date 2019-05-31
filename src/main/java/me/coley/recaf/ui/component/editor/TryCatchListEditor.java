package me.coley.recaf.ui.component.editor;

import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import javafx.scene.control.*;
import me.coley.recaf.ui.component.constructor.*;

/**
 * Editor for editing try-catch block lists as {@code List<TryCatchBlockNode>}.
 * 
 * @author Matt
 */
public class TryCatchListEditor extends AbstractListEditor<TryCatchBlockNode, TryCatchBlockConstructor, List<TryCatchBlockNode>> {
	public TryCatchListEditor(Item item) {
		super(item, "ui.bean.method.trycatchblocks.name", 600, 600);
	}

	@Override
	protected TryCatchBlockConstructor create(ListView<TryCatchBlockNode> view) {
		MethodNode method = (MethodNode) item.getOwner();
		return new TryCatchBlockConstructor(method, view);
	}

	@Override
	protected TryCatchBlockNode getValue(TryCatchBlockConstructor control) {
		return control.get();
	}

	@Override
	protected void reset(TryCatchBlockConstructor control) {
		control.reset();
	}

	@Override
	protected void setupView(ListView<TryCatchBlockNode> view) {
		MethodNode method = (MethodNode) item.getOwner();
		view.setCellFactory(cell -> new ListCell<TryCatchBlockNode>() {
			@Override
			protected void updateItem(TryCatchBlockNode node, boolean empty) {
				super.updateItem(node, empty);
				if (empty || node == null) {
					setGraphic(null);
				} else {
					setGraphic(new TryCatchBlockConstructor(method, view, node));
				}
			}
		});
	}
}