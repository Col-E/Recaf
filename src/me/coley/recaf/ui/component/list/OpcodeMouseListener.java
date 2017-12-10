package me.coley.recaf.ui.component.list;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.LabeledComponentGroup;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.internalframe.BlockInsertBox;
import me.coley.recaf.ui.component.internalframe.BlockSaveBox;
import me.coley.recaf.ui.component.internalframe.EditBox;
import me.coley.recaf.ui.component.internalframe.OpcodeCreationBox;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.LabelSwitcherPanel;
import me.coley.recaf.ui.component.panel.OpcodeTypeSwitchPanel;
import me.coley.recaf.ui.component.panel.TagTypeSwitchPanel;
import me.coley.recaf.ui.component.table.VariableTable;
import me.coley.recaf.util.Parse;
import me.coley.recaf.util.Reflect;

public class OpcodeMouseListener extends MouseAdapter {
	private final Recaf recaf = Recaf.INSTANCE;
	private final MethodNode method;
	private final OpcodeList list;
	private final ClassDisplayPanel display;

	public OpcodeMouseListener(MethodNode method, ClassDisplayPanel display, OpcodeList list) {
		this.method = method;
		this.display = display;
		this.list = list;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int button = e.getButton();
		if (list.getSelectedIndices().length <= 1) {
			// If not left-click, enforce selection at the given location
			if (button != MouseEvent.BUTTON1) {
				int index = list.locationToIndex(e.getPoint());
				list.setSelectedIndex(index);
			}
		}
		Object value = list.getSelectedValue();
		if (value == null) {
			return;
		}
		if (button == MouseEvent.BUTTON3) {
			createContextMenu((AbstractInsnNode) value, e.getX(), e.getY());
		} else if (button == MouseEvent.BUTTON2) {
			createEdit((AbstractInsnNode) value, e.getX(), e.getY());
		}
	}

