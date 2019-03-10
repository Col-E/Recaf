package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.InsnList;

import javafx.scene.Node;
import me.coley.event.Bus;
import me.coley.recaf.event.InsnOpenEvent;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.ui.component.ReflectiveMethodNodeItem;
import me.coley.recaf.util.Lang;

public class InsnProxyListEditor extends StagedCustomEditor<InsnList> {
	public InsnProxyListEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		ReflectiveMethodNodeItem refItem = (ReflectiveMethodNodeItem) item;
		InsnList list = (InsnList) item.getValue();
		return new ActionButton(Lang.get("misc.edit"), () -> {
			Bus.post(new InsnOpenEvent(refItem.getNode(), refItem.getMethodNode(), list.getFirst()));
		});
	}
}