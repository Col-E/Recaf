package me.coley.recaf.ssvm.processing.peephole;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.ssvm.util.VmValueUtil;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.TrackedValue;
import me.coley.recaf.util.InstructionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static me.coley.recaf.ssvm.value.ConstNumericValue.*;
import static me.coley.recaf.ssvm.value.ValueOperations.evaluate;

/**
 * Processors for folding math operations acting on constant values into a single value.
 *
 * @author Matt Coley
 * @author xDark
 */
public class MathOperationFolder {
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
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 */
	public static void install(VirtualMachine vm, Predicate<ExecutionContext> whitelist) {
		VMInterface vmi = vm.getInterface();
		// Integers
		int[] INT_OP_2PARAM = {Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR, Opcodes.IREM};
		for (int op : INT_OP_2PARAM)
			handleBiOperationFolding(vmi, whitelist, op, false, false, (v1, v2) -> ofInt(evaluate(op, v1.asInt(), v2.asInt())));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.INEG, false, false, true, v -> ofInt(-v.asInt()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2B, false, false, false, v -> ofInt(v.asByte()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2C, false, false, true, v -> ofInt(v.asChar()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2S, false, false, true, v -> ofInt(v.asShort()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2L, false, true, true, v -> ofLong(v.asLong()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2F, false, false, true, v -> ofFloat(v.asFloat()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.I2D, false, true, true, v -> ofDouble(v.asDouble()));
		// Longs (wide)
		int[] LONG_OP_2PARAM = {Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR, Opcodes.LREM, Opcodes.LCMP};
		for (int op : LONG_OP_2PARAM)
			handleBiOperationFolding(vmi, whitelist, op, true, true, (v1, v2) -> ofLong(evaluate(op, v1.asLong(), v2.asLong())));
		LONG_OP_2PARAM = new int[]{Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR};
		for (int op : LONG_OP_2PARAM)
			handleBiOperationFolding(vmi, whitelist, op, true, false, (v1, v2) -> ofLong(evaluate(op, v1.asLong(), v2.asLong())));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.LNEG, true, true, true, v -> ofLong(-v.asLong()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.L2I, true, false, true, v -> ofInt(v.asInt()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.L2F, true, false, true, v -> ofFloat(v.asFloat()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.L2D, true, true, true, v -> ofDouble(v.asDouble()));
		// Floats
		int[] FLOAT_OP_2PARAM = {Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.FCMPG, Opcodes.FCMPL};
		for (int op : FLOAT_OP_2PARAM)
			handleBiOperationFolding(vmi, whitelist, op, false, false, (v1, v2) -> ofFloat(evaluate(op, v1.asFloat(), v2.asFloat())));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.FNEG, false, false, true, v -> ofFloat(-v.asFloat()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.F2I, false, false, true, v -> ofInt(v.asInt()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.F2L, false, true, true, v -> ofLong(v.asLong()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.F2D, false, true, true, v -> ofDouble(v.asDouble()));
		// Doubles (wide)
		int[] DOUBLE_OP_2PARAM = {Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM, Opcodes.DCMPG, Opcodes.DCMPL};
		for (int op : DOUBLE_OP_2PARAM)
			handleBiOperationFolding(vmi, whitelist, op, true, false, (v1, v2) -> ofDouble(evaluate(op, v1.asDouble(), v2.asDouble())));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.DNEG, true, true, true, v -> ofDouble(-v.asDouble()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.D2I, true, false, true, v -> ofInt(v.asInt()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.D2F, true, false, true, v -> ofFloat(v.asFloat()));
		handleUnaryOperationFolding(vmi, whitelist, Opcodes.D2F, true, false, true, v -> ofFloat(v.asFloat()));
	}

	/**
	 * @param vmi
	 * 		Virtual machine interface to register processors on.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 * @param op
	 * 		Operation to register for.
	 * @param wideIn
	 *        {@code true} if the parameter is a wide type.
	 * @param wideOut
	 *        {@code true} if the operation result is wide.
	 * @param replace
	 *        {@code true} to fold the operation. Some values should not be folded, like {@code I2B}.
	 * 		Even so, we still want to track their constant value.
	 * @param compute
	 * 		Value computation function.
	 *
	 * @see #install(VirtualMachine, Predicate)
	 */
	private static void handleUnaryOperationFolding(VMInterface vmi, Predicate<ExecutionContext> whitelist,
													int op, boolean wideIn, boolean wideOut, boolean replace,
													Function<Value, TrackedValue> compute) {
		// Register a processor that checks if the operation parameter is a constant value.
		// If it is not, use the default instruction processor.
		InstructionProcessor<AbstractInsnNode> defaultProcessor = vmi.getProcessor(op);
		vmi.setProcessor(op, (InstructionProcessor<InsnNode>) (insn, ctx) -> {
			// Skip if not whitelisted.
			if (!whitelist.test(ctx))
				return defaultProcessor.execute(insn, ctx);
			// Pull values from stack
			Stack stack = ctx.getStack();
			Value v = stack.getAt(stack.position() - (wideIn ? 2 : 1));
			// Check for constant values
			if (v instanceof ConstNumericValue) {
				// Take both parameters, compute the operation value, and push onto the stack.
				TrackedValue operationValue = compute.apply(v);
				if (wideIn) stack.popWide();
				else stack.pop();
				// Pushed value size matches 1st parameter
				if (wideOut) {
					stack.pushWide(operationValue);
				} else {
					stack.push(operationValue);
				}
				// Record the contributing instructions from the two values used in this operation.
				operationValue.addContributing((ConstNumericValue) v);
				// Replace the instructions that contribute to the arguments with NOP.
				InsnList instructions = ctx.getMethod().getNode().instructions;
				List<AbstractInsnNode> contributingInstructions = operationValue.getContributingInstructions();
				for (AbstractInsnNode contributingInsn : contributingInstructions)
					if (instructions.contains(contributingInsn))
						InstructionUtil.nop(instructions, contributingInsn);
				// Not all operations should be replaced, like I2B
				if (replace) {
					// Replace the operation with the constant value
					AbstractInsnNode operationValuePushInsn = VmValueUtil.createConstInsn(operationValue);
					instructions.set(insn, operationValuePushInsn);
					// Record the updated instruction (so if this value needs to be folded later it can be)
					operationValue.addContributing(operationValuePushInsn);
				}
				return Result.CONTINUE;
			}
			// Non-constant values, use default processor
			return defaultProcessor.execute(insn, ctx);
		});
	}

	/**
	 * @param vmi
	 * 		Virtual machine interface to register processors on.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 * @param op
	 * 		Operation to register for.
	 * @param v1wide
	 *        {@code true} if the first operation parameter is a wide type.
	 * @param v2wide
	 *        {@code true} if the second operation parameter is a wide type.
	 * @param compute
	 * 		Value computation function.
	 *
	 * @see #install(VirtualMachine, Predicate)
	 */
	private static void handleBiOperationFolding(VMInterface vmi, Predicate<ExecutionContext> whitelist,
												 int op, boolean v1wide, boolean v2wide,
												 BiFunction<Value, Value, TrackedValue> compute) {
		// Register a processor that checks if the operation parameters are constant values.
		// If they are not, use the default instruction processor.
		InstructionProcessor<AbstractInsnNode> defaultProcessor = vmi.getProcessor(op);
		vmi.setProcessor(op, (InstructionProcessor<InsnNode>) (insn, ctx) -> {
			// Skip if not whitelisted.
			if (!whitelist.test(ctx))
				return defaultProcessor.execute(insn, ctx);
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
						InstructionUtil.nop(instructions, contributingInsn);
				// Replace the operation with the constant value
				instructions.set(insn, operationValuePushInsn);
				// Record the updated instruction (so if this value needs to be folded later it can be)
				operationValue.addContributing(operationValuePushInsn);
				return Result.CONTINUE;
			}
			// Non-constant values, use default processor
			return defaultProcessor.execute(insn, ctx);
		});
	}
}
