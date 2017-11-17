package me.coley.recaf.ui.component.internalframe;

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JTextField;

import org.objectweb.asm.tree.AbstractInsnNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;

@SuppressWarnings("serial")
public class BlockSaveBox extends BasicFrame {
	private JTextField text;

	public BlockSaveBox(List<AbstractInsnNode> list) {
		super("Save block");
		setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		add(new LabeledComponent("Name: ", text = new JTextField()));
		add(new ActionButton("Save", () -> {
			Recaf.INSTANCE.confblocks.add(text.getText(), list);
			dispose();
		}));
		setVisible(true);
	}
}
