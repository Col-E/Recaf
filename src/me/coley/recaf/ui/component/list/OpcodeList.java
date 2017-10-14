package me.coley.recaf.ui.component.list;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

@SuppressWarnings("serial")
public class OpcodeList extends JList<AbstractInsnNode> {

	private final Recaf recaf = Recaf.getInstance();
	private static final Color colEntryBG = new Color(200, 200, 200);
	private static final Color colListBG = new Color(166, 166, 166);
	/**
	 * The method being viewed.
	 */
	private final MethodNode method;
	/**
	 * Map of background-color overrides to be drawn by the cell renderer.
	 */
	private Map<AbstractInsnNode, Color> colorMap = new HashMap<>();
	/**
	 * Map of label opcodes to some label name.
	 */
	private final Map<AbstractInsnNode, String> labels = new HashMap<>();
	/**
	 * Map of appended text to be added to the cell renderer.
	 */
	private Map<AbstractInsnNode, String> appendMap = new HashMap<>();
	/**
	 * Key modifiers
	 */
	public boolean controlDown, shiftDown;

	public OpcodeList(ClassDisplayPanel display, MethodNode method) {
		this.method = method;
		setBackground(colListBG);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		DefaultListModel<AbstractInsnNode> model = new DefaultListModel<>();
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			model.addElement(ain);
		}
		setModel(model);
		setCellRenderer(new OpcodeCellRenderer(method, recaf.options));
		addListSelectionListener(new OpcodeSelectionListener());
		addMouseListener(new OpcodeMouseListener(method, display, this));
		addKeyListener(new OpcodeKeyListener(this));
		int i = 1;
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.LABEL) {
				labels.put(ain, Integer.toHexString(i++));
			}
		}
	}

	/**
	 * Re-populate the model.
	 */
	public void repopulate() {
		DefaultListModel<AbstractInsnNode> model = (DefaultListModel<AbstractInsnNode>) getModel();
		model.clear();
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			model.addElement(ain);
		}
		setModel(model);
	}

	/**
	 * Get the background color for the given opcode.
	 *
	 * @param index Opcode index.
	 * @param value Opcode value.
	 * @return Background color.
	 */
	public Color getColorFor(int index, AbstractInsnNode value) {
		if (colorMap.containsKey(value)) {
			return colorMap.get(value);
		}
		return colEntryBG;
	}

	/**
	 * Get the appended text for the given opcode.
	 *
	 * @param index Opcode index.
	 * @param value Opcode value.
	 * @return appended text.
	 */
	public String getAppendFor(int index, AbstractInsnNode value) {
		if (appendMap.containsKey(value)) {
			return appendMap.get(value);
		}
		return "";
	}

	/**
	 * Get the arbitrary label identifier for the given label opcode.
	 *
	 * @param ain Label opcode.
	 * @return label identifier.
	 */
	public String getLabelName(AbstractInsnNode ain) {
		return labels.getOrDefault(ain, "(New Label)");
	}

	/**
	 * Get the method node associated with the list.
	 *
	 * @return the method node.
	 */
	public MethodNode getMethod() {
		return method;
	}

	/**
	 * Getter for {@link #colorMap}.
	 *
	 * @return Map associating AbstractInsnNode instances with their
	 * colors.
	 */
	public Map<AbstractInsnNode, Color> getColorMap() {
		return colorMap;
	}

	/**
	 * Getter for {@link #appendMap}.
	 *
	 * @return Map associated AbstractInsnNode instances with their
	 * appended text.
	 */
	public Map<AbstractInsnNode, String> getAppendMap() {
		return appendMap;
	}

	/**
	 * Set key-modifiers.
	 *
	 * @param control TODO
	 * @param shift TODO
	 *
	 * TODO: What do control and shift correspond to? What is this method
	 * for?
	 *
	 *	- Charles
	 */
	public void setModifiers(boolean control, boolean shift) {
		this.controlDown = control;
		this.shiftDown = shift;
	}
}
