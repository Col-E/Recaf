package me.coley.recaf.ui.component;

import java.awt.Dimension;

import javax.swing.JTable;

import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.FontUtil;

@SuppressWarnings("serial")
public class VariableTable extends JTable {
	/**
	 * Construct a local variable table from the given method.
	 * 
	 * @param method
	 * @return
	 */
	public static VariableTable create(MethodNode method) {
		String column[] = { "Index", "Name", "Type" };
		String data[][] = new String[method.maxLocals][3];
		// Determine widths of table
		int maxIndexSize = 45;
		int maxNameSize = 10;
		int maxTypeSize = 10;
		int padding = 10;
		for (int i = 0; i < method.maxLocals; i++) {
			// Raw indices
			data[i][0] = String.valueOf(i);
			int sIndex = (int) (FontUtil.getStringBounds(data[i][0], FontUtil.monospace).getWidth());
			if (maxIndexSize < sIndex) {
				maxIndexSize = sIndex;
			}
			// If possible, add data from local variable table.
			if (method.localVariables != null) {
				LocalVariableNode variable = method.localVariables.get(i);
				data[i][1] = variable.name;
				data[i][2] = variable.desc;
				int sName = (int) (FontUtil.getStringBounds(data[i][1], FontUtil.monospace).getWidth());
				int sDesc = (int) (FontUtil.getStringBounds(data[i][2], FontUtil.monospace).getWidth());
				if (maxNameSize < sName) {
					maxNameSize = sName;
				}
				if (maxTypeSize < sDesc) {
					maxTypeSize = sDesc;
				}
			}
		}
		VariableTable table = new VariableTable(column, data);
		table.setFont(FontUtil.monospace);
		table.getColumn("Index").setPreferredWidth(maxIndexSize + (padding * 2));
		table.getColumn("Name").setPreferredWidth(maxNameSize + (padding * 3));
		table.getColumn("Type").setPreferredWidth(maxTypeSize + (padding * 4));
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