	private void createContextMenu(AbstractInsnNode ain, int x, int y) {
		JPopupMenu popup = new JPopupMenu();
		ActionMenuItem itemEdit = new ActionMenuItem("Edit", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createEdit(ain, x, y);
			}
		}));
		ActionMenuItem itemNewBefore = new ActionMenuItem("New Opcode Before...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new OpcodeCreationBox(true, list, method, ain));
			}
		}));
		ActionMenuItem itemNewAfter = new ActionMenuItem("New Opcode After...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new OpcodeCreationBox(false, list, method, ain));
			}
		}));
		ActionMenuItem itemUp = new ActionMenuItem("Move Up", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Asm.moveUp(list.getMethod().instructions, list.getSelectedValuesList());
				list.repopulate();
			}
		}));
		ActionMenuItem itemDown = new ActionMenuItem("Move Down", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Asm.moveDown(list.getMethod().instructions, list.getSelectedValuesList());
				list.repopulate();
			}
		}));
		ActionMenuItem itemSave = new ActionMenuItem("Save Opcodes As Block...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new BlockSaveBox(list.getSelectedValuesList()));
			}
		}));
		ActionMenuItem itemInsert = new ActionMenuItem("Insert Block...", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display.addWindow(new BlockInsertBox(method.instructions, list));
			}
		}));
		ActionMenuItem itemRemove = new ActionMenuItem("Remove", (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (recaf.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, "You sure you want to delete that opcode?", "Warning",
							JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
				DefaultListModel<AbstractInsnNode> model = (DefaultListModel<AbstractInsnNode>) list.getModel();
				int[] ascending = list.getSelectedIndices();
				if (ascending.length > 1) {
					// Create a list of ranges of opcodes to remove
					List<Range> ranges = new ArrayList<>();
					// Temp variables for storing current range info
					int startIndex = ascending[0], lastIndex = -1;
					for (int i = 1; i < ascending.length; i++) {
						// If the gap between current and last indices is > 1, there is a gap.
						int currentIndex = ascending[i];
						if (lastIndex - currentIndex != -1) {
							// Mark end of range due to gap detection.
							// End is previous since current is the start of the next range.
							ranges.add(new Range(startIndex, ascending[i - 1]));
							startIndex = currentIndex;
						}
						lastIndex = currentIndex;
					}
					// Finish last range
					ranges.add(new Range(startIndex, lastIndex));
					// Sort so ranges are iterated from last appearence to first appearence.
					// Makes removal easier so accounting for offsets isn't an issue.
					Collections.sort(ranges);
					for (Range range : ranges) {
						model.removeRange(range.start, range.end);
						AbstractInsnNode insnStart = method.instructions.get(range.start);
						AbstractInsnNode insnEnd = method.instructions.get(range.end);
						link(insnStart, insnEnd);
					}
					// Decrement method.instructions size
					setSize(model.size());
				} else {
					// Remove singular instruction
					model.remove(list.getSelectedIndex());
					method.instructions.remove(ain);
				}
			}

			/**
			 * Links two given insns together via their linked list's previous
			 * and next values.
			 * 
			 * @param insnStart
			 * @param insnEnd
			 */
			private void link(AbstractInsnNode insnStart, AbstractInsnNode insnEnd) {
				try {
					boolean first = method.instructions.getFirst().equals(insnStart);
					Field next = AbstractInsnNode.class.getDeclaredField("next");
					Field prev = AbstractInsnNode.class.getDeclaredField("prev");
					next.setAccessible(true);
					prev.setAccessible(true);
					if (first) {
						// Update head
						Field listStart = InsnList.class.getDeclaredField("first");
						listStart.setAccessible(true);
						listStart.set(method.instructions, insnEnd.getNext());
						// Remove link to previous sections
						prev.set(insnEnd.getNext(), null);
					} else {
						// insnStart.prev links to insnEnd.next
						next.set(insnStart.getPrevious(), insnEnd.getNext());
						prev.set(insnEnd.getNext(), insnStart.getPrevious());
					}
				} catch (Exception e) {}
			}

			/**
			 * Sets the InsnList size through reflection since insns were cut
			 * out of the list through reflection and not the given methods.
			 * It's ugly but it makes a MASSIVE performance boost to do it this
			 * way.
			 * 
			 * @param size
			 *            New method instructions size.
			 */
			private void setSize(int size) {
				try {
					Field f = InsnList.class.getDeclaredField("size");
					f.setAccessible(true);
					f.setInt(method.instructions, size);
				} catch (Exception e) {}
			}

			/**
			 * Utility class for creating comparable ranges of opcode indices.
			 * 
			 * @author Matt
			 */
			class Range implements Comparable<Range> {
				int start, end;

				public Range(int start, int end) {
					this.start = start;
					this.end = end;
				}

				@Override
				public int compareTo(Range r) {
					if (start > r.start) return -1;
					else if (start < r.start) return 1;
					return 0;
				}
			}
		}));
		if (list.getSelectedIndices().length == 1) {
			popup.add(itemEdit);
			popup.add(itemUp);
			popup.add(itemDown);
			popup.add(itemNewBefore);
			popup.add(itemNewAfter);
			popup.add(itemInsert);
		} else {
			popup.add(itemUp);
			popup.add(itemDown);
			popup.add(itemSave);
			popup.add(itemInsert);
		}
		popup.add(itemRemove);
		popup.show(list, x, y);
	}

	//@formatter:off
	private void createEdit(AbstractInsnNode ain, int x, int y) {
		EditBox frame = new EditBox("Opcode: " + OpcodeUtil.opcodeToName(ain.getOpcode()));
		switch (ain.getType()) {
		case AbstractInsnNode.INT_INSN:
			IntInsnNode insnInt = (IntInsnNode) ain;
			frame.add(new LabeledComponent("Value: ", new ActionTextField(insnInt.operand, s -> {
				if (Parse.isInt(s)) {
					insnInt.operand = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.VAR_INSN:
			VarInsnNode insnVar = (VarInsnNode) ain;
			frame.add(new JScrollPane(VariableTable.create(list, method)));
			frame.add(new LabeledComponent("Variable Index: ", new ActionTextField(insnVar.var, s -> {
				if (Parse.isInt(s)) {
					insnVar.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TYPE_INSN:
			TypeInsnNode insnType = (TypeInsnNode) ain;
			frame.add(new LabeledComponent("Type: ", new ActionTextField(insnType.desc, s -> insnType.desc = s)));
			break;
		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode insnField = (FieldInsnNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent("Owner: ", new ActionTextField(insnField.owner, s -> insnField.owner = s)),
			new LabeledComponent("Name: ", new ActionTextField(insnField.name, s -> insnField.name = s)),
			new LabeledComponent("Descriptor: ", new ActionTextField(insnField.desc, s -> insnField.desc = s))));
			break;
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode insnMethod = (MethodInsnNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent("Owner: ", new ActionTextField(insnMethod.owner, s -> insnMethod.owner = s)),
			new LabeledComponent("Name: ", new ActionTextField(insnMethod.name, s -> insnMethod.name = s)),
			new LabeledComponent("Descriptor: ", new ActionTextField(insnMethod.desc, s -> insnMethod.desc = s)),
			new LabeledComponent("", new ActionCheckBox("<html>Owner is Interface <i>(ITF)</i></html>", insnMethod.itf,
					b -> insnMethod.itf = b))));
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			InvokeDynamicInsnNode insnIndy = (InvokeDynamicInsnNode) ain;
			if (insnIndy.bsmArgs.length > 2 && insnIndy.bsmArgs[1] instanceof Handle) {
				Handle h = (Handle) insnIndy.bsmArgs[1];
				frame.add(new LabeledComponentGroup(
				new LabeledComponent("Name: ", new ActionTextField(h.getName(), s -> Reflect.set(h, "name", s))),
				new LabeledComponent("Descriptor: ", new ActionTextField(h.getDesc(), s -> Reflect.set(h, "desc", s))),
				new LabeledComponent("Owner: ", new ActionTextField(h.getOwner(), s -> Reflect.set(h, "owner", s))),
				new LabeledComponent("IsInterface: ", new ActionTextField(h.isInterface(), s -> Reflect.setBoolean(
						insnIndy.bsm, "itf", s)))));
				frame.add(new TagTypeSwitchPanel(list, h));
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode insnJump = (JumpInsnNode) ain;
			frame.add(new LabelSwitcherPanel(list, method, insnJump.label, l -> insnJump.label = l));
			break;
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode insnLdc = (LdcInsnNode) ain;
			frame.add(new LabeledComponent("Value: ", new ActionTextField(insnLdc.cst, s -> {
				String type = insnLdc.cst.getClass().getSimpleName();
				Object cst = null;
				// Attempt to set value.
				// If fail don't worry, probably in the middle of entering their intended type.
				try {
					switch (type) {
					case "String":
						cst = s;
						break;
					case "Integer":
						cst = Integer.parseInt(s);
						break;
					case "Long":
						cst = Long.parseLong(s);
						break;
					case "Float":
						cst = Float.parseFloat(s);
						break;
					case "Double":
						cst = Double.parseDouble(s);
						break;
					case "Type":
						cst = Type.getType(s);
						break;
					}
					insnLdc.cst = cst;
				} catch (Exception e) {}
			})));
			break;
		case AbstractInsnNode.IINC_INSN:
			IincInsnNode insnIinc = (IincInsnNode) ain;
			frame.add(new JScrollPane(VariableTable.create(list, method)));
			frame.add(new LabeledComponent("Variable Index: ", new ActionTextField(insnIinc.var, s -> {
				if (Parse.isInt(s)) {
					insnIinc.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
			frame.add(new LabeledComponent("Default: ", new LabelSwitcherPanel(list, method, insnTableSwitch.dflt,
					l -> insnTableSwitch.dflt = l)));
			for (int i = 0; i < insnTableSwitch.labels.size(); i++) {
				final int fi = i;
				LabelNode label = insnTableSwitch.labels.get(i);
				int j = insnTableSwitch.min + i;
				frame.add(new LabeledComponent(j + ": ", new LabelSwitcherPanel(list, method, label, l -> insnTableSwitch.labels
						.set(fi, l))));
			}
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
			frame.add(new LabeledComponent("Default: ", new LabelSwitcherPanel(list, method, insnLookupSwitch.dflt,
					l -> insnLookupSwitch.dflt = l)));
			for (int i = 0; i < insnLookupSwitch.labels.size(); i++) {
				final int fi = i;
				LabelNode label = insnLookupSwitch.labels.get(i);
				int j = insnLookupSwitch.keys.get(i);
				frame.add(new LabeledComponent(j + ": ", new LabelSwitcherPanel(list, method, label, l -> insnLookupSwitch.labels
						.set(fi, l))));
			}
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			MultiANewArrayInsnNode insnArray = (MultiANewArrayInsnNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent("Descriptor: ", new ActionTextField(insnArray.desc, s -> insnArray.desc = s)),
			new LabeledComponent("Dimensions: ", new ActionTextField(insnArray.dims, s -> {
				if (Parse.isInt(s)) {
					insnArray.dims = Integer.parseInt(s);
				}
			}))));
			break;
		case AbstractInsnNode.FRAME:
			// TODO: Should frames even be editable? By default recaf's options
			// tell ASM to regenerate them on-export.
			//
			// FrameNode insnFrame = (FrameNode) ain;
			break;
		case AbstractInsnNode.LINE:
			LineNumberNode insnLine = (LineNumberNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent("Line: ", new ActionTextField(insnLine.line, s -> {
				if (Parse.isInt(s)) {
					insnLine.line = Integer.parseInt(s);
				}
			})),
			new LabeledComponent("Start: ", new LabelSwitcherPanel(list, method, insnLine.start,
					l -> insnLine.start = l))));
			break;
		}
		OpcodeTypeSwitchPanel opSelector = new OpcodeTypeSwitchPanel(list, ain);
		if (opSelector.getOptionCount() > 0) {
			frame.add(opSelector);
		}
		// Tell the user the empty box is intentional.
		if (!frame.hasContent()) {
			JLabel nothing = new JLabel("Nothing to edit");
			nothing.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			frame.add(nothing);
		}
		display.addWindow(frame);
		// TODO: Reliable way of positioning frame in a reasonable place (near
		// the mouse)
		// frame.setLocation(x, y);
		frame.setVisible(true);
	}
}