package me.coley.recaf.ui.component.tree;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Program;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.util.StreamUtil;

@SuppressWarnings("serial")
public class FileTree extends JPanel {
	private final JTree tree = new JTree(new String[] { "Open a jar" });
	private final JScrollPane scrollTree = new JScrollPane(tree);
	private final Program callback;

	public FileTree(Program callback) {
		this.callback = callback;
		//
		try {
			tree.setCellRenderer(new FileTreeRenderer());
			FileTreeListener listener = new FileTreeListener(callback);
			tree.addTreeSelectionListener(listener);
			tree.addMouseListener(listener);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		setLayout(new BorderLayout());
		add(scrollTree, BorderLayout.CENTER);
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 * 
	 * @param parent
	 * @param dirPath
	 * @param cn
	 * @param model
	 */
	private void generateTreePath(ASMTreeNode parent, List<String> dirPath, ClassNode cn,
			DefaultTreeModel model) {
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			ASMTreeNode node;
			// Create child if it doesn't exist.
			if ((node = parent.getChild(section)) == null) {
				ASMTreeNode newDir = new ASMTreeNode(section, dirPath.size() == 1 ? cn : null);
				parent.addChild(section, newDir);
				parent.add(newDir);
				// update model
				model.nodesWereInserted(parent, new int[] { parent.getIndex(newDir) });
				node = newDir;
			}
			parent = node;
			dirPath.remove(0);
		}
	}

	/**
	 * Updates the JTree with class files loaded from the current jar.
	 */
	public void refresh() {
		JarData read = callback.jarData;
		// Root node
		String jarName = callback.currentJar.getName();
		ASMTreeNode root = new ASMTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		// Iterate classes
		List<String> names = StreamUtil.listOfSortedJavaNames(read.classes.keySet());
		for (String className : names) {
			if (!read.classes.containsKey(className)) {
				continue;
			}
			ClassNode node = read.classes.get(className);
			// Create directory path based on current node name.
			ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(node.name.split("/")));
			// Create directory of nodes
			generateTreePath(root, dirPath, node, model);
		}
		model.setRoot(root);

	}
}
