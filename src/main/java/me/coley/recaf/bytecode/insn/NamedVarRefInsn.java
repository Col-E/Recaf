package me.coley.recaf.bytecode.insn;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.parse.assembly.exception.AssemblyResolveError;
import me.coley.recaf.util.Parse;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Extended instructions with names to indicate placeholder variable locations.
 *
 * @author Matt
 */
public interface NamedVarRefInsn {
	/**
	 * @return Placeholder identifier.
	 */
	String getVarName();

	/**
	 * @param v
	 * 		Variable data to use
	 *
	 * @return Cloned instruction without serialization information. Uses the standard instruction
	 * isntance.
	 */
	AbstractInsnNode clone(Var v);

	// ========================= UTILITY METHODS ========================================= //

	/**
	 * Replace named instructions with the standard implementations.
	 *
	 * @param method
	 * 		Method instance.
	 * @param updateLocals
	 * 		Populate local variable table.
	 */
	static int clean(MethodNode method, boolean updateLocals) {
		Type type = Type.getType(method.desc);
		Type[] argTypes = type.getArgumentTypes();
		// Map of names to vars
		Map<String, Var> varMap = new LinkedHashMap<>();
		int nextIndex = 0, highest = 0;
		boolean isStatic = AccessFlag.isStatic(method.access);
		// Increase nextIndex by accounting for local variables
		Map<Integer, Integer> paramToVarIndex = new HashMap<>();
		int paramIndex = 1;
		int argSize = isStatic ? 0 : 1;
		for (Type typeArg : argTypes) {
			paramToVarIndex.put(paramIndex, argSize);
			switch(typeArg.getSort()) {
				case Type.DOUBLE:
				case Type.LONG:
					argSize += 2;
					break;
				default:
					argSize++;
			}
			paramIndex++;
		}
		nextIndex += argSize;
		// Set of opcodes to replace
		Set<NamedVarRefInsn> replaceSet = new HashSet<>();
		// Pass to find used names
		for(AbstractInsnNode ain : method.instructions.toArray()) {
			if(ain instanceof NamedVarRefInsn) {
				// Add to opcode set to replace
				NamedVarRefInsn named = (NamedVarRefInsn) ain;
				replaceSet.add(named);
				// Add to varMap
				String key = named.getVarName();
				if (!varMap.containsKey(key))
					varMap.put(key, new Var(key, ain));
			}
		}
		// Generate indices
		for(Map.Entry<String, Var> e : varMap.entrySet()) {
			String key = e.getKey();
			Var v = e.getValue();
			// Check if the key is actually a number literal or parameter value
			if (Parse.isInt(key)) {
				// Literal index given
				int index = Integer.parseInt(key);
				if(index > highest) {
					highest = index;
				}
				v.index = index;
				if (nextIndex <= index) {
					nextIndex = index + 1;
					if (v.isWide) {
						nextIndex++;
					}
				}
				continue;
			} else if (key.matches("p\\d\\w*")) {
				// Parameter index given p<index><name>
				Matcher m = new Pattern("p({INDEX}\\d+)\\w*").matcher(key);
				m.find();
				int index = Integer.parseInt(m.group("INDEX"));
				// remove "pN" of name
				v.key = v.key.substring(1 + String.valueOf(index).length());
				if (index - 1 >= argTypes.length) {
					throw new AssemblyResolveError(v.ain,
							String.format("Specified parameter does not exist, " +
									"given %d but maximum is %d.", index, argSize));
				}
				// Try to pull type from method descriptor
				v.desc = argTypes[index - 1].getDescriptor();
				// Correct the index to match the local-variable proper index
				if (paramToVarIndex.containsKey(index)) {
					index = paramToVarIndex.get(index);
				} else {
					String expected = Arrays.toString(paramToVarIndex.values().toArray(new Integer[0]));
					throw new AssemblyResolveError(v.ain,
							String.format("Specified parameter does not exist, " +
									"given %d but allowed values are: %s", index, expected));
				}
				// Update highest index if needed
				if(index > highest) {
					highest = index;
				}
				// Correct index for static methods
				if (isStatic)
					index -= 1;
				v.index = index;
				continue;
			} else if (key.equals("this")) {
				v.index = 0;
				continue;
			}
			// Find the first unused int to apply
			int index = nextIndex;
			if(index > highest) {
				highest = index;
			}
			v.index = index;
			nextIndex++;
			if (v.isWide) {
				nextIndex++;
			}
		}
		// Replace insns & update local variables
		method.localVariables = new ArrayList<>();
		LabelNode start = new LabelNode();
		LabelNode end = new LabelNode();
		if(updateLocals) {
			method.instructions.insert(start);
			method.instructions.add(end);
		}
		Set<Integer> used = new HashSet<>();
		for(NamedVarRefInsn nvri : replaceSet) {
			AbstractInsnNode index = (AbstractInsnNode) nvri;
			Var v = varMap.get(nvri.getVarName());
			// Create local variable data if necessary
			if(updateLocals && !used.contains(v.index)) {
				updateLocal(method, v, start, end);
				used.add(v.index);
			}
			// Replace
			method.instructions.set(index, nvri.clone(v));
		}
		return nextIndex;
	}

	/**
	 * Populate local variable table with the given variable
	 * @param method Method to add variable to.
	 * @param v Variable data.
	 * @param start Start label.
	 * @param end End label.
	 */
	static void updateLocal(MethodNode method, Var v, LabelNode start, LabelNode end) {
		String name = v.key;
		String desc = v.desc;
		if(desc == null) {
			String object = "Ljava/lang/Object;";
			switch(v.ain.getOpcode()) {
				case IINC:
				case ILOAD:
				case ISTORE:
					desc = "I";
					break;
				case FLOAD:
				case FSTORE:
					desc = "F";
					break;
				case DLOAD:
				case DSTORE:
					desc = "D";
					break;
				case LLOAD:
				case LSTORE:
					desc = "J";
					break;
				case ALOAD:
				case ASTORE:
				default:
					desc = object;
					break;
			}
			// Can we get the value from the previous opcode?
			if(desc.equals(object)) {
				AbstractInsnNode prev = v.ain.getPrevious();
				if(prev != null) {
					int prevOp = prev.getOpcode();
					switch(prevOp) {
						case GETFIELD:
						case GETSTATIC:
						case PUTFIELD:
						case PUTSTATIC:
							FieldInsnNode fin = (FieldInsnNode) prev;
							desc = fin.desc;
							break;
						case INVOKEINTERFACE:
						case INVOKESTATIC:
						case INVOKEVIRTUAL:
							MethodInsnNode min = (MethodInsnNode) prev;
							desc = Type.getMethodType(min.desc).getReturnType().getDescriptor();
							break;
						case INVOKESPECIAL:
							MethodInsnNode mins = (MethodInsnNode) prev;
							// Don't use return type of constructors.
							if (!mins.name.contains("<")) {
								desc = Type.getMethodType(mins.desc).getReturnType().getDescriptor();
							}
							break;
						default:
							// Can't do anything... Keep the object type
							break;
					}
				}
			}
		}
		int index = v.index;
		method.localVariables.add(new LocalVariableNode(name, desc, null, start, end, index));
	}

	/**
	 * Wrapper for variable.
	 */
	class Var {
		String key, desc;
		final boolean isWide;
		final AbstractInsnNode ain;
		int index = -1;

		Var(String key, AbstractInsnNode ain) {
			this.key = key;
			this.ain = ain;
			this.isWide = Arrays.asList(DLOAD, DSTORE, LLOAD, LSTORE)
					.contains(ain.getOpcode());
		}
	}
}
