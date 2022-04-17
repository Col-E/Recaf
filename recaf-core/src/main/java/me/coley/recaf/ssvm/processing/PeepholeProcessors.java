package me.coley.recaf.ssvm.processing;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.util.VmValueUtil;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.ConstStringValue;
import me.coley.recaf.ssvm.value.ConstValue;
import me.coley.recaf.ssvm.value.TrackedValue;
import me.coley.recaf.util.InstructionUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static me.coley.recaf.ssvm.value.ConstNumericValue.*;
import static me.coley.recaf.ssvm.value.ValueOperations.evaluate;

/**
 * Utility to install peephole optimization to a {@link VirtualMachine}.
 *
 * @author Matt Coley
 * @author xDark
 */
public class PeepholeProcessors implements Opcodes {
	/**
	 * Installs all peephole processors.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	public static void installAll(VirtualMachine vm) {
		installValuePushing(vm);
		installOperationFolding(vm);
		installReturnValueFolding(vm);
	}

	/**
	 * Install processors for instructions that push constant values onto the stack.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	public static void installValuePushing(VirtualMachine vm) {
		VMInterface vmi = vm.getInterface();
		VMHelper helper = vm.getHelper();
		// We take the value pushed by instructions like 'SIPUSH/BIPUSH <value>' and map it to a custom value class.
		// This 'ConstantValue' class is checked later for mathematical simplification/folding.
		// The same idea applies to other constant value instructions.
		for (int i : new int[]{BIPUSH, SIPUSH})
			vmi.setProcessor(i, (InstructionProcessor<IntInsnNode>) (insn, ctx) -> pushInt(ctx, insn, insn.operand));
		for (int i = -1; i <= 5; i++) {
			int k = i;
			vmi.setProcessor(ICONST_0 + i, (insn, ctx) -> pushInt(ctx, insn, k));
		}
		vmi.setProcessor(LCONST_0, (insn, ctx) -> pushLong(ctx, insn, 0));
		vmi.setProcessor(LCONST_1, (insn, ctx) -> pushLong(ctx, insn, 1));
		vmi.setProcessor(FCONST_0, (insn, ctx) -> pushFloat(ctx, insn, 0));
		vmi.setProcessor(FCONST_1, (insn, ctx) -> pushFloat(ctx, insn, 1));
		vmi.setProcessor(FCONST_2, (insn, ctx) -> pushFloat(ctx, insn, 2));
		vmi.setProcessor(DCONST_0, (insn, ctx) -> pushDouble(ctx, insn, 0));
		vmi.setProcessor(DCONST_1, (insn, ctx) -> pushDouble(ctx, insn, 1));
		// Need to handle stack manipulation like DUP
		installStackManipulationInstructionTracking(vm);
		// LDC can hold a variety of types, some of which we support
		InstructionProcessor<AbstractInsnNode> ldc = vmi.getProcessor(LDC);
		vmi.setProcessor(LDC, (InstructionProcessor<LdcInsnNode>) (insn, ctx) -> {
			Object cst = insn.cst;
			ConstValue value;
			if (cst instanceof Integer) {
				value = ConstNumericValue.ofInt((Integer) cst);
			} else if (cst instanceof Long) {
				value = ConstNumericValue.ofLong((Long) cst);
			} else if (cst instanceof Float) {
				value = ConstNumericValue.ofFloat((Float) cst);
			} else if (cst instanceof Double) {
				value = ConstNumericValue.ofDouble((Double) cst);
			} else if (cst instanceof String) {
				value = ConstStringValue.ofString(helper, (String) cst);
			} else {
				// We don't support the type of LDC constant used.
				return ldc.execute(insn, ctx);
			}
			return push(ctx, insn, value);
		});
	}

	/**
	 * Install processors for instructions that operate on mathematical types.
	 * If the values are {@link me.coley.recaf.ssvm.value.ConstValue} instances,
	 * we should be able to fold them without any changes to behavior.
	 * <br>
	 * For example, consider the following snippet:
	 * <pre>
	 *     int i = 10 * ((8 + 8) + (8 / 2) >> 1);
	 * </pre>
	 * Will be simplified into:
	 * <pre>
	 *     int i = 100;
	 * </pre>
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 *
	 * @see #installValuePushing(VirtualMachine) Required in order to detect foldable values.
	 */
	public static void installOperationFolding(VirtualMachine vm) {
		VMInterface vmi = vm.getInterface();
		// Integers
		int[] INT_OP_2PARAM = {IADD, ISUB, IMUL, IDIV, ISHL, ISHR, IUSHR, IXOR, IREM};
		for (int op : INT_OP_2PARAM)
			handleOperationFolding(vmi, op, false, false, (v1, v2) -> ofInt(evaluate(op, v1.asInt(), v2.asInt())));
		// Longs (wide)
		int[] LONG_OP_2PARAM = {LADD, LSUB, LMUL, LDIV, LXOR, LREM};
		for (int op : LONG_OP_2PARAM)
			handleOperationFolding(vmi, op, true, true, (v1, v2) -> ofLong(evaluate(op, v1.asLong(), v2.asLong())));
		LONG_OP_2PARAM = new int[]{LSHL, LSHR, LUSHR};
		for (int op : LONG_OP_2PARAM)
			handleOperationFolding(vmi, op, true, false, (v1, v2) -> ofLong(evaluate(op, v1.asLong(), v2.asLong())));
		// Floats
		int[] FLOAT_OP_2PARAM = {FADD, FSUB, FMUL, FDIV, FREM};
		for (int op : FLOAT_OP_2PARAM)
			handleOperationFolding(vmi, op, false, false, (v1, v2) -> ofFloat(evaluate(op, v1.asFloat(), v2.asFloat())));
		// Doubles (wide)
		int[] DOUBLE_OP_2PARAM = {DADD, DSUB, DMUL, DDIV, DREM};
		for (int op : DOUBLE_OP_2PARAM)
			handleOperationFolding(vmi, op, true, false, (v1, v2) -> ofDouble(evaluate(op, v1.asDouble(), v2.asDouble())));
	}

