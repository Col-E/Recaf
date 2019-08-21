package me.coley.recaf.parse.assembly;

import me.coley.recaf.util.*;
import me.coley.recaf.util.struct.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Variable manager.
 *
 * @author Matt
 */
public class Variables {
	private int currentVarIndex = 0;
	private Map<String, Integer> varNameToIndex = new LinkedHashMap<>();
	private Map<Integer, Integer> varToType = new LinkedHashMap<>();

	/**
	 * @return Set of variable names registered.
	 */
	public Set<String> names() {
		return varNameToIndex.keySet();
	}

	/**
	 * @return Set of indices registered.
	 */
	public Set<Integer> indices() {
		return varToType.keySet();
	}

	/**
	 * @param name
	 * 		Variable name.
	 *
	 * @return Index of variable.
	 */
	public int getIndex(String name) {
		if(name.matches("\\d+"))
			return Integer.parseInt(name);
		return varNameToIndex.get(name);
	}

	/**
	 * @param index
	 * 		Variable index.
	 *
	 * @return Variable's {@link Type#getSort() sort}.
	 */
	public int getSort(int index) {
		return varToType.get(index);
	}

	/**
	 * Registers variables to <i>"this"</i> and method paramters.
	 *
	 * @param access
	 * 		Method access flag mask.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @throws IllegalArgumentException
	 * 		When the setup failed to register all variable indices.
	 */
	public void setup(int access, String desc) {
		try {
			// Fill "this" variable index
			if (!AccessFlag.isStatic(access))
				register("this", ALOAD);
			// Fill method parameter variable indices
			Type methodType = Type.getMethodType(desc);
			for (Type argType : methodType.getArgumentTypes()) {
				int opcode = getDummyOp(argType);
				register(null, opcode);
			}
		} catch(LineParseException ex) {
			throw new IllegalArgumentException("Virtual setup in assembly failed!", ex);
		}
	}

	/**
	 * Registers the variable.
	 *
	 * @param lvn
	 * 		Local variable node.
	 *
	 * @return Index of local.
	 *
	 * @throws IllegalArgumentException
	 * 		When the variable node has an invalid type.
	 */
	public int register(LocalVariableNode lvn) throws IllegalArgumentException {
		try {
			// decode
			int opcode = getDummyOp(Type.getType(lvn.desc));
			Pair<Integer, Integer> typeInfo = getTypeInfo(opcode);
			int type = typeInfo.getKey();
			int typeSize = typeInfo.getValue();
			// update mpas
			varNameToIndex.put(lvn.name, lvn.index);
			varToType.put(lvn.index, type);
			// update next index
			int next = lvn.index + typeSize;
			if (currentVarIndex < next)
				currentVarIndex = next;
			return lvn.index;
		} catch(LineParseException ex) {
			throw new IllegalArgumentException("Failed to register variable:", ex);
		}
	}

	/**
	 * @param name
	 * 		Variable name.
	 * @param opcode
	 * 		Variable opcode, used to indicate type.
	 *
	 * @return Variable index.
	 *
	 * @throws LineParseException
	 * 		When a variable was incorrectly used.
	 */
	public int register(String name, int opcode) throws LineParseException {
		// Fetch type from opcode
		Pair<Integer, Integer> typeInfo = getTypeInfo(opcode);
		int type = typeInfo.getKey();
		int typeSize = typeInfo.getValue();
		// Get index
		int index = -1;
		if(name == null) {
			index = currentVarIndex;
			currentVarIndex += typeSize;
		} else if(name.matches("\\d+")) {
			index = Integer.parseInt(name);
		} else if (varNameToIndex.containsKey(name)){
			index = varNameToIndex.get(name);
		} else {
			index = currentVarIndex;
			currentVarIndex += typeSize;
			varNameToIndex.put(name, index);
		}
		// Verify consistent type usage
		if (varToType.containsKey(index)) {
			int existingType = varToType.get(index);
			if (existingType != type)
				throw new LineParseException(name, "Variable sort specified as " +
						TypeUtil.sortToString(type) + " but expected sort " +
						TypeUtil.sortToString(existingType));
		} else
			varToType.put(index, type);
		return index;
	}

	/**
	 * @param opcode
	 * 		Var opcode.
	 *
	 * @return Type information derived from the opcode.
	 *
	 * @throws LineParseException
	 * 		When the opcode given was not supported.
	 */
	private static Pair<Integer, Integer> getTypeInfo(int opcode) throws LineParseException {
		int type = -1;
		int typeSize = 1;
		switch(opcode) {
			case ALOAD:
			case ASTORE:
				type = Type.OBJECT;
				break;
			case IINC:
			case ILOAD:
			case ISTORE:
				type = Type.INT;
				break;
			case FLOAD:
			case FSTORE:
				type = Type.FLOAT;
				break;
			case DLOAD:
			case DSTORE:
				type = Type.DOUBLE;
				typeSize = 2;
				break;
			case LLOAD:
			case LSTORE:
				type = Type.LONG;
				typeSize = 2;
				break;
			default:
				String op = OpcodeUtil.opcodeToName(opcode);
				throw new LineParseException(op, "Variable specified with unsupported opcode: " + op);
		}
		return new Pair<>(type, typeSize);
	}

	/**
	 * @param type Some type.
	 * @return An var opcode that deals with the given type's sort.
	 */
	private static int getDummyOp(Type type) {
		switch(type.getSort()) {
			case Type.ARRAY:
			case Type.OBJECT:
				return ALOAD;
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.SHORT:
			case Type.CHAR:
			case Type.INT:
				return ILOAD;
			case Type.FLOAT:
				return FLOAD;
			case Type.DOUBLE:
				return DLOAD;
			case Type.LONG:
				return DLOAD;
			default:
				throw new IllegalStateException("Unsupported method parameter type: " + type);
		}
	}
}
