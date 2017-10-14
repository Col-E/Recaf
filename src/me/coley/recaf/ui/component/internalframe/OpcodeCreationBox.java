package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.list.OpcodeList;
import me.coley.recaf.ui.component.table.VariableTable;

@SuppressWarnings("serial")
public class OpcodeCreationBox extends BasicFrame {
	private static final Map<String, Integer> nameToType = new HashMap<>();
	private final Map<String, JComboBox<String>> typeToOpcodeSelector = new HashMap<>();
	private final Map<String, Map<String, JTextField>> typeToMapOfThings = new HashMap<>();
	private String currentType;

	public OpcodeCreationBox(boolean insertBefore, OpcodeList list, MethodNode method, AbstractInsnNode target) {
		super("Create Opcode");
		setLayout(new BorderLayout());
		// Setting up center of the border panel to be a inner card-layout
		// panel.
		JPanel content = new JPanel();
		JComboBox<String> comboCard = new JComboBox<>(from(nameToType.keySet()));
		comboCard.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				String item = (String) evt.getItem();
				currentType = item;
				CardLayout cl = (CardLayout) (content.getLayout());
				cl.show(content, item);
			}
		});
		content.setLayout(new CardLayout());
		// Creating cards per opcode-type.
		for (String key : nameToType.keySet()) {
			Map<String, JTextField> map = new HashMap<>();
			typeToMapOfThings.put(key, map);
			JPanel card = new JPanel();
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			int type = nameToType.get(key);
			content.add(card, key);
			// Adding content to cards
			String[] codes = OpcodeUtil.typeToCodes(type);
			JComboBox<String> comboCodes = new JComboBox<>(codes);
			if (codes.length > 1) {
				card.add(new LabeledComponent("Opcode: ", comboCodes));
			}
			typeToOpcodeSelector.put(key, comboCodes);
			// Type specific content
			switch (type) {
			case AbstractInsnNode.LDC_INSN:
			case AbstractInsnNode.INT_INSN: {
				JTextField text = new JTextField();
				card.add(new LabeledComponent("Value: ", text));
				map.put("value", text);
				break;
			}
			case AbstractInsnNode.IINC_INSN: {
				card.add(new JScrollPane(VariableTable.create(list, method)));
				JTextField var = new JTextField();
				JTextField text = new JTextField();
				card.add(new LabeledComponent("Variable: ", var));
				card.add(new LabeledComponent("Increment: ", text));
				map.put("var", var);
				map.put("inc", text);
				break;
			}
			case AbstractInsnNode.VAR_INSN: {
				card.add(new JScrollPane(VariableTable.create(list, method)));
				JTextField var = new JTextField();
				card.add(new LabeledComponent("Variable: ", var));
				map.put("var", var);
				break;
			}
			case AbstractInsnNode.TYPE_INSN: {
				JTextField text = new JTextField();
				card.add(new LabeledComponent("Type: ", text));
				map.put("value", text);
				break;
			}
			case AbstractInsnNode.FIELD_INSN:
			case AbstractInsnNode.METHOD_INSN: {
				JTextField owner = new JTextField();
				JTextField name = new JTextField();
				JTextField desc = new JTextField();
				card.add(new LabeledComponent("Owner: ", owner));
				card.add(new LabeledComponent("Name: ", name));
				card.add(new LabeledComponent("Desc: ", desc));
				map.put("owner", owner);
				map.put("name", name);
				map.put("desc", desc);
				break;
			}
			case AbstractInsnNode.MULTIANEWARRAY_INSN: {
				JTextField desc = new JTextField();
				JTextField dim = new JTextField();
				card.add(new LabeledComponent("Desc: ", desc));
				card.add(new LabeledComponent("Dimensions: ", dim));
				map.put("desc", desc);
				map.put("dims", dim);
				break;
			}
			case AbstractInsnNode.LINE: {
				JTextField text = new JTextField();
				card.add(new LabeledComponent("Line: ", text));
				map.put("value", text);
				break;
			}
			// TODO: The rest of these
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				break;
			case AbstractInsnNode.JUMP_INSN:
				break;
			case AbstractInsnNode.LABEL:
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				break;
			case AbstractInsnNode.FRAME:
				break;
			}
		}
		// Action for adding opcode.
		ActionButton btn = new ActionButton("Add Opcode", () -> {
			AbstractInsnNode ain = create();
			if (ain != null) {
				// Insert
				if (insertBefore) {
					method.instructions.insertBefore(target, ain);
				} else {
					method.instructions.insert(target, ain);
				}
				list.repopulate();
				// Close window
				try {
					setClosed(true);
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				}
			} else {
				// Couldn't make insn, show error.
				setTitle("Error: Check inputs!");
			}
		});
		add(content, BorderLayout.CENTER);
		add(btn, BorderLayout.SOUTH);
		add(comboCard, BorderLayout.NORTH);
		setVisible(true);
		pack();
	}

	/**
	 * Create an opcode from the current open card. If there's an input
	 * error no node is returned.
	 *
	 * @return The opcode created.
	 */
	public AbstractInsnNode create() {
		try {
			switch (nameToType.get(currentType)) {
			case AbstractInsnNode.INSN:
				return new InsnNode(getOpcode());
			case AbstractInsnNode.INT_INSN:
				return new IntInsnNode(getOpcode(), getInt("value"));
			case AbstractInsnNode.VAR_INSN:
				return new VarInsnNode(getOpcode(), getInt("var"));
			case AbstractInsnNode.TYPE_INSN:
				return new TypeInsnNode(getOpcode(), getString("value"));
			case AbstractInsnNode.FIELD_INSN:
				return new FieldInsnNode(getOpcode(), getString("owner"), getString("name"), getString("desc"));
			case AbstractInsnNode.METHOD_INSN:
				return new MethodInsnNode(getOpcode(), getString("owner"), getString("name"), getString("desc"),
										  getOpcode() == Opcodes.INVOKEINTERFACE);
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				break;
			case AbstractInsnNode.LABEL:
				return new LabelNode();
			case AbstractInsnNode.LDC_INSN:
				return new LdcInsnNode(get("value"));
			case AbstractInsnNode.IINC_INSN:
				return new IincInsnNode(getInt("var"), getInt("inc"));
			case AbstractInsnNode.LINE:
				return new LineNumberNode(getInt("value"), new LabelNode());
			case AbstractInsnNode.MULTIANEWARRAY_INSN:
				return new MultiANewArrayInsnNode(getString("desc"), getInt("dims"));
			case AbstractInsnNode.JUMP_INSN:
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				break;
			case AbstractInsnNode.FRAME:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get selected opcode.
	 *
	 * @return
	 */
	private int getOpcode() {
		return OpcodeUtil.nameToOpcode(typeToOpcodeSelector.get(currentType).getSelectedItem().toString());
	}

	/**
	 * Get value for the current card pertaining to the given key as a String.
	 *
	 * @param key
	 *            Label associated with an input.
	 * @return
	 */
	private String getString(String key) {
		return get(key).toString();
	}

	/**
	 * Get value for the current card pertaining to the given key as an integer.
	 *
	 * @param key
	 *            Label associated with an input.
	 * @return
	 */
	private int getInt(String key) {
		return Integer.parseInt(get(key).toString());
	}

	/**
	 * Get value for the current card pertaining to the given key.
	 *
	 * @param key
	 *            Label associated with an input.
	 * @return
	 */
	private Object get(String key) {
		JTextField field = typeToMapOfThings.get(currentType).get(key);
		if (field != null) {
			return field.getText();
		}
		return null;
	}

	/**
	 * Set to array.
	 *
	 * @param set
	 * @return
	 */
	private static String[] from(Set<String> set) {
		String[] s = new String[set.size()];
		int i = 0;
		for (String ss : set) {
			s[i++] = ss;
		}
		return s;
	}

	static {
		// Commenting out the lines that would add cards for unsupported insn
		// types. When they're supported they'll be uncommented.
		nameToType.put("Insn", AbstractInsnNode.INSN);
		nameToType.put("Field", AbstractInsnNode.FIELD_INSN);
		// nameToType.put("Frame", AbstractInsnNode.FRAME);
		nameToType.put("Increment", AbstractInsnNode.IINC_INSN);
		nameToType.put("Integer", AbstractInsnNode.INT_INSN);
		// nameToType.put("Jump", AbstractInsnNode.JUMP_INSN);
		nameToType.put("Ldc", AbstractInsnNode.LDC_INSN);
		// nameToType.put("Line", AbstractInsnNode.LINE);
		// nameToType.put("LookupSwitch", AbstractInsnNode.LOOKUPSWITCH_INSN);
		nameToType.put("Method", AbstractInsnNode.METHOD_INSN);
		// nameToType.put("MethodIndy", AbstractInsnNode.INVOKE_DYNAMIC_INSN);
		nameToType.put("MultiANewArray", AbstractInsnNode.MULTIANEWARRAY_INSN);
		// nameToType.put("TableSwitch", AbstractInsnNode.TABLESWITCH_INSN);
		nameToType.put("Type", AbstractInsnNode.TYPE_INSN);
		nameToType.put("Variable", AbstractInsnNode.VAR_INSN);
	}
}
