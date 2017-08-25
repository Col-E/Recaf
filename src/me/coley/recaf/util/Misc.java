package me.coley.recaf.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.tree.ClassNode;
import me.coley.recaf.ui.component.tree.ASMTreeNode;

/**
 * Random utility methods that don't fit in other places go here. Things here
 * should be moved elsewhere as soon as possible.
 */
public class Misc {

	public static boolean isInt(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isBoolean(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Boolean.parseBoolean(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void addAll(JInternalFrame owner, JComponent... components) {
		for (JComponent component : components) {
			owner.add(component);
		}
	}

	public static void setInt(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isBoolean(vts)) {
			set(owner, fieldName, Boolean.parseBoolean(vts));
		}
	}

	public static void setBoolean(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isInt(vts)) {
			set(owner, fieldName, Integer.parseInt(vts));
		}
	}

	public static void set(Object owner, String fieldName, Object value) {
		// Ok, so this is mostly used in lambdas, which can't handle
		// exceptions....
		// so just try-catch it. Ugly, but hey it'll have to do.
		try {
			Field field = owner.getClass().getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(owner, value);
		} catch (Exception e) {}
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 * 
	 * @param parent
	 * @param dirPath
	 * @param cn
	 *            Class
	 * @param model
	 *            Model to add node to
	 */
	public static ASMTreeNode generateTreePath(ASMTreeNode parent, List<String> dirPath, ClassNode cn, DefaultTreeModel model) {
		ASMTreeNode ret = null;
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			ASMTreeNode node;
			// Create child if it doesn't exist.
			if ((node = parent.getChild(section)) == null) {
				ASMTreeNode newDir = ret = new ASMTreeNode(section, dirPath.size() == 1 ? cn : null);
				parent.addChild(section, newDir);
				parent.add(newDir);
				// update model
				model.nodesWereInserted(parent, new int[] { parent.getIndex(newDir) });
				node = newDir;
			}
			parent = node;
			dirPath.remove(0);
		}
		return ret;
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 * 
	 * @param parent
	 * @param dirPath
	 * @param cn
	 *            Class
	 * @param mn
	 *            Method in class
	 * @param model
	 *            Model to add node to
	 */
	public static ASMTreeNode getTreePath(ASMTreeNode parent, List<String> dirPath) {
		ASMTreeNode node = parent;
		while (dirPath.size() > 0) {
			node = node.getChild(dirPath.get(0));
			dirPath.remove(0);
		}
		return node;
	}

	/**
	 * Get or create the tree node for the given class node from the given
	 * model.
	 * 
	 * @param model
	 * @param classNode
	 * @return
	 */
	public static ASMTreeNode getOrCreateNode(DefaultTreeModel model, ClassNode classNode) {
		ASMTreeNode root = (ASMTreeNode) model.getRoot();
		ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(classNode.name.split("/")));
		ASMTreeNode genClass = generateTreePath(root, dirPath, classNode, model);
		if (genClass == null) {
			dirPath = new ArrayList<String>(Arrays.asList(classNode.name.split("/")));
			genClass = getTreePath(root, dirPath);
		}
		return genClass;
	}

}
