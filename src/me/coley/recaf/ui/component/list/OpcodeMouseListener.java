package me.coley.recaf.ui.component.list;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.asm.tracking.TInsnList;
import me.coley.recaf.event.impl.EContextMenu;
import me.coley.recaf.ui.Lang;
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
import me.coley.recaf.ui.component.panel.SearchPanel.Results;
import me.coley.recaf.ui.component.panel.SearchPanel.SearchType;
import me.coley.recaf.ui.component.table.VariableTable;
import me.coley.recaf.ui.component.tree.ASMFieldTreeNode;
import me.coley.recaf.ui.component.tree.ASMMethodTreeNode;
import me.coley.recaf.ui.component.tree.ASMTreeNode;
import me.coley.recaf.util.Misc;
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
		if (button != MouseEvent.BUTTON1) {
			AbstractInsnNode ain = (AbstractInsnNode) value;
			int x = e.getX(), y = e.getY();
			// Map of actions to menu-items.
			Map<String, ActionMenuItem> actionMap = createActionMap(ain, x, y);
			if (button == MouseEvent.BUTTON3) {
				// Custom ordered context menu
				JPopupMenu popup = new JPopupMenu();
				for (String key : recaf.configs.ui.menuOrderOpcodes) {
					ActionMenuItem item = actionMap.get(key);
					if (item != null) {
						popup.add(item);
					}
				}
				recaf.bus.post(new EContextMenu(popup, display, method, ain));
				popup.show(list, x, y);
			} else if (button == MouseEvent.BUTTON2) {
				// Custom middle-click action.
				String key = recaf.configs.ui.menuOpcodesDefaultAction;
				ActionMenuItem action = actionMap.get(key);
				if (action != null) {
					action.run();
				}
			}
		}
	}

	private Map<String, ActionMenuItem> createActionMap(AbstractInsnNode ain, int x, int y) {
		//@formatter:off
		ActionMenuItem itemEdit = new ActionMenuItem(Lang.get("window.method.opcode.edit"), 
				() -> createEdit(ain, x, y));
		ActionMenuItem itemNewBefore = new ActionMenuItem(Lang.get("window.method.opcode.new.before"), 
				() -> display.addWindow(new OpcodeCreationBox(true, list, method, ain)));
		ActionMenuItem itemNewAfter = new ActionMenuItem(Lang.get("window.method.opcode.new.after"), 
				() -> display.addWindow(new OpcodeCreationBox(false, list, method, ain)));
		ActionMenuItem itemUp = new ActionMenuItem(Lang.get("window.method.opcode.move.up"), 
				() -> {
					Asm.moveUp(list.getMethod().instructions, list.getSelectedValuesList());
					list.repopulate();
				});
		ActionMenuItem itemDown = new ActionMenuItem(Lang.get("window.method.opcode.move.down"), 
				() -> {
					Asm.moveDown(list.getMethod().instructions, list.getSelectedValuesList());
					list.repopulate();
				});
		ActionMenuItem itemSave = new ActionMenuItem(Lang.get("window.method.opcode.saveblock"), 
				() -> display.addWindow(new BlockSaveBox(list.getSelectedValuesList())));
		ActionMenuItem itemInsert = new ActionMenuItem(Lang.get("window.method.opcode.insertblock"), 
				() -> display.addWindow(new BlockInsertBox(method.instructions, list)));
		ActionMenuItem itemRemove = new ActionMenuItem(Lang.get("window.method.opcode.remove"), (new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (recaf.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, Lang.get("misc.warn.opcode"), Lang.get(
							"misc.warn.title"), JOptionPane.YES_NO_OPTION);
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
						// If the gap between current and last indices is > 1,
						// there is a gap.
						int currentIndex = ascending[i];
						if (lastIndex - currentIndex != -1) {
							// Mark end of range due to gap detection.
							// End is previous since current is the start of the
							// next range.
							ranges.add(new Range(startIndex, ascending[i - 1]));
							startIndex = currentIndex;
						}
						lastIndex = currentIndex;
					}
					// Finish last range
					ranges.add(new Range(startIndex, lastIndex));
					// Sort so ranges are iterated from last appearence to first
					// appearence.
					// Makes removal easier so accounting for offsets isn't an
					// issue.
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
					// Reset cache
					Field listStart = InsnList.class.getDeclaredField("cache");
					listStart.setAccessible(true);
					listStart.set(method.instructions, null);
					// Tracking
					if (method.instructions instanceof TInsnList) {
						((TInsnList) method.instructions).setModified();
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
		//@formatter:on
		Map<String, ActionMenuItem> actionMap = new HashMap<>();
		if (list.getSelectedIndices().length == 1) {
			actionMap.put("window.method.opcode.edit", itemEdit);
			actionMap.put("window.method.opcode.new.before", itemNewBefore);
			actionMap.put("window.method.opcode.new.after", itemNewAfter);
			actionMap.put("window.method.opcode.move.up", itemUp);
			actionMap.put("window.method.opcode.move.down", itemDown);
			actionMap.put("window.method.opcode.insertblock", itemInsert);
			actionMap.put("window.method.opcode.gotodef", createGotoDef(ain));
			actionMap.put("window.method.opcode.gotojump", createGotoJump(ain));
		} else {
			actionMap.put("window.method.opcode.move.up", itemUp);
			actionMap.put("window.method.opcode.move.down", itemDown);
			actionMap.put("window.method.opcode.saveblock", itemSave);
			actionMap.put("window.method.opcode.insertblock", itemInsert);
		}
		actionMap.put("window.method.opcode.remove", itemRemove);
		return actionMap;
	}

	private ActionMenuItem createGotoJump(AbstractInsnNode ain) {
		if (ain instanceof JumpInsnNode) {
			JumpInsnNode jin =(JumpInsnNode) ain;
			return new ActionMenuItem(Lang.get("window.method.opcode.gotojump"), () -> {
				int i = method.instructions.indexOf(jin.label);
				list.setSelectedIndex(i);
				int k = Math.min(i + 5, method.instructions.size());
				list.ensureIndexIsVisible(k);
			});
		}
		return null;
	}

	private ActionMenuItem createGotoDef(AbstractInsnNode ain) {
		boolean isMethod = true;
		String owner = null, name = null, desc = null;
		if (ain instanceof FieldInsnNode) {
			FieldInsnNode fin = (FieldInsnNode) ain;
			owner = fin.owner;
			name = fin.name;
			desc = fin.desc;
			isMethod = false;
		} else if (ain instanceof MethodInsnNode) {
			MethodInsnNode min = (MethodInsnNode) ain;
			owner = min.owner;
			name = min.name;
			desc = min.desc;
		} else if (ain instanceof InvokeDynamicInsnNode) {
			InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) ain;
			if (indy.bsmArgs.length >= 2 && indy.bsmArgs[1] instanceof Handle) {
				Handle handle = (Handle) indy.bsmArgs[1];
				owner = handle.getOwner();
				name = handle.getName();
				desc = handle.getDesc();
			}
		} else if (ain instanceof TypeInsnNode) {
			TypeInsnNode tin = (TypeInsnNode) ain;
			owner = tin.desc;
		}
		if (owner != null) {
			// I really wish lambdas didn't need values to be effectively final.
			final String fOwner = owner, fName = name, fDesc = desc;
			final boolean method = isMethod;
			final boolean classSearch = fName == null;
			return new ActionMenuItem(Lang.get("window.method.opcode.gotodef"), () -> {
				Results results = null;
				if (classSearch) {
					// type search
					results = Recaf.INSTANCE.ui.openSearch(SearchType.DECLARED_CLASS, false, new String[] { fOwner, "true" });
				} else {
					// member search
					if (method) {
						results = Recaf.INSTANCE.ui.openSearch(SearchType.DECLARED_METHOD, false, new String[] { fName, fDesc, "true" });
					} else {
						results = Recaf.INSTANCE.ui.openSearch(SearchType.DECLARED_FIELD, false, new String[] { fName, fDesc, "true" });
					}
				}
				List<ASMTreeNode> list = results.getResults();
				for (ASMTreeNode node : list)  {
					if (!classSearch && method && node instanceof ASMMethodTreeNode) {
						ASMMethodTreeNode tn = (ASMMethodTreeNode) node;
						ClassNode nn = tn.getNode();
						if (nn.name.equals(fOwner)) {
							ClassDisplayPanel display = Recaf.INSTANCE.selectClass(nn);
							display.openOpcodes(tn.getMethod());
							return;
						}
					} else if (!classSearch && !method && node instanceof ASMFieldTreeNode) {
						ASMFieldTreeNode tn = (ASMFieldTreeNode) node;
						ClassNode nn = tn.getNode();
						if (nn.name.equals(fOwner)) {
							ClassDisplayPanel display = Recaf.INSTANCE.selectClass(nn);
							display.openDefinition(tn.getField());
							return;
						}
					} else if (classSearch && node instanceof ASMTreeNode) {
						ASMTreeNode tn = (ASMTreeNode) node;
						ClassNode nn = tn.getNode();
						if (nn != null) {
							Recaf.INSTANCE.selectClass(nn);
						}
					}
				}
				String k = (classSearch ? fOwner : fName);
				Recaf.INSTANCE.ui.setTempTile(Lang.get("window.method.opcode.gotodef.fail") + " '" + k + "'", 2000);
			});
		}
		return null;
	}

	//@formatter:off
	private void createEdit(AbstractInsnNode ain, int x, int y) {
		EditBox frame = new EditBox(Lang.get("window.method.opcode") + OpcodeUtil.opcodeToName(ain.getOpcode()));
		switch (ain.getType()) {
		case AbstractInsnNode.INT_INSN:
			IntInsnNode insnInt = (IntInsnNode) ain;
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.value"), new ActionTextField(insnInt.operand, s -> {
				if (Parse.isInt(s)) {
					insnInt.operand = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.VAR_INSN:
			VarInsnNode insnVar = (VarInsnNode) ain;
			frame.add(new JScrollPane(VariableTable.create(list, method)));
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.var"), new ActionTextField(insnVar.var, s -> {
				if (Parse.isInt(s)) {
					insnVar.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TYPE_INSN:
			TypeInsnNode insnType = (TypeInsnNode) ain;
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.type"), new ActionTextField(insnType.desc, s -> insnType.desc = s)));
			break;
		case AbstractInsnNode.FIELD_INSN:
			FieldInsnNode insnField = (FieldInsnNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent(Lang.get("window.method.opcode.owner"), new ActionTextField(insnField.owner, s -> insnField.owner = s)),
			new LabeledComponent(Lang.get("window.method.opcode.name"), new ActionTextField(insnField.name, s -> insnField.name = s)),
			new LabeledComponent(Lang.get("window.method.opcode.desc"), new ActionTextField(insnField.desc, s -> insnField.desc = s))));
			break;
		case AbstractInsnNode.METHOD_INSN:
			MethodInsnNode insnMethod = (MethodInsnNode) ain;
			frame.add(new LabeledComponentGroup(
			new LabeledComponent(Lang.get("window.method.opcode.owner"), new ActionTextField(insnMethod.owner, s -> insnMethod.owner = s)),
			new LabeledComponent(Lang.get("window.method.opcode.name"), new ActionTextField(insnMethod.name, s -> insnMethod.name = s)),
			new LabeledComponent(Lang.get("window.method.opcode.desc"), new ActionTextField(insnMethod.desc, s -> insnMethod.desc = s)),
			new LabeledComponent("", new ActionCheckBox(Lang.get("window.method.opcode.itf"), insnMethod.itf,
					b -> insnMethod.itf = b))));
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			InvokeDynamicInsnNode insnIndy = (InvokeDynamicInsnNode) ain;
			JPanel wrap = new JPanel();
			Dimension dim = new Dimension(500, 9999);
			frame.setMaximumSize(dim);
			// Indy values
			wrap.setMaximumSize(dim);
			wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
			wrap.setBorder(BorderFactory.createTitledBorder("InvokeDynamic"));
			wrap.add(new LabeledComponentGroup(
				new LabeledComponent(Lang.get("window.method.opcode.name"), new ActionTextField(insnIndy.name, s -> insnIndy.name = s)),
				new LabeledComponent(Lang.get("window.method.opcode.desc"), new ActionTextField(insnIndy.desc, s -> insnIndy.desc = s))
			));
			frame.add(wrap);
			// BSM callsite
			wrap = new JPanel();
			wrap.setMaximumSize(dim);
			wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
			wrap.setBorder(BorderFactory.createTitledBorder("Callsite"));
			addHandle(wrap, insnIndy.bsm);
			frame.add(wrap);
			// BSM args
			if (insnIndy.bsmArgs.length >= 3 && insnIndy.bsmArgs[1] instanceof Handle) {
				for (int i = 0; i < insnIndy.bsmArgs.length; i++) {
					final int j = i;
					Object arg = insnIndy.bsmArgs[i];
					wrap = new JPanel();
					wrap.setMaximumSize(dim);
					wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
					wrap.setBorder(BorderFactory.createTitledBorder("Bootstrap arg[" + i + "]: " + arg.getClass().getSimpleName()));
					if (arg instanceof Type) {
						Type t = (Type) arg;
						wrap.add( new ActionTextField(t.getDescriptor(), s -> {
							Type newVal = Misc.parseType(s);
							if (newVal != null) {
								insnIndy.bsmArgs[j] = newVal;
							}
						}));
					} else if (arg instanceof Handle) {
						Handle h = (Handle) insnIndy.bsmArgs[j];
						addHandle(wrap, h);
					} else {
						throw new RuntimeException("Unknown BSM-Arg type: " + arg.getClass());
					}
					frame.add(wrap);
				}
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			JumpInsnNode insnJump = (JumpInsnNode) ain;
			frame.add(new LabelSwitcherPanel(list, method, insnJump.label, l -> insnJump.label = l));
			break;
		case AbstractInsnNode.LDC_INSN:
			LdcInsnNode insnLdc = (LdcInsnNode) ain;
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.value"), new ActionTextField(insnLdc.cst, s -> {
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
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.var"), new ActionTextField(insnIinc.var, s -> {
				if (Parse.isInt(s)) {
					insnIinc.var = Integer.parseInt(s);
				}
			})));
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.default"), new LabelSwitcherPanel(list, method, insnTableSwitch.dflt,
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
			frame.add(new LabeledComponent(Lang.get("window.method.opcode.default"), new LabelSwitcherPanel(list, method, insnLookupSwitch.dflt,
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
			new LabeledComponent(Lang.get("window.method.opcode.desc"), new ActionTextField(insnArray.desc, s -> insnArray.desc = s)),
			new LabeledComponent(Lang.get("window.method.opcode.dims"), new ActionTextField(insnArray.dims, s -> {
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
			new LabeledComponent(Lang.get("window.method.opcode.line"), new ActionTextField(insnLine.line, s -> {
				if (Parse.isInt(s)) {
					insnLine.line = Integer.parseInt(s);
				}
			})),
			new LabeledComponent(Lang.get("window.method.opcode.start"), new LabelSwitcherPanel(list, method, insnLine.start,
					l -> insnLine.start = l))));
			break;
		}
		OpcodeTypeSwitchPanel opSelector = new OpcodeTypeSwitchPanel(list, ain);
		if (opSelector.getOptionCount() > 0) {
			frame.add(opSelector);
		}
		// Tell the user the empty box is intentional.
		if (!frame.hasContent()) {
			JLabel nothing = new JLabel(Lang.get("window.method.opcode.empty"));
			nothing.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			frame.add(nothing);
		}
		display.addWindow(frame);
		// TODO: Reliable way of positioning frame in a reasonable place (near
		// the mouse)
		// frame.setLocation(x, y);
		frame.setVisible(true);
	}

	private void addHandle(JPanel wrap, Handle h) {
		wrap.add(new LabeledComponentGroup(
			new LabeledComponent(Lang.get("window.method.opcode.owner"), new ActionTextField(h.getOwner(), s -> Reflect.set(h, "owner", s))),
			new LabeledComponent(Lang.get("window.method.opcode.name"), new ActionTextField(h.getName(), s -> Reflect.set(h, "name", s))),
			new LabeledComponent(Lang.get("window.method.opcode.desc"), new ActionTextField(h.getDesc(), s -> Reflect.set(h, "desc", s))),
			new LabeledComponent("", new ActionCheckBox(Lang.get("window.method.opcode.itf"), h.isInterface(), b -> Reflect.set(h, "itf", b)))));
		wrap.add(new TagTypeSwitchPanel(list, h));
	}
}