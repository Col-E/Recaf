package me.coley.recaf.ui.component.tree;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
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
import me.coley.recaf.ui.Lang;
import me.coley.recaf.util.Streams;
import me.coley.recaf.util.Swing;

/**
 * JTree wrapper for displaying the contents of a jar file.
 *
 * @author Matt
 *
 */
@SuppressWarnings("serial")
public class JarFileTree extends JPanel {
	private final JTree tree = new JTree(new String[] { Lang.get("tree.prompt") });
	private final JScrollPane scrollTree = new JScrollPane(tree);

	public JarFileTree() {
		try {
			tree.setCellRenderer(new JavaTreeRenderer());
			JavaTreeListener listener = new JavaTreeListener();
			tree.addTreeExpansionListener(listener);
			tree.addTreeSelectionListener(listener);
			tree.addMouseListener(listener);
			tree.setDropTarget(new DropTarget() {
				@Override
				public final void drop(final DropTargetDropEvent event) {
					try {
						event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
						Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if (transferData == null) {
							return;
						}
						@SuppressWarnings("unchecked")
						List<File> ls = (List<File>) transferData;
						File file = ls.get(0);
						String name = file.getName().toLowerCase();
						if (name.endsWith(".jar") || name.endsWith(".class")) {
							Recaf.INSTANCE.selectInput(file);
						} else {
							Recaf.INSTANCE.logging.error(Lang.get("tree.badinput"));
						}
					} catch (Exception ex) {
						Recaf.INSTANCE.logging.error(ex);
					}
				}
			});
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
		JarData jar = Recaf.INSTANCE.jarData;
		if (jar == null) {
			return;
		}
		// TODO: Expand new tree model to match the original
		// Root node
		String jarName = (jar.jar != null) ? jar.jar.getName() : "?";
		ASMTreeNode root = new ASMTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		// Iterate classes
		List<String> names = Streams.sortedNameList(jar.classes.keySet());
		for (String className : names) {
			if (!jar.classes.containsKey(className)) {
				continue;
			}
			ClassNode node = jar.classes.get(className);
			// Create directory path based on current node name.
			ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(node.name.split("/")));
			// Create directory of nodes
			Swing.generateTreePath(root, dirPath, node, model);
		}
		model.setRoot(root);
	}
}