package me.coley.recaf.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.OpcodeUtil;

public class BlocksConfig extends Config {
	private final static String ERR = "Could not parse saved block content.";

	public Map<String, List<AbstractInsnNode>> blocks = new HashMap<>();

	public BlocksConfig() {
		super("rcblocks");
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
			case AbstractInsnNode.TABLESWITCH_INSN:
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
			case AbstractInsnNode.FRAME:
			case AbstractInsnNode.LABEL:
			case AbstractInsnNode.JUMP_INSN:
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

	private AbstractInsnNode parse(JsonObject o) {
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
			return new IincInsnNode(opcode, getI(o, "var"));
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
			return new LineNumberNode(getI(o, "line"), new LabelNode());
		}
		}
		throw new UnsupportedOperationException("Unsupported opcode <load> : " + opcodeName + " : " + opcode + "(type:" + type
				+ ")");
	}

	private String get(JsonObject object, String key) {
		return object.getString(key, ERR);
	}

	private boolean getB(JsonObject object, String key) {
		return object.getBoolean(key, false);
	}

	private int getI(JsonObject object, String key) {
		return object.getInt(key, -1);
	}

	public void add(String key, List<AbstractInsnNode> list) {
		// TODO: Support of classes shown in the switch below,
		// For now, flat-out reject saving of anything containing them.

		// Create map of new labels
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode ain : list) {
			switch (ain.getType()) {
			case AbstractInsnNode.TABLESWITCH_INSN:
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
			case AbstractInsnNode.FRAME:
			case AbstractInsnNode.LABEL:
			case AbstractInsnNode.JUMP_INSN:
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				// Skip saving anything containing this type.
				Recaf.INSTANCE.gui.displayError(new UnsupportedOperationException("Unsupported opcode: " + ain.getOpcode() + ":"
						+ OpcodeUtil.opcodeToName(ain.getOpcode())));
				return;
			default:
				break;
			}
			if (ain instanceof LabelNode) {
				labels.put((LabelNode) ain, new LabelNode());
			}
		}
		// Clone to prevent synchronization issues
		List<AbstractInsnNode> clone = new ArrayList<>();
		for (AbstractInsnNode ain : list) {
			clone.add(ain.clone(labels));
		}
		blocks.put(key, clone);
		// Save changes
		save();
	}
}
