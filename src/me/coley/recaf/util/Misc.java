package me.coley.recaf.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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

	public static void setBoolean(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isBoolean(vts)) {
			set(owner, fieldName, Boolean.parseBoolean(vts));
		}
	}

	public static void setInt(Object owner, String fieldName, Object value) {
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

	public static String readFile(String path) throws IOException {
		return new String(Files.readAllBytes(Paths.get(path)), Charset.forName("utf-8"));
	}

	public static void writeFile(String path, String content) throws IOException {
		Files.write(Paths.get(path), content.getBytes("utf-8"));
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 *
	 * @param parent The parent node.
	 * @param dirPath TODO
	 * @param cn Class
	 * @param model Model to add node to
	 *
	 * TODO: document dirPath param; I'm not sure what it's for.
	 *
	 *	- Charles
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
	 * @param parent The parent node
	 * @param dirPath TODO
	 *
	 * TODO: document dirPath param; I'm not sure what it's for.
	 *
	 *	- Charles
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
	 * @param model The default tree model to use.
	 * @param classNode The class node.
	 * @return The tree node.
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