	/**
	 * @param vmi
	 * 		Virtual machine interface to register processors on.
	 * @param op
	 * 		Operation to register for.
	 * @param v1wide
	 *        {@code true} if the first operation parameter is a wide type.
	 * @param v2wide
	 *        {@code true} if the second operation parameter is a wide type.
	 * @param compute
	 * 		Value computation function.
	 *
	 * @see #installOperationFolding(VirtualMachine)
	 */
	private static void handleOperationFolding(VMInterface vmi, int op, boolean v1wide, boolean v2wide,
											   BiFunction<Value, Value, TrackedValue> compute) {
		// Register a processor that checks if the operation parameters are constant values.
		// If they are not, use the default instruction processor.
		InstructionProcessor<AbstractInsnNode> defaultProcessor = vmi.getProcessor(op);
		vmi.setProcessor(op, (InstructionProcessor<InsnNode>) (insn, ctx) -> {
			// Pull values from stack
			Stack stack = ctx.getStack();
			Value v1 = stack.getAt(stack.position() - (v1wide ? 4 : 2));
			Value v2 = stack.getAt(stack.position() - (v2wide ? 2 : 1));
			// Check for constant values
			if (v1 instanceof ConstNumericValue && v2 instanceof ConstNumericValue) {
				// Take both parameters, compute the operation value, and push onto the stack.
				TrackedValue operationValue = compute.apply(v1, v2);
				AbstractInsnNode operationValuePushInsn = VmValueUtil.createConstInsn(operationValue);
				if (v2wide) stack.popWide();
				else stack.pop();
				if (v1wide) stack.popWide();
				else stack.pop();
				// Pushed value size matches 1st parameter
				if (v1wide) {
					stack.pushWide(operationValue);
				} else {
					stack.push(operationValue);
				}
				// Record the contributing instructions from the two values used in this operation.
				operationValue.addContributing((ConstNumericValue) v1, (ConstNumericValue) v2);
				// Replace the instructions that contribute to the arguments with NOP.
				InsnList instructions = ctx.getMethod().getNode().instructions;
				List<AbstractInsnNode> contributingInstructions = operationValue.getContributingInstructions();
				for (AbstractInsnNode contributingInsn : contributingInstructions)
					if (instructions.contains(contributingInsn))
						instructions.set(contributingInsn, new InsnNode(NOP));
				// Replace the method invoke with the constant value
				instructions.set(insn, operationValuePushInsn);
				// Record the updated instruction (so if this value needs to be folded later it can be)
				operationValue.addContributing(operationValuePushInsn);
				return Result.CONTINUE;
			}
			// Non-constant values, use default processor
			return defaultProcessor.execute(insn, ctx);
		});
	}

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
	 *
	 * @see #installValuePushing(VirtualMachine) Required in order to detect foldable values.
	 */
	public static void installReturnValueFolding(VirtualMachine vm) {
		VMInterface vmi = vm.getInterface();
		VMHelper helper = vm.getHelper();
		InstructionProcessor<AbstractInsnNode> invokestatic = vmi.getProcessor(INVOKESTATIC);
		vmi.setProcessor(INVOKESTATIC, (InstructionProcessor<MethodInsnNode>) (insn, ctx) -> {
			Type methodType = Type.getMethodType(insn.desc);
			// Cannot fold types that are not supported (primitives and String only)
			if (canFoldMethodType(methodType)) {
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
					paramsAreConst &= (value instanceof ConstValue);
					argumentValues.add(value);
					argOffset += argType.getSize();
				}
				// If the result continues execution, and the parameters are constant, replace the
				// method call with its return value.
				Result result = invokestatic.execute(insn, ctx);
				// Execution of the invokestatic call done, stack should contain the return value on top
				if (paramsAreConst && result == Result.CONTINUE) {
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
						returnValue = helper.readUtf8(invokeReturn);
					}
					// Value found, replace it
					if (returnValue != null) {
						for (Value value : argumentValues) {
							TrackedValue trackedValue = (TrackedValue) value;
							List<AbstractInsnNode> contributingInstructions = trackedValue.getContributingInstructions();
							for (AbstractInsnNode contributingInsn : contributingInstructions)
								if (instructions.contains(contributingInsn))
									instructions.set(contributingInsn, new InsnNode(NOP));
						}
						instructions.set(insn, InstructionUtil.createPush(returnValue));
					}
				}
				return result;
			}
			// Use fallback processor
			return invokestatic.execute(insn, ctx);
		});
	}

	/**
	 * Due to the default implementation of stack manipulating instructions in {@link dev.xdark.ssvm.NativeJava}
	 * the state of {@link TrackedValue#getContributingInstructions()} can be polluted.
	 * <br>
	 * Consider you have the following code:
	 * <pre>
	 *     ICONST_2
	 *     DUP
	 *     IMUL
	 * </pre>
	 * When the {@code DUP} is encountered we need to update the duplicated {@link TrackedValue}
	 * on the stack that the {@code DUP} contributed to it. But the default handling copies the
	 * {@link Value} instance, which means technically even the value for {@code ICONST_2} is
	 * aware of the contributing {@code DUP}, which is incorrect.
	 * <br>
	 * This installs processors to ensure that does not happen.
	 * <br>
	 * Most of the logic is copy-pasted from {@link Stack}. See:
	 * <ul>
	 *     <li>{@link Stack#dup()}</li>
	 *     <li>{@link Stack#dupx1()}</li>
	 *     <li>{@link Stack#dupx2()}</li>
	 *     <li>{@link Stack#dup2()}</li>
	 *     <li>{@link Stack#dup2x1()}</li>
	 *     <li>{@link Stack#dup2x2()}</li>
	 *     <li>{@link Stack#swap()}</li>
	 *     <li>{@link Stack#pop()}</li>
	 * </ul>
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	private static void installStackManipulationInstructionTracking(VirtualMachine vm) {
		VMInterface vmi = vm.getInterface();
		vmi.setProcessor(DUP, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			Value c1 = cloneAndContribute(v1, insn);
			stack.push(v1);
			stack.push(c1);
			return Result.CONTINUE;
		});
		vmi.setProcessor(DUP2, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.popGeneric();
			Value c1 = cloneAndContribute(v1, insn);
			if (v1.isWide()) {
				stack.pushWide(v1);
				stack.pushWide(c1);
			} else {
				Value v2 = stack.pop();
				Value c2 = cloneAndContribute(v2, insn);
				stack.push(v2);
				stack.push(v1);
				stack.push(c2);
				stack.push(c1);
			}
			return Result.CONTINUE;
		});
		vmi.setProcessor(DUP_X1, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			Value v2 = stack.pop();
			Value c1 = cloneAndContribute(v1, insn);
			stack.push(c1);
			stack.push(v2);
			stack.push(v1);
			return Result.CONTINUE;
		});
		vmi.setProcessor(DUP_X2, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			Value v2 = stack.popGeneric();
			Value c1 = cloneAndContribute(v1, insn);
			if (v2.isWide()) {
				stack.push(c1);
				stack.pushWide(v2);
				stack.push(v1);
			} else {
				Value v3 = stack.pop();
				stack.push(c1);
				stack.push(v3);
				stack.push(v2);
				stack.push(v1);
			}
			return Result.CONTINUE;
		});
		vmi.setProcessor(DUP2_X1, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.popGeneric();
			Value c1 = cloneAndContribute(v1, insn);
			if (v1.isWide()) {
				Value v2 = stack.pop();
				stack.pushWide(c1);
				stack.push(v2);
				stack.pushWide(v1);
			} else {
				Value v2 = stack.pop();
				Value v3 = stack.pop();
				Value c2 = cloneAndContribute(v2, insn);
				stack.push(c2);
				stack.push(c1);
				stack.push(v3);
				stack.push(v2);
				stack.push(v1);
			}
			return Result.CONTINUE;
		});
		vmi.setProcessor(DUP2_X2, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.popGeneric();
			Value v2 = stack.popGeneric();
			Value c1 = cloneAndContribute(v1, insn);
			if (v1.isWide()) {
				if (v2.isWide()) {
					stack.pushWide(c1);
					stack.pushWide(v2);
					stack.pushWide(v1);
				} else {
					Value v3 = stack.pop();
					stack.pushWide(c1);
					stack.push(v3);
					stack.push(v2);
					stack.push(v1);
				}
			} else {
				Value c2 = cloneAndContribute(v2, insn);
				Value v3 = stack.popGeneric();
				if (v3.isWide()) {
					stack.push(c2);
					stack.push(c1);
					stack.pushWide(v3);
					stack.push(v2);
					stack.push(v1);
				} else {
					Value v4 = stack.popGeneric();
					stack.push(c2);
					stack.push(c1);
					stack.push(v4);
					stack.push(v3);
					stack.push(v2);
					stack.push(v1);
				}
			}
			return Result.CONTINUE;
		});
		vmi.setProcessor(SWAP, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			Value v2 = stack.pop();
			Value c1 = cloneAndContribute(v1, insn);
			Value c2 = cloneAndContribute(v2, insn);
			stack.push(c1);
			stack.push(c2);
			return Result.CONTINUE;
		});
		// We need to track the values affected by POP/POP2 so that later if we remove instructions that
		// contribute to the values being popped off the stack, that the pop instructions themselves also get removed.
		Multimap<ExecutionContext, TrackedValue> valuesWithPops =
				MultimapBuilder.ListMultimapBuilder.hashKeys().arrayListValues().build();
		vmi.setProcessor(POP, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			trackPop(valuesWithPops, insn, ctx, v1);
			return Result.CONTINUE;
		});
		vmi.setProcessor(POP2, (insn, ctx) -> {
			Stack stack = ctx.getStack();
			Value v1 = stack.pop();
			Value v2 = stack.pop();
			trackPop(valuesWithPops, insn, ctx, v1);
			trackPop(valuesWithPops, insn, ctx, v2);
			return Result.CONTINUE;
		});
		// When the method ends, clear any POP/POP2 instructions that have their contributing instructions removed.
		vmi.registerMethodExit(ctx -> {
			InsnList instructions = ctx.getMethod().getNode().instructions;
			for (TrackedValue value : valuesWithPops.removeAll(ctx)) {
				if (value.getContributingInstructions().stream().noneMatch(instructions::contains)) {
					value.getAssociatedPops().forEach(insn -> instructions.set(insn, new InsnNode(NOP)));
				}
			}
		});
	}

	/**
	 * @param methodType
	 * 		Type of the method (args/ret).
	 *
	 * @return {@code true} when all types are foldable <i>(Supported by {@link ConstValue})</i>
	 */
	private static boolean canFoldMethodType(Type methodType) {
		// All arguments must be foldable (primitives and String)
		Type[] argTypes = methodType.getArgumentTypes();
		for (Type argType : argTypes) {
			if (!canFoldType(argType)) {
				return false;
			}
		}
		// Same goes for return type.
		Type returnType = methodType.getReturnType();
		return canFoldType(returnType);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} when the type is foldable <i>(Supported by {@link ConstValue})</i>
	 */
	private static boolean canFoldType(Type type) {
		return Types.isPrimitive(type) || Types.STRING_TYPE.equals(type);
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param insn
	 * 		Instruction executed.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushInt(ExecutionContext ctx, AbstractInsnNode insn, int value) {
		return push(ctx, insn, ConstNumericValue.ofInt(value));
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param insn
	 * 		Instruction executed.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushLong(ExecutionContext ctx, AbstractInsnNode insn, long value) {
		return push(ctx, insn, ConstNumericValue.ofLong(value));
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param insn
	 * 		Instruction executed.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushFloat(ExecutionContext ctx, AbstractInsnNode insn, float value) {
		return push(ctx, insn, ConstNumericValue.ofFloat(value));
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param insn
	 * 		Instruction executed.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushDouble(ExecutionContext ctx, AbstractInsnNode insn, double value) {
		return push(ctx, insn, ConstNumericValue.ofDouble(value));
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param insn
	 * 		Instruction executed.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result push(ExecutionContext ctx, AbstractInsnNode insn, ConstValue value) {
		value.addContributing(insn);
		ctx.getStack().pushGeneric(value);
		return Result.CONTINUE;
	}

	/**
	 * @param value
	 * 		Value to clone.
	 * @param insn
	 * 		Instruction to {@link TrackedValue#addContributing(AbstractInsnNode) add}.
	 *
	 * @return Copy of value for {@link TrackedValue} types. Otherwise, same instance.
	 */
	private static Value cloneAndContribute(Value value, AbstractInsnNode insn) {
		Value clone = clone(value);
		if (clone instanceof TrackedValue)
			((TrackedValue) clone).addContributing(insn);
		return clone;
	}

	/**
	 * @param value
	 * 		Value to clone.
	 *
	 * @return Copy of value for {@link TrackedValue} types. Otherwise, same instance.
	 */
	private static Value clone(Value value) {
		// If the value is an instance of 'TrackedValue' we need to create a clone.
		if (value instanceof TrackedValue) {
			TrackedValue original = (TrackedValue) value;
			TrackedValue clone = original.clone();
			clone.addContributing(original);
			original.addClonedValue(clone);
			return clone;
		}
		// We don't care about value types that aren't used for constant tracking.
		return value;
	}

	/**
	 * @param map
	 * 		Map to record values that are affected by {@code POP}/{@code POP2}.
	 * @param insn
	 * 		The instruction, opcode being either {@code POP} or {@code POP2}.
	 * @param ctx
	 * 		Execution context of the method.
	 * @param value
	 * 		The value being popped.
	 *
	 * @see #installStackManipulationInstructionTracking(VirtualMachine) Usage for tracking {@code POP}/{@code POP2}.
	 */
	private static void trackPop(Multimap<ExecutionContext, TrackedValue> map,
								 AbstractInsnNode insn, ExecutionContext ctx, Value value) {
		if (value instanceof TrackedValue) {
			((TrackedValue) value).addAssociatedPop(insn);
			map.put(ctx, (TrackedValue) value);
		}
	}
}
