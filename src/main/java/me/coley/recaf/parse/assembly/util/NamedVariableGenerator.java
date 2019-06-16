package me.coley.recaf.parse.assembly.util;

import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.bytecode.InsnUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Correct variable names for text generation of variable instructions.
 *
 * @author Matt
 */
public interface NamedVariableGenerator {
	/**
	 * Correct the name of the given variable. Used in generation of text representatiosn of
	 * variable-referencing instructions in the assembler.
	 *
	 * @param method
	 * 		Method containing the instruction.
	 * @param index
	 * 		Instruction's variable index.
	 * @param name
	 * 		Instruction's current name.
	 *
	 * @return Updated name.
	 */
	default String name(MethodNode method, int index, String name) {
		boolean isStatic = AccessFlag.isStatic(method.access);
		if(index == 0 && !isStatic) {
			return "this";
		}
		Map<Integer, String> varNames = new HashMap<>();
		if (!isStatic) {
			varNames.put(0, "this");
		}
		Type type = Type.getMethodType(method.desc);
		int argSize = isStatic ? 0 : 1;
		int pIndex = 1;
		for(Type typeArg : type.getArgumentTypes()) {
			LocalVariableNode lvn = InsnUtil.getLocal(method, argSize);
			if(lvn != null) {
				varNames.put(argSize, "p" + pIndex + lvn.name);
			}
			switch(typeArg.getSort()) {
				case Type.DOUBLE:
				case Type.LONG:
					argSize += 2;
					break;
				default:
					argSize++;
			}
			pIndex++;
		}
		// The p<N> pattern used by the assembler forces parameter counting to start at 1.
		// So the due to static methods not reserving '0:this' the index needs to be offset.
		if(isStatic && index < argSize) {
			String varName = varNames.get(index);
			if(varName != null)
				return varName;
		} else if(!isStatic && index > 0 && index <= argSize) {
			String varName = varNames.get(index);
			if(varName != null)
				return varName;
		}
		return name;
	}
}
