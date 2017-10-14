package me.coley.recaf.ui.component.tree;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.asm.Access;

import static me.coley.recaf.ui.Icons.*;

/**
 * TreeCellRenderer for the contents of a jar file/java bin.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class JavaTreeRenderer extends DefaultTreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node instanceof ASMInsnTreeNode) {
			setIcon(MISC_RESULT);
		} else if (node instanceof ASMMethodTreeNode) {
			int acc = ((ASMMethodTreeNode) node).getMethod().access;
			if (Access.isPublic(acc)) {
				setIcon(ML_PUBLIC);
			} else if (Access.isProtected(acc)) {
				setIcon(ML_PROTECTED);
			} else if (Access.isPrivate(acc)) {
				setIcon(ML_PRIVATE);
			} else {
				setIcon(ML_DEFAULT);
			}
		} else if (node instanceof ASMFieldTreeNode) {
			int acc = ((ASMFieldTreeNode) node).getField().access;
			if (Access.isPublic(acc)) {
				setIcon(FL_PUBLIC);
			} else if (Access.isProtected(acc)) {
				setIcon(FL_PROTECTED);
			} else if (Access.isPrivate(acc)) {
				setIcon(FL_PRIVATE);
			} else {
				setIcon(FL_DEFAULT);
			}
		} else if (node instanceof ASMTreeNode) {
			ASMTreeNode mtNode = (ASMTreeNode) node;
			if (mtNode.getNode() == null) {
				// The root node of the tree has no node.
				// The root isn't DefaultMutableTreeNode because otherwise
				// it makes the code for generating the tree a uglier. This
				// if statement is the exchange.
				if (node.getChildCount() > 0) {
					// Tis a package.
					setIcon(MISC_PACKAGE);
				} else {
					setIcon(MISC_RESULT);
				}
			} else {
				// Get the classnode, determine icon by access
				ClassNode cn = mtNode.getNode();
				int acc = cn.access;
				if (Access.isInterface(acc)) {
					setIcon(CL_INTERFACE);
				} else if (Access.isEnum(acc)) {
					setIcon(CL_ENUM);
				} else if (Access.isAnnotation(acc)) {
					setIcon(CL_ANNOTATION);
				} else {
					setIcon(CL_CLASS);
				}
			}
		} else if (node.getChildCount() > 0) {
			// Tis a package.
			setIcon(MISC_PACKAGE);
		} else {
			setIcon(MISC_RESULT);
		}

		return this;
	}

}