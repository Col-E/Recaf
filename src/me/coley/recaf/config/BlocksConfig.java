package me.coley.recaf.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.tree.*;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.asm.OpcodeUtil;

public class BlocksConfig extends Config {
	public Map<String, List<AbstractInsnNode>> blocks = new HashMap<>();

	public BlocksConfig() {
		super("rcblocks");
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		JsonObject v = new JsonObject();
		if (type.isAssignableFrom(blocks.getClass())) {
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
				throw new UnsupportedOperationException("Unsupported opcode: " + OpcodeUtil.opcodeToName(ain.getOpcode()));
			}
		}
		return v;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		return null;
	}

	public void add(String key, List<AbstractInsnNode> list) {
		// Create map of new labels
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode ain : list) {
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
		System.out.println(key + ":" + clone.size());
		// Save changes
		save();
	}
}
