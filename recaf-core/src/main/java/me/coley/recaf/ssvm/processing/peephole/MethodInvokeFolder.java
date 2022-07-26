package me.coley.recaf.ssvm.processing.peephole;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.asm.VMCallInsnNode;
import dev.xdark.ssvm.asm.VMOpcodes;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.util.VmValueUtil;
import me.coley.recaf.ssvm.value.TrackedValue;
import me.coley.recaf.util.InstructionUtil;
import me.coley.recaf.util.Types;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Processors for folding calls to methods when the output is a result of only constant operations.
 *
 * @author xDark
 * @author Matt Coley
 */
public class MethodInvokeFolder {
	private static final Logger logger = Logging.get(MethodInvokeFolder.class);

	/**
	 * Install processors for replacing method calls that take in constants and yield foldable values.
	 * <br>
	 * For example, consider the following snippet:
	 * <pre>
	 *     double d = Math.pow(2, 7);
	 * </pre>
	 * Will be simplified into:
	 * <pre>
	 *     double d = 128;
	 * </pre>
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 */
	public static void install(VirtualMachine vm, Predicate<ExecutionContext> whitelist) {
		VMInterface vmi = vm.getInterface();
		VMHelper helper = vm.getHelper();
		InstructionProcessor<AbstractInsnNode> invokestatic = vmi.getProcessor(VMOpcodes.VM_INVOKESTATIC);
		vmi.setProcessor(VMOpcodes.VM_INVOKESTATIC, (InstructionProcessor<VMCallInsnNode>) (insn, ctx) -> {
			Type methodType = Type.getMethodType(insn.getDelegate().desc);
			// Cannot fold types that are not supported (primitives and String only)
			if (whitelist.test(ctx)) {
				// Ensure parameter values are constant
				Stack stack = ctx.getStack();
				int stackSize = stack.position();
				boolean paramsAreConst = true;
				List<Value> argumentValues = new ArrayList<>();
				int argOffset = 0;
				Type[] argTypes = methodType.getArgumentTypes();
				for (int i = argTypes.length - 1; i >= 0; i--) {
					Type argType = argTypes[i];
					Value value = stack.getAt(stackSize - 1 - argOffset);
					paramsAreConst &= VmValueUtil.isConstant(value);
					argumentValues.add(value);
					argOffset += argType.getSize();
				}
				// If the result continues execution, and the parameters are constant, replace the
				// method call with its return value.
				Result result = invokestatic.execute(insn, ctx);
				// Execution of the invokestatic call done, stack should contain the return value on top
				if (paramsAreConst && result == Result.CONTINUE && !Types.isVoid(methodType.getReturnType())) {
					Value invokeReturn = ctx.getStack().peek();
					InsnList instructions = ctx.getMethod().getNode().instructions;
					Object returnValue = null;
					if (invokeReturn instanceof IntValue) {
						returnValue = invokeReturn.asInt();
					} else if (invokeReturn instanceof LongValue) {
						returnValue = invokeReturn.asLong();
					} else if (invokeReturn instanceof FloatValue) {
						returnValue = invokeReturn.asFloat();
					} else if (invokeReturn instanceof DoubleValue) {
						returnValue = invokeReturn.asDouble();
					} else if (invokeReturn instanceof InstanceValue) {
						// Return value must be a string
						InstanceJavaClass valueType = ((InstanceValue) invokeReturn).getJavaClass();
						if (vm.getSymbols().java_lang_String().equals(valueType))
							returnValue = helper.readUtf8(invokeReturn);
					}
					// Value found, replace it
					if (returnValue != null) {
						int contributingInstructioncount = 0;
						for (Value value : argumentValues) {
							TrackedValue trackedValue = (TrackedValue) value;
							List<AbstractInsnNode> contributingInstructions = trackedValue.getContributingInstructions();
							/* TODO: Create test cases and handle array inlining into strings when possible
							         There are a lot of funky edge cases to detect, so the below code isn't a 'stable' solution AFAIK
							if (trackedValue instanceof TrackedArrayValue) {
								TrackedArrayValue array = (TrackedArrayValue) trackedValue;
								contributingInstructions.addAll(array.getAssociatedPops());
								contributingInstructions.addAll(array.getParentValues().stream().flatMap(t -> t.getContributingInstructions().stream()).collect(Collectors.toList()));
								contributingInstructions.addAll(array.getClonedValues().stream().flatMap(t -> t.getContributingInstructions().stream()).collect(Collectors.toList()));
							}
							 */
							for (AbstractInsnNode contributingInsn : contributingInstructions)
								if (instructions.contains(contributingInsn))
									InstructionUtil.nop(instructions, contributingInsn);
							contributingInstructioncount += contributingInstructions.size();
						}
						instructions.set(insn, InstructionUtil.createPush(returnValue));
						logger.debug("Folding {} instructions in {}.{}{}",
								contributingInstructioncount,
								ctx.getOwner().getInternalName(),
								ctx.getMethod().getName(),
								ctx.getMethod().getDesc());
					}
				}
				return result;
			}
			// Use fallback processor
			return invokestatic.execute(insn, ctx);
		});
	}
}
