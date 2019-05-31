package me.coley.recaf.bytecode.analysis;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.bytecode.insn.ParameterValInsnNode;

/**
 * Analyzer that allows SourceValue values to be shown from parameter-set
 * values.
 * 
 * @author Matt
 */
public class SourceAnalyzer extends Analyzer<SourceValue> {
	/**
	 * Copy since super-class declaration is private.
	 */
	private final Interpreter<SourceValue> interpreter;

	public SourceAnalyzer(Interpreter<SourceValue> interpreter) {
		super(interpreter);
		this.interpreter = interpreter;
	}

	@Override
	protected void init(final String owner, final MethodNode method) throws AnalyzerException {
		Frame<SourceValue> initial = getFrames()[0];
		Frame<SourceValue> modified = computeInitialFrame(owner, method);
		for (int l = 0; l < method.maxLocals; l++) {
			initial.setLocal(l, modified.getLocal(l));
		}
	}

	/**
	 * Modified from parent to call {@link #valueArg(Type, ParameterNode)}.
	 * 
	 * @param owner
	 * @param method
	 * @return
	 */
	public Frame<SourceValue> computeInitialFrame(final String owner, final MethodNode method) {
		Frame<SourceValue> frame = newFrame(method.maxLocals, method.maxStack);
		int currentLocal = 0;
		if (!AccessFlag.isStatic(method.access)) {
			Type ownerType = Type.getObjectType(owner);
			frame.setLocal(currentLocal++, valueArg(0, ownerType, null));
		}
		Type[] argumentTypes = Type.getArgumentTypes(method.desc);
		for (int i = 0; i < argumentTypes.length; ++i) {
			ParameterNode param = method.parameters == null ? null : method.parameters.get(i);
			int k = currentLocal++;
			frame.setLocal(k, valueArg(k, argumentTypes[i], param));
			if (argumentTypes[i].getSize() == 2) {
				frame.setLocal(currentLocal++, interpreter.newValue(null));
			}
		}
		while (currentLocal < method.maxLocals) {
			frame.setLocal(currentLocal++, interpreter.newValue(null));
		}
		frame.setReturn(valueArg(-1, Type.getReturnType(method.desc), null));
		return frame;
	}

	/**
	 * Dummy opcode for SourceValue based on parameter value.
	 * 
	 * @param type
	 *            Type of value.
	 * @param parameter
	 *            Optional parameter-node for extra debug information.
	 * @return Dummy.
	 */
	private SourceValue valueArg(int index, Type type, ParameterNode parameter) {
		return new SourceValue(type == null ? 1 : type.getSize(), new ParameterValInsnNode(index, type, parameter));
	}
}
