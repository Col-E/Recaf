package me.coley.recaf.config.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.asm.opcode.LabeledJumpInsnNode;
import me.coley.recaf.asm.opcode.LabeledLookupSwitchInsnNode;
import me.coley.recaf.asm.opcode.LabeledTableSwitchInsnNode;
import me.coley.recaf.asm.opcode.LineNumberNodeExt;
import me.coley.recaf.asm.opcode.NamedLabelNode;

import me.coley.recaf.config.Config;
import me.coley.recaf.util.Misc;

public class ConfBlocks extends Config {
	private final static String ERR = "Could not parse saved block content.";

	public Map<String, List<AbstractInsnNode>> blocks = new HashMap<>();

	public ConfBlocks() {
		super("rcblocks");
	}

	/**
	 * Add a block of opcodes by the given key to the config.
	 * 
	 * @param key
	 *            Identifier for the opcodes.
	 * @param list
	 *            List of opcodes.
	 */
	public void add(String key, List<AbstractInsnNode> list) {
		// TODO: Support of classes shown in the switch below,
		// For now, flat-out reject saving of anything containing them.

		// Create map of new labels
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		int label = 0;
		for (int i = 0; i < list.size(); i++) {
			AbstractInsnNode ain = list.get(i);
			if (ain.getType() == AbstractInsnNode.LABEL) {
				labels.put((LabelNode) ain, new NamedLabelNode(Misc.generateName(label++)));
			}
		}
		// Clone to prevent synchronization issues
		// This also updates labels with the NamedLabelNode clones.
		List<AbstractInsnNode> clone = new ArrayList<>();
		for (AbstractInsnNode ain : list) {
			clone.add(ain.clone(labels));
		}
		// Replace with specified opcode types.
		for (int i = 0; i < clone.size(); i++) {
			AbstractInsnNode ain = clone.get(i);
			switch (ain.getType()) {
			case AbstractInsnNode.LINE:
				LineNumberNode line = (LineNumberNode) ain;
				clone.set(i, new LineNumberNodeExt(line));
				break;
			case AbstractInsnNode.FRAME:
				// I am way too lazy to handle serialization of frames.
				// ASM can just figure that nonsense out on the fly anyways, so
				// its not really a problem.
				clone.set(i, new InsnNode(Opcodes.NOP));
				break;
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				// Skip saving anything containing this type.
				Recaf.INSTANCE.logging.error(new UnsupportedOperationException("Unsupported opcode: " + ain.getOpcode() + ":"
						+ OpcodeUtil.opcodeToName(ain.getOpcode())));
				return;
			default:
				break;
			}
		}
		blocks.put(key, clone);
		// Save changes
		save();
	}

	@Override
	public void load() {
		super.load();
		// Update labels of jumps not filled in from JSON parsing.
		for (String block : blocks.keySet()) {
			updateJumps(blocks.get(block));
		}
	}

