package me.coley.recaf.ui.component.list;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Program;

@SuppressWarnings("serial")
public class OpcodeList extends JList<AbstractInsnNode> {
	private static final Color colEntryBG =  new Color(200,200,200);

	/**
	 * Map of background-color overrides to be drawn by the cell renderer.
	 */
	private Map<AbstractInsnNode, Color> colorMap = new HashMap<>();

	public OpcodeList(Program callback, MethodNode mn) {
		setBackground(colEntryBG);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		DefaultListModel<AbstractInsnNode> model = new DefaultListModel<>();
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			model.addElement(ain);
		}
		setModel(model);
		setCellRenderer(new OpcodeCellRenderer(mn, callback.options));
		addListSelectionListener(new OpcodeSelectionListener(mn, callback));
	}

	/**
	 * Get the background color for the given opcode.
	 * 
	 * @param index
	 *            Opcode index.
	 * @param value
	 *            Opcode value.
	 * @return
	 */
	public Color getColorFor(int index, AbstractInsnNode value) {
		if (colorMap.containsKey(value)) {
			return colorMap.get(value);
		}
		return colEntryBG;
	}

	/**
	 * Getter for {@link #colorMap}.
	 * 
	 * @return
	 */
	public Map<AbstractInsnNode, Color> getColorMap() {
		return colorMap;
	}

}
