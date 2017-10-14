package me.coley.recaf.ui.component.table;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JTable;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Options;
import me.coley.recaf.Recaf;
import me.coley.recaf.ui.FontUtil;
import me.coley.recaf.ui.component.list.OpcodeList;

/**
 * JTable populated by local variable data from a given MethodNode.<br>
 * Constructed via {@link #create(OpcodeList, MethodNode)}.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class VariableTable extends JTable {
	private static final int INDEX = 0, NAME = 1, DESC = 2, SIGNATURE = 3;
	private static final Options options = Recaf.getInstance().options;

	/**
	 * Construct a local variable table from the given method.
	 *
	 * @param list List of opcodes.
	 *
	 * @param method The method.
	 * @return VariableTable containing the local variables for the method.
	 */
	public static VariableTable create(OpcodeList list, MethodNode method) {
		boolean showSignatures = options.showVariableSignatureInTable;
		int max = showSignatures ? 4 : 3;
		String column[] = { "Index", "Name", "Descriptor", "Signature" };
		if (!showSignatures) {
			// If not showing signatures, remove last column.
			column = Arrays.copyOf(column, max);
		}
		int locals = method.localVariables != null ? method.localVariables.size() : method.maxLocals;
		String data[][] = new String[locals][max];
		// Determine widths of table
		int maxIndexSize = 45;
		int maxNameSize = 10;
		int maxTypeSize = 10;
		int maxSigSize = 10;
		int padding = 10;
		for (int i = 0; i < locals; i++) {
			// Raw indices
			data[i][INDEX] = String.valueOf(i);
			int sIndex = (int) (FontUtil.getStringBounds(data[i][0], FontUtil.monospace).getWidth());
			if (maxIndexSize < sIndex) {
				maxIndexSize = sIndex;
			}
			// If possible, add data from local variable table.
			if (method.localVariables != null && i < method.localVariables.size()) {
				LocalVariableNode variable = method.localVariables.get(i);
				data[i][NAME] = variable.name;
				data[i][DESC] = variable.desc;
				int sName = (int) (FontUtil.getStringBounds(data[i][1], FontUtil.monospace).getWidth());
				int sDesc = (int) (FontUtil.getStringBounds(data[i][2], FontUtil.monospace).getWidth());
				if (maxNameSize < sName) {
					maxNameSize = sName;
				}
				if (maxTypeSize < sDesc) {
					maxTypeSize = sDesc;
				}
				// Signature
				if (showSignatures) {
					data[i][SIGNATURE] = variable.signature == null ? "" : variable.signature;
					int sSign = (int) (FontUtil.getStringBounds(data[i][3], FontUtil.monospace).getWidth());
					if (maxSigSize < sSign) {
						maxSigSize = sSign;
					}
				}
			}
		}
		VariableTable table = new VariableTable(column, data);
		table.setFont(FontUtil.monospace);
		table.getColumn(column[INDEX]).setPreferredWidth(maxIndexSize + (padding * 2));
		table.getColumn(column[NAME]).setPreferredWidth(maxNameSize + (padding * 3));
		table.getColumn(column[DESC]).setPreferredWidth(maxTypeSize + (padding * 4));
		if (showSignatures) {
			table.getColumn(column[SIGNATURE]).setPreferredWidth(maxSigSize + (padding * 4));
		}
		table.setCellSelectionEnabled(true);
		if (method.localVariables != null) {
			table.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						int row = table.getSelectedRow();
						int col = table.getSelectedColumn();
						String value = table.getValueAt(row, col).toString();
						switch (col) {
						case INDEX:
							break;
						case NAME:
							method.localVariables.get(row).name = value;
							list.repaint();
							break;
						case DESC:
							method.localVariables.get(row).desc = value;
							list.repaint();
							break;
						case SIGNATURE:
							method.localVariables.get(row).signature = value;
							list.repaint();
							break;
						}
					}
				}
			});
		}
		return table;
	}

	private VariableTable(String[] column, String[][] data) {
		super(data, column);
	}

	/**
	 * Override the getPrefferedScrollableViewportSize to prevent the wrapping
	 * scroll pane from adding lots of pointless empty space near the bottom of
	 * the container.
	 *
	 * Credits: https://stackoverflow.com/a/42436205/8071915
	 */
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension(super.getPreferredSize().width, getRowHeight() * getRowCount());
	}
}
