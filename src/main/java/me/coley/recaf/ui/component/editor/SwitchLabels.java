package me.coley.recaf.ui.component.editor;

import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.*;
import javafx.scene.control.*;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.Lang;

/**
 * Editor for editing labels <i>(in switch opcodes)</i>, as
 * {@code List<LabelNode>}.
 * 
 * @author Matt
 */
public class SwitchLabels extends AbstractListEditor<LabelNode, LabelButton, List<LabelNode>> {
	public SwitchLabels(Item item) {
		super(item, "ui.bean.opcode.labels.name", 300, 500);
	}

	@Override
	protected void setupView(ListView<LabelNode> view) {
		MethodNode method = (MethodNode) item.getOwner();
		view.setCellFactory(cell -> new ListCell<LabelNode>() {
			@Override
			protected void updateItem(LabelNode node, boolean empty) {
				super.updateItem(node, empty);
				if (empty || node == null) {
					setGraphic(null);
				} else {
					setGraphic(FormatFactory.insnNode(node, method));
				}
			}
		});
	}

	@Override
	protected LabelButton create(ListView<LabelNode> view) {
		MethodNode method = (MethodNode) item.getOwner();
		return new LabelButton(Lang.get("ui.bean.opcode.label.nullvalue"), method);
	}

	@Override
	protected LabelNode getValue(LabelButton control) {
		return control.getLabel();
	}

	@Override
	protected void reset(LabelButton control) {
		control.setLabel(null);
	}
}