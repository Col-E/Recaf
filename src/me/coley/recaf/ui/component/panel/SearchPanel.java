package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import org.objectweb.asm.tree.AbstractInsnNode;
import me.coley.recaf.Program;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.tree.ASMInsnTreeNode;
import me.coley.recaf.ui.component.tree.ASMMethodTreeNode;
import me.coley.recaf.ui.component.tree.ASMTreeNode;
import me.coley.recaf.ui.component.tree.JavaTreeRenderer;
import me.coley.recaf.util.Misc;
import me.coley.recaf.util.StreamUtil;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	public static final int S_STRINGS = 0;
	public static final int S_FIELD = 10;
	public static final int S_METHOD = 20;
	public static final int S_CLASS_NAME = 30, S_CLASS_REF = 31;
	private final Program callback = Program.getInstance();
	private final JTree tree = new JTree(new String[] {});

	public SearchPanel(int type) {
		setLayout(new BorderLayout());
		JPanel pnlInput = new JPanel(), pnlOutput = new JPanel();
		pnlInput.setLayout(new BoxLayout(pnlInput, BoxLayout.Y_AXIS));
		pnlOutput.setLayout(new BorderLayout());
		JScrollPane scrollTree = new JScrollPane(tree);
		pnlOutput.add(scrollTree, BorderLayout.CENTER);
		tree.setCellRenderer(new JavaTreeRenderer());
		// SearchResultTreeListener sel = new
		// SearchResultTreeListener(callback);
		//
		// tree.addTreeSelectionListener(sel);
		// tree.addMouseListener(sel);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlInput, pnlOutput);
		split.setResizeWeight(0.67);
		switch (type) {
		case S_STRINGS: {
			JTextField text;
			pnlInput.add(new LabeledComponent("String", text = new JTextField("")));
			pnlInput.add(new ActionButton("Search", () -> searchString(text.getText())));
			break;
		}
		case S_FIELD: {
			JTextField name, desc;
			pnlInput.add(new LabeledComponent("Field name", name = new JTextField("")));
			pnlInput.add(new LabeledComponent("Field desc", desc = new JTextField("")));
			pnlInput.add(new ActionButton("Search", () -> searchField(name.getText(), desc.getText())));
			break;
		}
		case S_METHOD: {
			JTextField name, desc;
			pnlInput.add(new LabeledComponent("Method name", name = new JTextField("")));
			pnlInput.add(new LabeledComponent("Method desc", desc = new JTextField("")));
			pnlInput.add(new ActionButton("Search", () -> searchMethod(name.getText(), desc.getText())));
			break;
		}
		case S_CLASS_NAME: {
			JTextField clazz;
			JCheckBox ex;
			pnlInput.add(new LabeledComponent("Class name", clazz = new JTextField("")));
			pnlInput.add(ex = new JCheckBox("Exact match"));
			pnlInput.add(new ActionButton("Search", () -> searchClass(clazz.getText(), ex.isSelected())));
			break;
		}
		case S_CLASS_REF: {
			JTextField clazz, name, desc;
			pnlInput.add(new LabeledComponent("Class owner", clazz = new JTextField("")));
			pnlInput.add(new LabeledComponent("Member name", name = new JTextField("")));
			pnlInput.add(new LabeledComponent("Member desc", desc = new JTextField("")));
			pnlInput.add(new ActionButton("Search", () -> searchClassRef(clazz.getText(), name.getText(), desc.getText())));
			break;
		}
		}
		add(split, BorderLayout.CENTER);
	}

	private void searchString(String text) {
		DefaultTreeModel model = setup();
		search((n) -> {
			for (MethodNode m : n.methods) {
				for (AbstractInsnNode ain : m.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.LDC_INSN) {
						LdcInsnNode ldc = (LdcInsnNode) ain;
						String s = ldc.cst.toString();
						if (s.contains(text)) {
							// Get tree node for class
							ASMTreeNode genClass = Misc.getOrCreateNode(model, n);

							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(m.name);
							if (genMethod == null) {
								genClass.addChild(m.name, genMethod = new ASMMethodTreeNode(m.name + m.desc, n, m));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(m.instructions.indexOf(ain) + ": '" + s + "'", n, m, ain));
						}
					}
				}
			}
		});
		tree.setModel(model);
	}

	private void searchField(String name, String desc) {
		DefaultTreeModel model = setup();
		search((n) -> {
			for (FieldNode f : n.fields) {
				if (f.name.contains(name) && f.desc.contains(desc)) {
					ASMTreeNode genClass = Misc.getOrCreateNode(model, n);
					ASMTreeNode genMethod = genClass.getChild(f.name);
					if (genMethod == null) {
						genMethod = new ASMTreeNode(f.desc + " " + f.name, n);
					}
					genClass.add(genMethod);
				}
			}
		});
		tree.setModel(model);
	}

	private void searchMethod(String name, String desc) {
		DefaultTreeModel model = setup();
		search((n) -> {
			for (MethodNode m : n.methods) {
				if (m.name.contains(name) && m.desc.contains(desc)) {
					ASMTreeNode genClass = Misc.getOrCreateNode(model, n);
					ASMTreeNode genMethod = genClass.getChild(m.name);
					if (genMethod == null) {
						genMethod = new ASMTreeNode(m.name + m.desc, n);
					}
					genClass.add(genMethod);
				}
			}
		});
		tree.setModel(model);
	}

	private void searchClass(String text, boolean exact) {
		DefaultTreeModel model = setup();
		search((n) -> {
			if (exact ? n.name.equals(text) : n.name.contains(text)) {
				Misc.getOrCreateNode(model, n);
			}
		});
		tree.setModel(model);
	}

	private void searchClassRef(String owner, String name, String desc) {
		DefaultTreeModel model = setup();
		search((n) -> {
			for (MethodNode m : n.methods) {
				for (AbstractInsnNode ain : m.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
						FieldInsnNode fin = (FieldInsnNode) ain;
						if (fin.owner.equals(owner) && fin.name.equals(name) && fin.desc.equals(desc)) {
							System.out.println("A");
							ASMTreeNode genClass = Misc.getOrCreateNode(model, n);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(m.name);
							if (genMethod == null) {
								genClass.addChild(m.name, genMethod = new ASMMethodTreeNode(m.name + m.desc, n, m));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(m.instructions.indexOf(ain) + ": " + name, n, m, ain));
						} 
					} else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) ain;
						if (min.owner.equals(owner) && min.name.equals(name) && min.desc.equals(desc)) {
							// Get tree node for class
							ASMTreeNode genClass = Misc.getOrCreateNode(model, n);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(m.name);
							if (genMethod == null) {
								genClass.addChild(m.name, genMethod = new ASMMethodTreeNode(m.name + m.desc, n, m));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(m.instructions.indexOf(ain) + ": " + name, n, m, ain));
						}
					}
				}
			}
		});
		tree.setModel(model);
	}

	/**
	 * Setup and return the tree model for a search.
	 * 
	 * @return
	 */
	private DefaultTreeModel setup() {
		String jarName = callback.currentJar.getName();
		ASMTreeNode root = new ASMTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		model.setRoot(root);
		return model;
	}

	/**
	 * Search and pass classnodes through the given function.
	 * 
	 * @param model
	 * @param func
	 */
	private void search(Consumer<ClassNode> func) {
		List<String> names = StreamUtil.listOfSortedJavaNames(callback.jarData.classes.keySet());
		for (String className : names) {
			ClassNode node = callback.jarData.classes.get(className);
			func.accept(node);
		}
	}
}
