package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import javafx.scene.Node;
import me.coley.recaf.ui.component.LabelButton;
import me.coley.recaf.ui.component.ReflectiveInsnItem;

/**
 * Editor for labels.
 * 
 * @author Matt
 */
public abstract class LabelEditor extends StagedCustomEditor<LabelNode> {
	public LabelEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		MethodNode mn = getMethod();
		LabelButton btn = new LabelButton(getValue(), mn);
		btn.setUpdateTask(l -> {
			setValue(l);
		});
		return btn;
	}

	protected abstract MethodNode getMethod();

	public static class Opcode extends LabelEditor {
		public Opcode(Item item) {
			super(item);
		}

		@Override
		protected MethodNode getMethod() {
			return ((ReflectiveInsnItem) item).getMethod();
		}
	}
}