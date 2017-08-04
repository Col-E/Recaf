package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.AccessPanel;

@SuppressWarnings("serial")
public class AccessBox extends BasicFrame {

	public AccessBox(Object value, JComponent owner) throws Exception {
		AccessPanel panel = createPanel(value, owner);
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		setTitle(panel.getTitle());
		setVisible(true);
	}

	private static AccessPanel createPanel(Object value, JComponent owner) throws Exception {
		if (value instanceof ClassNode) {
			return new AccessPanel((ClassNode) value, owner);
		} else if (value instanceof FieldNode) {
			return new AccessPanel((FieldNode) value, owner);
		} else if (value instanceof MethodNode) {
			return new AccessPanel((MethodNode) value, owner);
		}
		return null;
	}

}
