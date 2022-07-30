package me.coley.recaf.ssvm.processing;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.asm.DelegatingInsnNode;
import dev.xdark.ssvm.asm.VMOpcodes;
import dev.xdark.ssvm.asm.VMTypeInsnNode;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.InstructionProcessor;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.value.*;
import me.coley.recaf.util.InstructionUtil;
import me.coley.recaf.util.Multimap;
import me.coley.recaf.util.MultimapBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Utility to install tracking logic into a {@link VirtualMachine}.
 * Allows other processors to have better insights into runtime values.
 *
 * @author Matt Coley
 * @author xDark
 */
public class DataTracking implements Opcodes {
	/**
	 * Install all processors for data tracking purposes.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	public static void install(VirtualMachine vm) {
		installValuePushing(vm);
		installStackManipulationInstructionTracking(vm);
		installArrays(vm);
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
		// LDC can hold a variety of types, some of which we support
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
				ctx.getStack().pushGeneric(helper.valueFromLdc(insn.cst));
				return Result.CONTINUE;
			}
			return push(ctx, insn, value);
		});
		// We'll want to track string values
		InstanceJavaClass jcString = vm.getSymbols().java_lang_String();
		InstructionProcessor<VMTypeInsnNode> newProcessor = vmi.getProcessor(VMOpcodes.VM_NEW);
		vmi.setProcessor(VMOpcodes.VM_NEW, (VMTypeInsnNode insn, ExecutionContext ctx) -> {
			Result result = newProcessor.execute(insn, ctx);
			Stack stack = ctx.getStack();
			InstanceValue value = stack.peek();
			if (jcString == value.getJavaClass()) {
				// If we allocate a string,
				// pop it off the stack & replace with our tracked value.
				stack.pop();
				TrackedInstanceValue replacement = new TrackedInstanceValue(value);
				replacement.addContributing(insn);
				stack.push(replacement);
			}
			return result;
		});
	}

	/**
	 * Install processors for array instructions.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 */
	public static void installArrays(VirtualMachine vm) {
		VMInterface vmi = vm.getInterface();
		for (int opcode = VMOpcodes.VM_BOOLEAN_NEW_ARRAY; opcode <= VMOpcodes.VM_LONG_NEW_ARRAY; opcode++) {
			InstructionProcessor<DelegatingInsnNode<IntInsnNode>> delegate = vmi.getProcessor(opcode);
			vmi.setProcessor(opcode, (DelegatingInsnNode<IntInsnNode> insn, ExecutionContext ctx) -> {
				Stack stack = ctx.getStack();
				Value lengthValue = stack.peek();
				Result result = delegate.execute(insn, ctx);
				ArrayValue value = stack.pop();
				TrackedArrayValue tracked = new TrackedArrayValue(value);
				if (lengthValue instanceof TrackedValue) {
					tracked.addContributing((TrackedValue) lengthValue);
					tracked.setConstantLength(true);
				}
				tracked.addContributing(insn);
				stack.push(tracked);
				return result;
			});
		}
		vmi.setProcessor(IASTORE, arraySet((array, index, value) -> array.setInt(index, value.asInt())));
		vmi.setProcessor(LASTORE, arraySet((array, index, value) -> array.setLong(index, value.asLong())));
		vmi.setProcessor(FASTORE, arraySet((array, index, value) -> array.setFloat(index, value.asFloat())));
		vmi.setProcessor(DASTORE, arraySet((array, index, value) -> array.setDouble(index, value.asDouble())));
		vmi.setProcessor(AASTORE, arraySet((array, index, value) -> array.setValue(index, (ObjectValue) value)));
		vmi.setProcessor(BASTORE, arraySet((array, index, value) -> array.setByte(index, value.asByte())));
		vmi.setProcessor(CASTORE, arraySet((array, index, value) -> array.setChar(index, value.asChar())));
		vmi.setProcessor(SASTORE, arraySet((array, index, value) -> array.setShort(index, value.asShort())));
		vmi.setProcessor(IALOAD, arrayLoad((array, index) -> IntValue.of(array.getInt(index))));
		vmi.setProcessor(LALOAD, arrayLoad((array, index) -> LongValue.of(array.getLong(index))));
		vmi.setProcessor(FALOAD, arrayLoad((array, index) -> new FloatValue(array.getFloat(index))));
		vmi.setProcessor(DALOAD, arrayLoad((array, index) -> new DoubleValue(array.getDouble(index))));
		vmi.setProcessor(AALOAD, arrayLoad(ArrayValue::getValue));
		vmi.setProcessor(BALOAD, arrayLoad((array, index) -> IntValue.of(array.getByte(index))));
		vmi.setProcessor(CALOAD, arrayLoad((array, index) -> IntValue.of(array.getChar(index))));
		vmi.setProcessor(SALOAD, arrayLoad((array, index) -> IntValue.of(array.getShort(index))));
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
	public static void installStackManipulationInstructionTracking(VirtualMachine vm) {
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
		var valuesWithPops = MultimapBuilder
				.<ExecutionContext, TrackedValue>hashKeys()
				.arrayValues()
				.build();
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
			for (TrackedValue value : valuesWithPops.remove(ctx)) {
				if (value.getContributingInstructions().stream().noneMatch(instructions::contains)) {
					value.getAssociatedPops().forEach(insn -> InstructionUtil.nop(instructions, insn));
				}
			}
		});
	}

	/**
	 * Creates instruction processor for array setter.
	 *
	 * @param setter
	 * 		Array setter.
	 *
	 * @return New processor.
	 */
	private static InstructionProcessor<AbstractInsnNode> arraySet(ArraySetter setter) {
		return (insn, ctx) -> {
			VMHelper helper = ctx.getHelper();
			Stack stack = ctx.getStack();
			Value value = stack.pop();
			Value indexValue = stack.pop();
			int index = indexValue.asInt();
			Value top = stack.pop();
			ArrayValue array = helper.checkNotNull(top);
			helper.rangeCheck(array, index);
			setter.set(array, index, value);
			if (array instanceof TrackedArrayValue) {
				TrackedArrayValue tracked = (TrackedArrayValue) array;
				tracked.addContributing(insn);
				if (indexValue instanceof TrackedValue) {
					tracked.addContributing((TrackedValue) indexValue);
				}
				tracked.trackValue(index, value);
			}
			return Result.CONTINUE;
		};
	}

	/**
	 * Creates instruction processor for array getter.
	 *
	 * @param loader
	 * 		Array getter.
	 *
	 * @return New processor.
	 */
	private static InstructionProcessor<AbstractInsnNode> arrayLoad(ArrayLoader loader) {
		return (insn, ctx) -> {
			VMHelper helper = ctx.getHelper();
			Stack stack = ctx.getStack();
			int index = stack.pop().asInt();
			ArrayValue array = helper.checkNotNullArray(stack.pop());
			helper.rangeCheck(array, index);
			Value value = null;
			if (array instanceof TrackedArrayValue) {
				value = ((TrackedArrayValue) array).getTrackedValue(index);
			}
			if (value == null) {
				value = loader.load(array, index);
			}
			if (value instanceof TrackedValue) {
				((TrackedValue) value).addContributing(insn);
			}
			stack.pushGeneric(value);
			return Result.CONTINUE;
		};
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
	private static void trackPop(Multimap<ExecutionContext, TrackedValue, List<TrackedValue>> map,
								 AbstractInsnNode insn, ExecutionContext ctx, Value value) {
		if (value instanceof TrackedValue) {
			((TrackedValue) value).addAssociatedPop(insn);
			map.put(ctx, (TrackedValue) value);
		}
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

	private interface ArraySetter {
		void set(ArrayValue array, int index, Value value);
	}

	private interface ArrayLoader {
		Value load(ArrayValue array, int index);
	}
}
