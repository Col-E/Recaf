package me.coley.recaf.config.impl;

import com.eclipsesource.json.*;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.bytecode.insn.*;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Saved opcode blocks.
 *
 * @author Matt
 */
public class ConfBlocks extends Config {
	@Conf(category = "block", key = "map")
	public Map<String, List<AbstractInsnNode>> blocks = new HashMap<>();

	public ConfBlocks() {
		super("rc_blocks");
		load();
		for (List<AbstractInsnNode> block : blocks.values()) {
			updateJumps(block);
		}
	}

	/**
	 * @return Map of opcode blocks.
	 */
	public Map<String, List<AbstractInsnNode>> getMaps() {
		return blocks;
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
		List<AbstractInsnNode> clone = cloneAsSerializable(list);
		blocks.put(key, clone);
		// Save changes
		save();
	}

	/**
	 * Create a clones list of the intended block.
	 *
	 * @param blockKey
	 *            Block to get clone of.
	 * @return Clone of block by it's key. {@code null} if no block by the name
	 *         exists.
	 */
	public Block getClone(String blockKey) {
		List<AbstractInsnNode> orig = blocks.get(blockKey);
		if (orig == null) {
			// no block by name, return nothing.
			return null;
		}
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
		return new Block(blockKey, clone);
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		JsonObject v = new JsonObject();
		if (type.equals(Map.class)) {
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
				NamedLabelNode lbl = (NamedLabelNode) lnn.start;
				v.add("line", lnn.line);
				v.add("start", lbl.name);
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
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) ain;
				v.add("name", indy.name);
				v.add("desc", indy.desc);
				JsonObject handle = new JsonObject();
				handle.add("owner", indy.bsm.getOwner());
				handle.add("name", indy.bsm.getName());
				handle.add("desc", indy.bsm.getDesc());
				handle.add("tag", indy.bsm.getTag());
				handle.add("itf", indy.bsm.isInterface());
				v.add("handle", handle);
				JsonArray args = new JsonArray();
				for (Object obj : indy.bsmArgs) {
					JsonObject arg = new JsonObject();
					arg.add("type-readonly", obj.getClass().getSimpleName());
					if (obj instanceof Type) {
						arg.add("value", obj.toString());
					} else if (obj instanceof Handle) {
						Handle argHandle = (Handle) obj;
						arg.add("owner", argHandle.getOwner());
						arg.add("name", argHandle.getName());
						arg.add("desc", argHandle.getDesc());
						arg.add("tag", argHandle.getTag());
						arg.add("itf", argHandle.isInterface());
					} else {
						// numbers and strings
						arg.add("value", obj.toString());
					}
					args.add(arg);
				}
				v.add("args", args);
				break;
			case AbstractInsnNode.FRAME:
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
	 * @param list
	 * 		Original opcode collection.
	 *
	 * @return clone of the list, because duplicate instances of instances
	 * shared across multiple spaces is not proper usage of ASM.
	 */
	public static List<AbstractInsnNode> cloneAsSerializable(List<AbstractInsnNode> list) {
		// Create map of new labels
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		int label = 0;
		for(AbstractInsnNode ain : list) {
			if(ain.getType() == AbstractInsnNode.LABEL) {
				LabelNode lbl = (LabelNode) ain;
				labels.put(lbl, new NamedLabelNode(NamedLabelNode.generateName(label++)));
			}
		}
		// Clone the opcodes to new instances with the given label mappings.
		List<AbstractInsnNode> clone = new ArrayList<>();
		for(AbstractInsnNode ain : list) {
			clone.add(ain.clone(labels));
		}
		// Replace with specified opcode types for serialization purposes.
		for(int i = 0; i < clone.size(); i++) {
			AbstractInsnNode ain = clone.get(i);
			switch(ain.getType()) {
				case AbstractInsnNode.LINE:
					// replaced because opcode conflicts with -1
					LineNumberNode line = (LineNumberNode) ain;
					clone.set(i, new NamedLineNumberNode(line));
					break;
				case AbstractInsnNode.FRAME:
					// I am way too lazy to handle serialization of frames.
					// Recaf recalculates frames by default anyways so, whatever.
					clone.set(i, new InsnNode(Opcodes.NOP));
					break;
				default:
					break;
			}
		}
		return clone;
	}

	/**
	 * @param insns
	 * 		The full instruction list of the method.
	 * @param list
	 * 		Original opcode collection.
	 *
	 * @return clone of the list, because duplicate instances of instances
	 * shared across multiple spaces is not proper usage of ASM.
	 */
	public static List<AbstractInsnNode> clone(InsnList insns, List<AbstractInsnNode> list) {
		// Create map of new labels
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode ain : insns.toArray()) {
			if (ain.getType() == AbstractInsnNode.LABEL) {
				LabelNode lbl = (LabelNode) ain;
				// If the to-clone list contains the label, we need to make a unique copy of it.
				// Otherwise, external references don't need to be updated.
				if (list.contains(lbl)) {
					labels.put(lbl, new LabelNode());
				} else {
					labels.put(lbl, lbl);
				}
			}
		}
		// Clone the opcodes to new instances with the given label mappings.
		List<AbstractInsnNode> clone = new ArrayList<>();
		for (AbstractInsnNode ain : list) {
			clone.add(ain.clone(labels));
		}
		return clone;
	}

