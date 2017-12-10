package me.coley.recaf.util;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.impl.ConfUI;
import me.coley.recaf.ui.component.tree.ASMTreeNode;

public class Swing {
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

	/**
	 * So swing is a magical thing. Changing the look and feel on the fly is
	 * very finicky. After attempting to make the layout of recaf a little less
	 * ugly, I broke what worked for me before the update. Yes, calling the
	 * config's setter twice in a row is intentional. If you remove one the jar
	 * file tree's contents will be cut off despite having plenty of space to be
	 * rendered.
	 */
	public static void fixLaunchLAF() {
		new Thread() {
			@Override
			public void run() {
				try {
					ConfUI ui = Recaf.INSTANCE.configs.ui;
					ui.setLookAndFeel(ui.getLookAndFeel());
					ui.setLookAndFeel(ui.getLookAndFeel());
				} catch (Exception e) {
					Recaf.INSTANCE.logging.error(e);
				}
			}
		}.start();
	}
}
