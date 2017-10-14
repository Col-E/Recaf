package me.coley.recaf.ui.component.table;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JTable;

import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.FontUtil;

/**
 * JTable populated by a list of members
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class MemberTable extends JTable {

	// TODO: Clean this up and make it a viable alternative to the list display.
	// TODO: Male alternative for List<FieldNode>
	//
	// Pros:
	// * Sortable
	//
	// Cons:
	// * Usually 2x wider than list display
	public static MemberTable create(List<MethodNode> methods) {
		String column[] = { "Index", "Name", "Descriptor", "Flags" };
		Object data[][] = new Object[methods.size()][4];
		// Determine widths of table
		int maxIndexSize = 45;
		int maxNameSize = 10;
		int maxTypeSize = 10;
		int maxAccessSize = 45;
		int padding = 1;
		for (int i = 0; i < methods.size(); i++) {
			MethodNode method = methods.get(i);
			data[i][0] = i;
			data[i][1] = method.name;
			data[i][2] = method.desc;
			data[i][3] = method.access;
			// Widths detection
			int sIndex = (int) (FontUtil.getStringBounds(data[i][0].toString(), FontUtil.monospace).getWidth());
			if (maxIndexSize < sIndex) {
				maxIndexSize = sIndex;
			}
			int sName = (int) (FontUtil.getStringBounds(data[i][1].toString(), FontUtil.monospace).getWidth());
			if (maxNameSize < sName) {
				maxNameSize = sName;
			}
			int sDesc = (int) (FontUtil.getStringBounds(data[i][2].toString(), FontUtil.monospace).getWidth());
			if (maxTypeSize < sDesc) {
				maxTypeSize = sDesc;
			}
			int sAccess = (int) (FontUtil.getStringBounds(data[i][3].toString(), FontUtil.monospace).getWidth());
			if (maxAccessSize < sAccess) {
				maxAccessSize = sAccess;
			}
		}
		MemberTable table = new MemberTable(column, data);
		table.setFont(FontUtil.monospace);
		table.getColumn("Index").setPreferredWidth(maxIndexSize + (padding * 2));
		table.getColumn("Name").setPreferredWidth(maxNameSize + (padding * 3));
		table.getColumn("Descriptor").setPreferredWidth(maxTypeSize + (padding * 4));
		table.getColumn("Flags").setPreferredWidth(maxAccessSize + (padding * 4));
		return table;
	}

	private MemberTable(String[] column, Object[][] data) {
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
