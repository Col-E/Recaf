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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import me.coley.recaf.Recaf;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.component.EnumCombobox;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.tree.ASMFieldTreeNode;
import me.coley.recaf.ui.component.tree.ASMInsnTreeNode;
import me.coley.recaf.ui.component.tree.ASMMethodTreeNode;
import me.coley.recaf.ui.component.tree.ASMTreeNode;
import me.coley.recaf.ui.component.tree.JavaTreeListener;
import me.coley.recaf.ui.component.tree.JavaTreeRenderer;
import me.coley.recaf.util.Misc;
import me.coley.recaf.util.StreamUtil;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	private static final String[] DEFAULT = new String[5];
	private final Recaf recaf = Recaf.INSTANCE;
	private final JTree tree = new JTree(new String[] {});

	public SearchPanel(SearchType type, String[] defaults) {
		setLayout(new BorderLayout());
		// Lazy array size exception prevention
		if (defaults.length == 0) {
			defaults = DEFAULT;
		}
		JPanel pnlOutput = new JPanel();
		JPanel pnlInput = new JPanel();
		pnlInput.setLayout(new BoxLayout(pnlInput, BoxLayout.Y_AXIS));
		pnlOutput.setLayout(new BorderLayout());
		JScrollPane scrollTree = new JScrollPane(tree);
		pnlOutput.add(scrollTree, BorderLayout.CENTER);
		JavaTreeListener sel = new JavaTreeListener();
		tree.setCellRenderer(new JavaTreeRenderer());
		tree.addTreeSelectionListener(sel);
		tree.addMouseListener(sel);
		tree.addTreeExpansionListener(sel);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlInput, pnlOutput);
		split.setResizeWeight(0.67);
		ActionButton btn = null;
		// @formatter:off
		switch (type) {
		case LDC_STRING: {
			JTextField text;
			EnumCombobox<StringSearchType> enumCombo = new EnumCombobox<StringSearchType>(StringSearchType.values()) {
				@Override
				protected String getText(StringSearchType value) {
					return value.getDisplay();
				}
			};
			pnlInput.add(new LabeledComponent("String", text = new JTextField(defaults[0])));
			pnlInput.add(new LabeledComponent("Search type", enumCombo));
			pnlInput.add(btn = new ActionButton("Search", () -> searchString(text.getText(), enumCombo.getEnumSelection())));
			break;
		}
		case CONSTANT: {
			JTextField text;
			pnlInput.add(new LabeledComponent("Value", text = new JTextField(defaults[0])));
			pnlInput.add(btn = new ActionButton("Search", () -> searchConstant(text.getText())));
			break;
		}
		case DECLARED_FIELD: {
			JTextField name, desc;
			JCheckBox ex;
			pnlInput.add(new LabeledComponent("Field name", name = new JTextField(defaults[0])));
			pnlInput.add(new LabeledComponent("Field desc", desc = new JTextField(defaults[1])));
			pnlInput.add(ex = new JCheckBox("Exact match", Boolean.parseBoolean(defaults[3])));
			pnlInput.add(btn = new ActionButton("Search", () -> searchField(name.getText(), desc.getText(), ex.isSelected())));
			break;
		}
		case DECLARED_METHOD: {
			JTextField name, desc;
			JCheckBox ex;
			pnlInput.add(new LabeledComponent("Method name", name = new JTextField(defaults[0])));
			pnlInput.add(new LabeledComponent("Method desc", desc = new JTextField(defaults[1])));
			pnlInput.add(ex = new JCheckBox("Exact match", Boolean.parseBoolean(defaults[3])));
			pnlInput.add(btn = new ActionButton("Search", () -> searchMethod(name.getText(), desc.getText(), ex.isSelected())));
			break;
		}
		case DECLARED_CLASS: {
			JTextField clazz;
			JCheckBox ex;
			pnlInput.add(new LabeledComponent("Class name", clazz = new JTextField(defaults[0])));
			pnlInput.add(ex = new JCheckBox("Exact match", Boolean.parseBoolean(defaults[1])));
			pnlInput.add(btn = new ActionButton("Search", () -> searchClass(clazz.getText(), ex.isSelected())));
			break;
		}
		case REFERENCES: {
			JTextField clazz, name, desc;
			JCheckBox ex;
			pnlInput.add(new LabeledComponent("Class owner", clazz = new JTextField(defaults[0])));
			pnlInput.add(new LabeledComponent("Member name", name = new JTextField(defaults[1])));
			pnlInput.add(new LabeledComponent("Member desc", desc = new JTextField(defaults[2])));
			pnlInput.add(ex = new JCheckBox("Exact match", Boolean.parseBoolean(defaults[3])));
			pnlInput.add(btn = new ActionButton("Search", () -> searchClassRef(clazz.getText(), name.getText(), desc.getText(), ex
					.isSelected())));
			break;
		}
		}
		// @formatter:on
		add(split, BorderLayout.CENTER);
		// Defaults not given, implied the search was intended from
		// instantiation.
		if (defaults != DEFAULT) {
			btn.doClick();
		}
	}

	private void searchString(String text, StringSearchType type) {
		DefaultTreeModel model = setup();
		search(cn -> {
			for (MethodNode mn : cn.methods) {
				for (AbstractInsnNode ain : mn.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.LDC_INSN) {
						LdcInsnNode ldc = (LdcInsnNode) ain;
						if (!(ldc.cst instanceof String)) {
							continue;
						}
						String cst = (String) ldc.cst;
						boolean contains = false;
						switch (type) {
						case CASE_INSENSITIVE:
							contains = cst.toLowerCase().contains(text);
							break;
						case CASE_SENSITIVE:
							contains = cst.contains(text);
							break;
						case EXACT:
							contains = cst.equals(text);
							break;
						case REGEX:
							contains = cst.matches(text);
							break;
						}
						if (contains) {
							// Get tree node for class
							ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(mn.name);
							if (genMethod == null) {
								genClass.addChild(mn.name, genMethod = new ASMMethodTreeNode(mn.name + mn.desc, cn, mn));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(mn.instructions.indexOf(ain) + ": '" + cst + "'", cn, mn, ain));
						}
					}
				}
			}
		});
		setTreeModel(model);
	}

	private void searchConstant(String text) {
		try {
			int value = Integer.valueOf(text);
			DefaultTreeModel model = setup();
			search(cn -> {
				// Search values of final fields
				for (FieldNode f : cn.fields) {
					if (f.value != null && f.value instanceof Number) {
						int defaultValue = ((Number) f.value).intValue();
						if (value == defaultValue) {
							ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
							ASMTreeNode genMethod = genClass.getChild(f.name);
							if (genMethod == null) {
								genMethod = new ASMFieldTreeNode(f.desc + " " + f.name, cn, f);
							}
							genClass.add(genMethod);
						}
					}
				}
				// Search LDC and int opcode types for the given value
				for (MethodNode mn : cn.methods) {
					for (AbstractInsnNode ain : mn.instructions.toArray()) {
						boolean match = false;
						if (ain.getType() == AbstractInsnNode.INSN) {
							match = (OpcodeUtil.getValue(ain.getOpcode()) == value);
						} else if (ain.getType() == AbstractInsnNode.INT_INSN) {
							match = ((IntInsnNode)ain).operand == value;
						} else if (ain.getType() == AbstractInsnNode.LDC_INSN) {
							LdcInsnNode ldc = (LdcInsnNode) ain;
							if (!(ldc.cst instanceof Number)) {
								continue;
							}
							int cst = ((Number) ldc.cst).intValue();
							match = (value == cst);
						}
						if (match) {
							// Get tree node for class
							ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(mn.name);
							if (genMethod == null) {
								genClass.addChild(mn.name, genMethod = new ASMMethodTreeNode(mn.name + mn.desc, cn, mn));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(mn.instructions.indexOf(ain) + ": '" + value + "'", cn, mn,
									ain));
						}
					}
				}
			});
			setTreeModel(model);
		} catch (NumberFormatException e) {
			return;
		}
	}

	private void searchField(String name, String desc, boolean exact) {
		DefaultTreeModel model = setup();
		search((cn) -> {
			for (FieldNode f : cn.fields) {
				boolean match = false;
				if (exact) {
					match = f.name.equals(name) && f.desc.equals(desc);
				} else {
					match = f.name.contains(name) && f.desc.contains(desc);
				}
				if (match) {
					ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
					ASMTreeNode genMethod = genClass.getChild(f.name);
					if (genMethod == null) {
						genMethod = new ASMFieldTreeNode(f.desc + " " + f.name, cn, f);
					}
					genClass.add(genMethod);
				}
			}
		});
		setTreeModel(model);
	}

	private void searchMethod(String name, String desc, boolean exact) {
		DefaultTreeModel model = setup();
		search((cn) -> {
			for (MethodNode m : cn.methods) {
				boolean match = false;
				if (exact) {
					match = m.name.equals(name) && m.desc.equals(desc);
				} else {
					match = m.name.contains(name) && m.desc.contains(desc);
				}
				if (match) {
					ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
					ASMTreeNode genMethod = genClass.getChild(m.name);
					if (genMethod == null) {
						genMethod = new ASMMethodTreeNode(m.name + m.desc, cn, m);
					}
					genClass.add(genMethod);
				}
			}
		});
		setTreeModel(model);
	}

	private void searchClass(String text, boolean exact) {
		DefaultTreeModel model = setup();
		search((cn) -> {
			if (exact ? cn.name.equals(text) : cn.name.contains(text)) {
				Misc.getOrCreateNode(model, cn);
			}
		});
		setTreeModel(model);
	}

	private void searchClassRef(String owner, String name, String desc, boolean exact) {
		DefaultTreeModel model = setup();
		search((cn) -> {
			for (MethodNode m : cn.methods) {
				for (AbstractInsnNode ain : m.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
						FieldInsnNode fin = (FieldInsnNode) ain;
						if ((exact && (fin.owner.equals(owner) && fin.name.equals(name) && fin.desc.equals(desc))) || (!exact
								&& (fin.owner.contains(owner) && fin.name.contains(name) && fin.desc.contains(desc)))) {
							ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(m.name);
							if (genMethod == null) {
								genClass.addChild(m.name, genMethod = new ASMMethodTreeNode(m.name + m.desc, cn, m));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(m.instructions.indexOf(ain) + ": " + fin.name, cn, m, ain));
						}
					} else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode min = (MethodInsnNode) ain;
						if ((exact && (min.owner.equals(owner) && min.name.equals(name) && min.desc.equals(desc))) || (!exact
								&& (min.owner.contains(owner) && min.name.contains(name) && min.desc.contains(desc)))) {
							// Get tree node for class
							ASMTreeNode genClass = Misc.getOrCreateNode(model, cn);
							// Get or create tree node for method
							ASMTreeNode genMethod = genClass.getChild(m.name);
							if (genMethod == null) {
								genClass.addChild(m.name, genMethod = new ASMMethodTreeNode(m.name + m.desc, cn, m));
								genClass.add(genMethod);
							}
							// Add opcode node to method tree node
							genMethod.add(new ASMInsnTreeNode(m.instructions.indexOf(ain) + ": " + min.name, cn, m, ain));
						}
					}
				}
			}
		});
		setTreeModel(model);
	}

	private void setTreeModel(DefaultTreeModel model) {
		tree.setModel(model);
		expandAllNodes(tree, 0, tree.getRowCount());
	}

	/**
	 * Setup and return the tree model for a search.
	 *
	 * @return
	 */
	private DefaultTreeModel setup() {
		String jarName = recaf.currentJar.getName();
		ASMTreeNode root = new ASMTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		model.setRoot(root);
		return model;
	}

	private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			tree.expandRow(i);
		}
		if (tree.getRowCount() != rowCount) {
			expandAllNodes(tree, rowCount, tree.getRowCount());
		}
	}

	/**
	 * Search and pass classnodes through the given function.
	 *
	 * @param model
	 * @param func
	 */
	private void search(Consumer<ClassNode> func) {
		List<String> names = StreamUtil.listOfSortedJavaNames(recaf.jarData.classes.keySet());
		for (String className : names) {
			ClassNode node = recaf.jarData.classes.get(className);
			func.accept(node);
		}
	}

	/**
	 * Search type.
	 * 
	 * @author Matt
	 */
	public static enum SearchType {
		LDC_STRING, CONSTANT, DECLARED_FIELD, DECLARED_METHOD, DECLARED_CLASS, REFERENCES
	}

	/**
	 * String sub-search type.
	 * 
	 * @author Matt
	 */
	public static enum StringSearchType {
		CASE_INSENSITIVE("Case insensitive"), CASE_SENSITIVE("Case sensitive"), EXACT("Exact"), REGEX("Regex");
		private final String display;

		private StringSearchType(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}
}
