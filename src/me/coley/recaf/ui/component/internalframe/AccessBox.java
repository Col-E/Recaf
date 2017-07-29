package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.AccessPanel;

@SuppressWarnings("serial")
public class AccessBox extends BasicFrame {

	public AccessBox(Object value) throws Exception {
		AccessPanel panel = getPanel(value);
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		setTitle(panel.getTitle());
		setVisible(true);
	}

	private static AccessPanel getPanel(Object value) throws Exception {
		if (value instanceof ClassNode) {
			return new AccessPanel((ClassNode) value);
		} else if (value instanceof FieldNode) {
			return new AccessPanel((FieldNode) value);
		} else if (value instanceof MethodNode) {
			return new AccessPanel((MethodNode) value);
		}
		return null;
	}

}
