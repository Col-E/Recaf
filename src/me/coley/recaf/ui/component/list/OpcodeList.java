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
	private final Recaf recaf = Recaf.INSTANCE;
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

	public OpcodeList(ClassDisplayPanel display, MethodNode method) {
		this.method = method;
		setBackground(Color.decode(recaf.confTheme.listBackground));
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		DefaultListModel<AbstractInsnNode> model = new DefaultListModel<>();
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			model.addElement(ain);
		}
		setModel(model);
		setCellRenderer(new OpcodeCellRenderer(method, recaf.confUI));
		addListSelectionListener(new OpcodeSelectionListener());
		addMouseListener(new OpcodeMouseListener(method, display, this));
		int i = 1;
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.LABEL) {
				labels.put(ain, generateName(i++));
			}
		}
	}

	/**
	 * Creates a string incrementing in numerical value.
	 * 
	 * Example: a, b, c, ... z, aa, ab ...
	 * 
	 * @param index
	 *            Name index.
	 * 
	 * @return Generated String
	 */
	private static String generateName(int index) {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		char[] charz = alphabet.toCharArray();
		int alphabetLength = charz.length;
		int m = 8;
		final char[] array = new char[m];
		int n = m - 1;
		while (index > charz.length - 1) {
			int k = Math.abs(-(index % alphabetLength));
			array[n--] = charz[k];
			index /= alphabetLength;
			index -= 1;
		}
		array[n] = charz[index];
		return new String(array, n, m - n);
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
	 * @param index
	 *            Opcode index.
	 * @param value
	 *            Opcode value.
	 * @return Background color.
	 */
	public Color getColorFor(int index, AbstractInsnNode value) {
		if (colorMap.containsKey(value)) {
			return colorMap.get(value);
		}
		return Color.decode(recaf.confTheme.listItemBackground);
	}

	/**
	 * Get the appended text for the given opcode.
	 *
	 * @param index
	 *            Opcode index.
	 * @param value
	 *            Opcode value.
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
	 * @param ain
	 *            Label opcode.
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
	 * @return Map associating AbstractInsnNode instances with their colors.
	 */
	public Map<AbstractInsnNode, Color> getColorMap() {
		return colorMap;
	}

	/**
	 * Getter for {@link #appendMap}.
	 *
	 * @return Map associating AbstractInsnNode instances with their appended
	 *         text.
	 */
	public Map<AbstractInsnNode, String> getAppendMap() {
		return appendMap;
	}

	/**
	 * Getter for {@link #labels}.
	 * 
	 * @return Map associating AbstractInsnNode <i>(of LabelNode)</i> instances
	 *         with their label name.
	 */
	public Map<AbstractInsnNode, String> getLabels() {
		return labels;
	}
}
