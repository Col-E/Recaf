package me.coley.recaf.ssvm.processing;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.ConstStringValue;
import me.coley.recaf.ssvm.value.ValueOperations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Utility to install peephole optimization to a {@link VirtualMachine}.
 * For example, consider the following snippet:
 * <pre>
 *     int i = 10 * ((8 + 8) + (8 / 2) >> 1);
 * </pre>
 * Will be simplified into:
 * <pre>
 *     int i = 100;
 * </pre>
 *
 * @author Matt Coley
 * @author xDark
 */
public class PeepholeProcessors implements Opcodes {
	/**
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	public static void install(VirtualMachine vm) {
		installValuePushing(vm);
		installValueFolding(vm);
		// TODO: Install for method calls where all parameters are constants, and return value is also a constant
		//   - should address most basic string obfuscation
	}

	/**
	 * Install processors for instructions that push constant values onto the stack.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	private static void installValuePushing(VirtualMachine vm) {
		// TODO: Register for non-ints
		VMInterface vmi = vm.getInterface();
		VMHelper helper = vm.getHelper();
		for (int i : new int[]{BIPUSH, SIPUSH}) {
			vmi.setProcessor(i, (InstructionProcessor<IntInsnNode>) (insn, ctx) -> {
				// We take the value pushed by the instruction 'SIPUSH/BIPUSH <value>' and map it to a custom value class.
				// This 'ConstantValue' class is checked later for mathematical simplification/folding.
				ConstNumericValue value = ConstNumericValue.ofInt(insn.operand);
				ctx.getStack().push(value);
				return Result.CONTINUE;
			});
		}
		for (int i = -1; i < 5; i++) {
			int k = i;
			vmi.setProcessor(ICONST_0 + i, (InstructionProcessor<InsnNode>) (insn, ctx) -> {
				ConstNumericValue value = ConstNumericValue.ofInt(k);
				ctx.getStack().push(value);
				return Result.CONTINUE;
			});
		}
		InstructionProcessor<AbstractInsnNode> ldc = vmi.getProcessor(LDC);
		vmi.setProcessor(LDC, (InstructionProcessor<LdcInsnNode>) (insn, ctx) -> {
			Object cst = insn.cst;
			Value value;
			if (cst instanceof Integer) {
				value = ConstNumericValue.ofInt((Integer) cst);
			} else if (cst instanceof String) {
				value = ConstStringValue.ofString(helper, (String) cst);
			} else {
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
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	private static void installValueFolding(VirtualMachine vm) {
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


}