	/**
	 * Reads an AbstractInsnNode from the given JsonObject
	 *
	 * @param o
	 * 		Json object.
	 *
	 * @return AbstractInsnNode instance.
	 */
	private static AbstractInsnNode parse(JsonObject o) {
		String opcodeName = get(o, "opcode");
		int opcode = OpcodeUtil.nameToOpcode(opcodeName);
		int type = OpcodeUtil.opcodeToType(opcode);
		switch (type) {
		case AbstractInsnNode.INSN:
			return new InsnNode(opcode);
		case AbstractInsnNode.FIELD_INSN:
			return new FieldInsnNode(opcode, get(o, "owner"), get(o, "name"), get(o, "desc"));
		case AbstractInsnNode.METHOD_INSN:
			return new MethodInsnNode(opcode, get(o, "owner"), get(o, "name"), get(o, "desc"), getBool(o, "itf"));
		case AbstractInsnNode.IINC_INSN:
			return new IincInsnNode(getInt(o, "var"), getInt(o, "incr"));
		case AbstractInsnNode.VAR_INSN:
			return new VarInsnNode(opcode, getInt(o, "var"));
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			return new MultiANewArrayInsnNode(get(o, "desc"), getInt(o, "dims"));
		case AbstractInsnNode.INT_INSN:
			return new IntInsnNode(opcode, getInt(o, "value"));
		case AbstractInsnNode.LDC_INSN: {
			String typeHelper = get(o, "type-readonly");
			Object obj = getObject(typeHelper, o);
			return new LdcInsnNode(obj);
		}
		case AbstractInsnNode.TYPE_INSN:
			return new TypeInsnNode(opcode, get(o, "desc"));
		case AbstractInsnNode.LINE:
			return new NamedLineNumberNode(getInt(o, "line"), null, get(o, "start"));
		case AbstractInsnNode.LABEL:
			return new NamedLabelNode(get(o, "id"));
		case AbstractInsnNode.JUMP_INSN:
			return new NamedJumpInsnNode(opcode, get(o, "dest"));
		case AbstractInsnNode.TABLESWITCH_INSN:
			return new NamedTableSwitchInsnNode(getInt(o, "min"), getInt(o, "max"), get(o, "default"), getStringArray(o,
					"labels"));
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			return new NamedLookupSwitchInsnNode(get(o, "default"), getStringArray(o, "labels"), getIntArray(o, "keys"));
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			JsonObject oHandle = o.get("handle").asObject();
			Handle handle = (Handle) getObject("Handle", oHandle);
			JsonArray oArr = o.get("args").asArray();
			Object[] args = new Object[oArr.size()];
			for (int i = 0; i < args.length; i++) {
				JsonObject oArg = oArr.get(i).asObject();
				args[i] = getObject(get(oArg, "type-readonly"), oArg);
			}
			return new InvokeDynamicInsnNode(get(o, "name"), get(o, "desc"), handle, args);
		}
		throw new UnsupportedOperationException("Unsupported opcode <load> : " + opcodeName + " : " + opcode + "(type:" + type
				+ ")");
	}

	private static Object getObject(String typeHelper, JsonObject o) {
		Object obj = null;
		switch (typeHelper) {
		case "Integer":
			obj = Integer.parseInt(get(o, "value"));
			break;
		case "Long":
			obj = Long.parseLong(get(o, "value"));
			break;
		case "Float":
			obj = Float.parseFloat(get(o, "value"));
			break;
		case "Double":
			obj = Double.parseDouble(get(o, "value"));
			break;
		case "String":
			obj = get(o, "value");
			break;
		case "Type":
			obj = TypeUtil.parse(get(o, "value"));
			break;
		case "Handle":
			//@formatter:off
			obj = new Handle(
				getInt(o, "tag"), 
				get(o, "owner"), 
				get(o, "name"), 
				get(o, "desc"),
				getBool(o, "itf"));
			//@formatter:on
			break;
		}
		return obj;
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
		return object.getString(key, "ERROR");
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
	private static String[] getStringArray(JsonObject object, String key) {
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
	private static boolean getBool(JsonObject object, String key) {
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
	private static int getInt(JsonObject object, String key) {
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
	private static int[] getIntArray(JsonObject object, String key) {
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
		Map<String, LabelNode> labels = NamedLabelRefInsn.getLabels(block);
		// Fill in labels of labeled jump's that do not have their label's set.
		NamedLabelRefInsn.setupLabels(labels, block);
	}

	/**
	 * Static getter.
	 *
	 * @return ConfBlocks instance.
	 */
	public static ConfBlocks instance() {
		return ConfBlocks.instance(ConfBlocks.class);
	}

	public static class Block {
		public final String name;
		public final List<AbstractInsnNode> list;

		public Block(String name, List<AbstractInsnNode> list) {
			this.name = name;
			this.list = list;
		}
	}
}