	/**
	 * Create a clones list of the intended block.
	 * 
	 * @param blockKey
	 *            Block to get clone of.
	 * @return Clone of block by it's key.
	 */
	public List<AbstractInsnNode> getClone(String blockKey) {
		List<AbstractInsnNode> orig = blocks.get(blockKey);
		List<AbstractInsnNode> clone = new ArrayList<>();
		// Create map of label's clones
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode ain : orig) {
			if (ain instanceof NamedLabelNode) {
				NamedLabelNode nln = (NamedLabelNode) ain;
				labels.put(nln, new NamedLabelNode(nln.name));
			}
		}
		// Clone each opcode with the clone map
		for (AbstractInsnNode ain : orig) {
			clone.add(ain.clone(labels));
		}
		// Patch jumps
		updateJumps(clone);
		return clone;
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		JsonObject v = new JsonObject();
		if (value.equals(blocks)) {
			// Save map
			@SuppressWarnings("unchecked")
			Map<String, List<AbstractInsnNode>> map = (Map<String, List<AbstractInsnNode>>) value;
			for (Entry<String, List<AbstractInsnNode>> entry : map.entrySet()) {
				JsonArray array = new JsonArray();
				for (AbstractInsnNode ain : entry.getValue()) {
					array.add(convert(ain.getClass(), ain));
				}
				v.add(entry.getKey(), array);
			}
		} else if (value instanceof AbstractInsnNode) {
			// Save each opcode
			AbstractInsnNode ain = (AbstractInsnNode) value;
			v.add("opcode", OpcodeUtil.opcodeToName(ain.getOpcode()));
			switch (ain.getType()) {
			case AbstractInsnNode.INSN:
				break;
			case AbstractInsnNode.FIELD_INSN: {
				FieldInsnNode fin = (FieldInsnNode) ain;
				v.add("owner", fin.owner);
				v.add("name", fin.name);
				v.add("desc", fin.desc);
				break;
			}
			case AbstractInsnNode.METHOD_INSN: {
				MethodInsnNode min = (MethodInsnNode) ain;
				v.add("owner", min.owner);
				v.add("name", min.name);
				v.add("desc", min.desc);
				v.add("itf", min.itf);
				break;
			}
			case AbstractInsnNode.IINC_INSN: {
				IincInsnNode iinc = (IincInsnNode) ain;
				v.add("var", iinc.var);
				v.add("incr", iinc.incr);
				break;
			}
			case AbstractInsnNode.VAR_INSN: {
				VarInsnNode vin = (VarInsnNode) ain;
				v.add("var", vin.var);
				break;
			}
			case AbstractInsnNode.MULTIANEWARRAY_INSN: {
				MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) ain;
				v.add("desc", manain.desc);
				v.add("dims", manain.dims);
				break;
			}
			case AbstractInsnNode.INT_INSN: {
				IntInsnNode iin = (IntInsnNode) ain;
				v.add("value", iin.operand);
				break;
			}
			case AbstractInsnNode.LDC_INSN: {
				LdcInsnNode ldc = (LdcInsnNode) ain;
				v.add("value", ldc.cst.toString());
				v.add("type-readonly", ldc.cst.getClass().getSimpleName());
				break;
			}
			case AbstractInsnNode.TYPE_INSN: {
				TypeInsnNode tin = (TypeInsnNode) ain;
				v.add("desc", tin.desc);
				break;
			}
			case AbstractInsnNode.LINE: {
				LineNumberNode lnn = (LineNumberNode) ain;
				v.add("line", lnn.line);
				break;
			}
			case AbstractInsnNode.LABEL: {
				NamedLabelNode lnn = (NamedLabelNode) ain;
				v.add("id", lnn.name);
				break;
			}
			case AbstractInsnNode.JUMP_INSN: {
				JumpInsnNode jin = (JumpInsnNode) ain;
				NamedLabelNode lnn = (NamedLabelNode) jin.label;
				v.add("dest", lnn.name);
				break;
			}
			case AbstractInsnNode.TABLESWITCH_INSN: {
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
				v.add("min", tsin.min);
				v.add("max", tsin.max);
				NamedLabelNode lnn = (NamedLabelNode) tsin.dflt;
				JsonArray lbls = new JsonArray();
				for (int j = 0; j < tsin.labels.size(); j++) {
					lnn = (NamedLabelNode) tsin.labels.get(j);
					lbls.add(lnn.name);
				}
				v.add("labels", lbls);
				v.add("default", lnn.name);
				break;
			}
			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
				NamedLabelNode lnn = (NamedLabelNode) lsin.dflt;
				JsonArray keys = new JsonArray();
				for (int j = 0; j < lsin.labels.size(); j++) {
					int i = lsin.keys.get(j);
					keys.add(i);
				}
				v.add("keys", keys);
				JsonArray lbls = new JsonArray();
				for (int j = 0; j < lsin.labels.size(); j++) {
					lnn = (NamedLabelNode) lsin.labels.get(j);
					lbls.add(lnn.name);
				}
				v.add("labels", lbls);
				v.add("default", lnn.name);
				break;
			}
			case AbstractInsnNode.FRAME:
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				throw new UnsupportedOperationException("Unsupported opcode <save> : " + OpcodeUtil.opcodeToName(ain
						.getOpcode()));
			}
		}
		return v;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (type.equals(Map.class)) {
			// Construct new map
			Map<String, List<AbstractInsnNode>> temp = new HashMap<>();
			// For each entry stored in the passes value
			value.asObject().forEach(m -> {
				// Extract block name and opcode list.
				// Put results into map.
				String name = m.getName();
				JsonArray opcodes = m.getValue().asArray();
				List<AbstractInsnNode> opcodeList = new ArrayList<>();
				for (JsonValue entry : opcodes.values()) {
					opcodeList.add(parse(entry.asObject()));
				}
				temp.put(name, opcodeList);
			});
			return temp;
		}
		return super.parse(type, value);
	}

	/**
	 * Reads an AbstractInsnNode from the given JsonObject
	 * 
	 * @param o
	 *            Json object.
	 * @return AbstractInsnNode instance.
	 */
	private static AbstractInsnNode parse(JsonObject o) {
		String opcodeName = get(o, "opcode");
		int opcode = OpcodeUtil.nameToOpcode(opcodeName);
		int type = OpcodeUtil.opcodeToType(opcode);
		switch (type) {
		case AbstractInsnNode.INSN:
			return new InsnNode(opcode);
		case AbstractInsnNode.FIELD_INSN: {
			return new FieldInsnNode(opcode, get(o, "owner"), get(o, "name"), get(o, "desc"));
		}
		case AbstractInsnNode.METHOD_INSN: {
			return new MethodInsnNode(opcode, get(o, "owner"), get(o, "name"), get(o, "desc"), getB(o, "itf"));
		}
		case AbstractInsnNode.IINC_INSN: {
			return new IincInsnNode(getI(o, "var"), getI(o, "incr"));
		}
		case AbstractInsnNode.VAR_INSN: {
			return new VarInsnNode(opcode, getI(o, "var"));
		}
		case AbstractInsnNode.MULTIANEWARRAY_INSN: {
			return new MultiANewArrayInsnNode(get(o, "desc"), getI(o, "dims"));
		}
		case AbstractInsnNode.INT_INSN: {
			return new IntInsnNode(opcode, getI(o, "value"));
		}
		case AbstractInsnNode.LDC_INSN: {
			String typeHelper = get(o, "type-readonly");
			String value = get(o, "value");
			Object obj = null;
			switch (typeHelper) {
			case "Integer":
				obj = Integer.parseInt(value);
				break;
			case "Float":
				obj = Float.parseFloat(value);
				break;
			case "Long":
				obj = Long.parseLong(value);
				break;
			case "Double":
				obj = Double.parseDouble(value);
				break;
			case "String":
				obj = value;
				break;
			case "Type":
				obj = Type.getType(value);
				break;
			}
			return new LdcInsnNode(obj);
		}
		case AbstractInsnNode.TYPE_INSN: {
			return new TypeInsnNode(opcode, get(o, "desc"));
		}
		case AbstractInsnNode.LINE: {
			return new LineNumberNodeExt(getI(o, "line"), new LabelNode());
		}
		case AbstractInsnNode.LABEL: {
			return new NamedLabelNode(get(o, "id"));
		}
		case AbstractInsnNode.JUMP_INSN: {
			return new LabeledJumpInsnNode(opcode, get(o, "dest"));
		}
		case AbstractInsnNode.TABLESWITCH_INSN: {
			return new LabeledTableSwitchInsnNode(getI(o, "min"), getI(o, "max"), get(o, "default"), getSA(o, "labels"));
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN: {
			return new LabeledLookupSwitchInsnNode(get(o, "default"), getSA(o, "labels"), getIA(o, "keys"));
		}
		}
		throw new UnsupportedOperationException("Unsupported opcode <load> : " + opcodeName + " : " + opcode + "(type:" + type
				+ ")");
	}

	/**
	 * Read string from given json object.
	 * 
	 * @param object
	 *            Object to read from.
	 * @param key
	 *            Key to read.
	 * @return Value in object of key.
	 */
	private static String get(JsonObject object, String key) {
		return object.getString(key, ERR);
	}

	/**
	 * Read string array from given json object.
	 * 
	 * @param object
	 *            Object to read from.
	 * @param key
	 *            Key to read.
	 * @return Value in object of key.
	 */
	private static String[] getSA(JsonObject object, String key) {
		JsonArray arr = object.get(key).asArray();
		int len = arr.size();
		String[] sa = new String[len];
		for (int i = 0; i < len; i++) {
			sa[i] = arr.get(i).asString();
		}
		return sa;
	}

	/**
	 * Read boolean from given json object.
	 * 
	 * @param object
	 *            Object to read from.
	 * @param key
	 *            Key to read.
	 * @return Value in object of key.
	 */
	private static boolean getB(JsonObject object, String key) {
		return object.getBoolean(key, false);
	}

	/**
	 * Read integer from given json object.
	 * 
	 * @param object
	 *            Object to read from.
	 * @param key
	 *            Key to read.
	 * @return Value in object of key.
	 */
	private static int getI(JsonObject object, String key) {
		return object.getInt(key, -1);
	}

	/**
	 * Read integer array from given json object.
	 * 
	 * @param object
	 *            Object to read from.
	 * @param key
	 *            Key to read.
	 * @return Value in object of key.
	 */
	private static int[] getIA(JsonObject object, String key) {
		JsonArray arr = object.get(key).asArray();
		int len = arr.size();
		int[] ia = new int[len];
		for (int i = 0; i < len; i++) {
			ia[i] = arr.get(i).asInt();
		}
		return ia;
	}

	/**
	 * Patch opcodes with custom labels in the given block. This is necessary
	 * since the complete map of LabelNode's is not known until all the opcodes
	 * are parsed.
	 * 
	 * @param block
	 *            Block to patch.
	 */
	private static void updateJumps(List<AbstractInsnNode> block) {
		// Create map of label names to label instances
		Map<String, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode ain : block) {
			if (ain instanceof NamedLabelNode) {
				NamedLabelNode nln = (NamedLabelNode) ain;
				labels.put(nln.name, nln);
			}
		}
		// Fill in labels of labeled jump's that do not have their label's set.
		for (AbstractInsnNode ain : block) {
			if (ain instanceof LabeledJumpInsnNode) {
				LabeledJumpInsnNode njin = (LabeledJumpInsnNode) ain;
				njin.setupLabel(labels);
			} else if (ain instanceof LabeledTableSwitchInsnNode) {
				LabeledTableSwitchInsnNode ltsin = (LabeledTableSwitchInsnNode) ain;
				ltsin.setupLabels(labels);
			} else if (ain instanceof LabeledLookupSwitchInsnNode) {
				LabeledLookupSwitchInsnNode llsin = (LabeledLookupSwitchInsnNode) ain;
				llsin.setupLabels(labels);
			}
		}
	}

}