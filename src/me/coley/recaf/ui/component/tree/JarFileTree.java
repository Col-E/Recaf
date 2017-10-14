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

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.util.Misc;
import me.coley.recaf.util.StreamUtil;

/**
 * JTree wrapper for displaying the contents of a jar file.
 *
 * @author Matt
 *
 */
@SuppressWarnings("serial")
public class JarFileTree extends JPanel {
	private final Recaf recaf = Recaf.getInstance();
	private final JTree tree = new JTree(new String[] { "Open a jar" });
	private final JScrollPane scrollTree = new JScrollPane(tree);

	public JarFileTree() {
		try {
			tree.setCellRenderer(new JavaTreeRenderer());
			JavaTreeListener listener = new JavaTreeListener();
			tree.addTreeExpansionListener(listener);
			tree.addTreeSelectionListener(listener);
			tree.addMouseListener(listener);
		} catch (Exception e) {
			e.printStackTrace();
		}
		setLayout(new BorderLayout());
		add(scrollTree, BorderLayout.CENTER);
	}

	/**
	 * Updates the JTree with class files loaded from the current jar.
	 */
	public void refresh() {
		JarData read = recaf.jarData;
		// Root node
		String jarName = recaf.currentJar.getName();
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
			Misc.generateTreePath(root, dirPath, node, model);
		}
		model.setRoot(root);

	}
}
