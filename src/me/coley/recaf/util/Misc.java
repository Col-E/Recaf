package me.coley.recaf.util;

import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import me.coley.recaf.ui.component.tree.ASMTreeNode;

/**
 * Random utility methods that don't fit in other places go here. Things here
 * should be moved elsewhere as soon as possible.
 */
public class Misc {
	/**
	 * @param s
	 *            Text to check.
	 * @return Does text represent an integer.
	 */
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

	/**
	 * @param s
	 *            Text to check.
	 * @return Does text represent a boolean.
	 */
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

	/**
	 * Sets the boolean value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set. May be a string, value is converted to boolean
	 *            regardless.
	 */
	public static void setBoolean(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isBoolean(vts)) {
			set(owner, fieldName, Boolean.parseBoolean(vts));
		}
	}

	/**
	 * Sets the integer value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set. May be a string, value is converted to int
	 *            regardless.
	 */
	public static void setInt(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isInt(vts)) {
			set(owner, fieldName, Integer.parseInt(vts));
		}
	}

	/**
	 * Sets the value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set.
	 */
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
	 * Reads all text from the file at the given path.
	 * 
	 * @param path
	 *            Path to text file.
	 * @return Text contents of file.
	 * @throws IOException
	 *             Thrown if a stream to the file could not be opened or read
	 *             from.
	 */
	public static String readFile(String path) throws IOException {
		return new String(Files.readAllBytes(Paths.get(path)), Charset.forName("utf-8"));
	}

	/**
	 * Writes the contents to the given path. File is written to in utf-8
	 * encoding.
	 * 
	 * @param path
	 *            Path to file to write to.
	 * @param content
	 *            Text contents to write.
	 * @throws IOException
	 *             Thrown if the file could not be written.
	 */
	public static void writeFile(String path, String content) throws IOException {
		Files.write(Paths.get(path), content.getBytes("utf-8"));
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 *
	 * @param parent
	 *            The parent node.
	 * @param dirPath
	 *            The node name split by package separator.
	 * @param cn
	 *            The ClassNode.
	 * @param model
	 *            Model to add node to.
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
	 *            The parent node
	 * @param dirPath
	 *            The node name split by package separator.
	 */
	private static ASMTreeNode getTreePath(ASMTreeNode parent, List<String> dirPath) {
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
	 *            The default tree model to use.
	 * @param classNode
	 *            The class node.
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

	/**
	 * Moves the insns up one in the list.
	 * 
	 * @param list
	 *            Complete list of opcodes.
	 * @param insn
	 *            Sublist to be moved.
	 */
	public static void moveUp(InsnList list, List<AbstractInsnNode> insns) {
		AbstractInsnNode prev = insns.get(0).getPrevious();
		if (prev == null) return;
		InsnList x = new InsnList();
		for (AbstractInsnNode ain : insns) {
			list.remove(ain);
			x.add(ain);
		}
		list.insertBefore(prev, x);
	}
	

	/**
	 * Moves the insns down one in the list.
	 * 
	 * @param list
	 *            Complete list of opcodes.
	 * @param insn
	 *            Sublist to be moved.
	 */
	public static void moveDown(InsnList list, List<AbstractInsnNode> insns) {
		AbstractInsnNode prev = insns.get(insns.size() - 1).getNext();
		if (prev == null) return;
		InsnList x = new InsnList();
		for (AbstractInsnNode ain : insns) {
			list.remove(ain);
			x.add(ain);
		}
		list.insert(prev, x);
	}

	/**
	 * From <a href=
	 * "https://www.java-tips.org/how-to-tile-all-internal-frames-when-requested.html">java-tips.org</a>
	 * <br>
	 * Modified to account for iconified windows.
	 * 
	 * @param desk
	 *            Desktop pane containing windows to be tiled.
	 */
	public static void tile(JDesktopPane desk) {
		// How many frames do we have?
		JInternalFrame[] frames = desk.getAllFrames();
		List<JInternalFrame> tileableFrames = new ArrayList<>();
		List<JInternalFrame> iconifiedFrames = new ArrayList<>();
		boolean icons = false;
		int count = 0;
		for (JInternalFrame frame : frames) {
			if (frame.isIcon()) {
				iconifiedFrames.add(frame);
				icons = true;
			} else {
				tileableFrames.add(frame);
				count++;
			}
		}
		if (count == 0) return;

		// Determine the necessary grid size
		int sqrt = (int) Math.sqrt(count);
		int rows = sqrt;
		int cols = sqrt;
		if (rows * cols < count) {
			cols++;
			if (rows * cols < count) {
				rows++;
			}
		}

		// Define some initial values for size & location.
		Dimension size = desk.getSize();
		int hb = size.height;
		if (icons && hb > 27) {
			hb -= 27;
		}
		int w = size.width / cols;
		int h = hb / rows;

		int x = 0;
		int y = 0;

		// Iterate over the frames and relocating & resizing each.
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols && ((i * cols) + j < count); j++) {
				JInternalFrame f = tileableFrames.get((i * cols) + j);
				desk.getDesktopManager().resizeFrame(f, x, y, w, h);
				x += w;
			}
			y += h; // start the next row
			x = 0;
		}
	}
}
