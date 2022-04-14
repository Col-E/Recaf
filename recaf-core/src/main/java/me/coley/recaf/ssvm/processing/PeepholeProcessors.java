package me.coley.recaf.ssvm.processing;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.ConstStringValue;
import me.coley.recaf.ssvm.value.ConstValue;
import me.coley.recaf.ssvm.value.ValueOperations;
import me.coley.recaf.util.InstructionUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

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
		for (int i : new int[]{BIPUSH, SIPUSH}) {
			vmi.setProcessor(i, (InstructionProcessor<IntInsnNode>) (insn, ctx) -> pushInt(ctx, insn.operand));
		}
		for (int i = -1; i < 5; i++) {
			int k = i;
			vmi.setProcessor(ICONST_0 + i, (insn, ctx) -> pushInt(ctx, k));
		}
		vmi.setProcessor(LCONST_0, (insn, ctx) -> pushLong(ctx, 0));
		vmi.setProcessor(LCONST_1, (insn, ctx) -> pushLong(ctx, 1));
		vmi.setProcessor(FCONST_0, (insn, ctx) -> pushFloat(ctx, 0));
		vmi.setProcessor(FCONST_1, (insn, ctx) -> pushFloat(ctx, 1));
		vmi.setProcessor(FCONST_2, (insn, ctx) -> pushFloat(ctx, 2));
		vmi.setProcessor(DCONST_0, (insn, ctx) -> pushDouble(ctx, 0));
		vmi.setProcessor(DCONST_1, (insn, ctx) -> pushDouble(ctx, 1));
		// LDC can hold a variety of types, some of which we support
		InstructionProcessor<AbstractInsnNode> ldc = vmi.getProcessor(LDC);
		vmi.setProcessor(LDC, (InstructionProcessor<LdcInsnNode>) (insn, ctx) -> {
			Object cst = insn.cst;
			Value value;
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
			ctx.getStack().push(value);
			return Result.CONTINUE;
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
		int[] INT_OP_2PARAM = {IADD, ISUB, IMUL, IDIV, ISHL, ISHR, IXOR, IREM};
		for (int intOp : INT_OP_2PARAM) {
			// Register a processor that checks if the operation parameters are constant values.
			// If they are not, use the default instruction processor.
			InstructionProcessor<AbstractInsnNode> defaultProcessor = vmi.getProcessor(intOp);
			vmi.setProcessor(intOp, (InstructionProcessor<InsnNode>) (insn, ctx) -> {
				// Pull values from stack
				Stack stack = ctx.getStack();
				List<Value> stackView = stack.view();
				int stackSize = stackView.size();
				Value v1 = stackView.get(stackSize - 2);
				Value v2 = stackView.get(stackSize - 1);
				// Check for constant values
				if (v1 instanceof ConstNumericValue && v2 instanceof ConstNumericValue) {
					// Evaluate the operation's value
					int result = ValueOperations.evaluate(intOp, v1.asInt(), v2.asInt());
					// TODO: Track contributing instructions, remove them (or use NOP if simpler) instead of POP2
					// Replace the instructions with the constant value
					InsnList instructions = ctx.getMethod().getNode().instructions;
					instructions.insertBefore(insn, new InsnNode(POP2));
					instructions.set(insn, new LdcInsnNode(result));
					// We are adding an instruction, so the position needs to be offset by one
					ctx.setInsnPosition(ctx.getInsnPosition() + 1);
					// Remove the values from the stack and replace with a new constant of the operation's value
					stack.pop();
					stack.pop();
					stack.push(ConstNumericValue.ofInt(result));
					return Result.CONTINUE;
				}
				// Non-constant values, use default processor
				return defaultProcessor.execute(insn, ctx);
			});
		}
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
				int argCount = methodType.getArgumentTypes().length;
				// Ensure parameter values are constant
				Stack stack = ctx.getStack();
				List<Value> stackView = stack.view();
				int stackSize = stackView.size();
				boolean paramsAreConst = true;
				for (int i = 0; i < argCount; i++) {
					Value value = stackView.get(stackSize - 1 - i);
					paramsAreConst &= (value instanceof ConstValue);
				}
				// If the result continues execution, and the parameters are constant, replace the
				// method call with its return value.
				Result result = invokestatic.execute(insn, ctx);
				// Execution of the invokestatic call done, stack should contain the return value on top
				if (paramsAreConst && result == Result.CONTINUE) {
					Value invokeReturn = ctx.getStack().peek();
					// TODO: replace method call and parameters instead of this dumb pop approach
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
						instructions.insert(insn, InstructionUtil.createPush(returnValue));
						instructions.insert(insn, new InsnNode(POP));
					}
				}
				return result;
			}
			// Use fallback processor
			return invokestatic.execute(insn, ctx);
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
	 * @param type Type to check.
	 * @return  {@code true} when the type is foldable <i>(Supported by {@link ConstValue})</i>
	 */
	private static boolean canFoldType(Type type) {
		return Types.isPrimitive(type) || Types.STRING_TYPE.equals(type);
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushInt(ExecutionContext ctx, int value) {
		ctx.getStack().push(ConstNumericValue.ofInt(value));
		return Result.CONTINUE;
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushLong(ExecutionContext ctx, long value) {
		ctx.getStack().push(ConstNumericValue.ofLong(value));
		return Result.CONTINUE;
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushFloat(ExecutionContext ctx, float value) {
		ctx.getStack().push(ConstNumericValue.ofFloat(value));
		return Result.CONTINUE;
	}

	/**
	 * @param ctx
	 * 		Context to push to.
	 * @param value
	 * 		Value to push.
	 *
	 * @return {@link Result#CONTINUE}.
	 */
	private static Result pushDouble(ExecutionContext ctx, double value) {
		ctx.getStack().push(ConstNumericValue.ofDouble(value));
		return Result.CONTINUE;
	}
}
