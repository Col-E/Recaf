package me.coley.recaf.ssvm.processing;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.*;
import dev.xdark.ssvm.thread.SimpleThreadStorage;
import dev.xdark.ssvm.thread.ThreadStorage;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * Utility to install processors that ensure all code branches are visited to a {@link VirtualMachine}.
 *
 * @author Matt Coley
 */
public class FlowRevisitingProcessors implements Opcodes {
	private static final Logger logger = Logging.get(FlowRevisitingProcessors.class);

	/**
	 * Installs processors that ensures all code branches are visited.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 */
	public static void installBranchingProcessor(VirtualMachine vm, Predicate<ExecutionContext> whitelist) {
		VMInterface vmi = vm.getInterface();
		Map<ExecutionContext, Value> initialReturnValues = new HashMap<>();
		ListMultimap<ExecutionContext, AbstractInsnNode> visited =
				MultimapBuilder.ListMultimapBuilder.hashKeys().arrayListValues().build();
		ListMultimap<ExecutionContext, FlowPoint> flowPoints =
				MultimapBuilder.ListMultimapBuilder.hashKeys().arrayListValues().build();
		// TODO: When a visited instruction is seen again, abort restore next flow-point condition
		//       instead of just continuing until a return value is met again.
		vmi.registerInstructionInterceptor((ctx, insn) -> {
			// Skip if not whitelisted
			if (!whitelist.test(ctx))
				return;
			logger.trace("VISIT: " + ctx.getInsnPosition() + ": " + OpcodeUtil.opcodeToName(insn.getOpcode()));
			// Record visited instruction
			List<AbstractInsnNode> visitedInstructions = visited.get(ctx);
			if (visitedInstructions.contains(insn))
				return;
			visitedInstructions.add(insn);
			// Record flow points
			int type = insn.getType();
			if (type == JUMP_INSN || type == TABLESWITCH_INSN || type == LOOKUPSWITCH_INSN) {
				int op = insn.getOpcode();
				if (op != GOTO) {
					logger.debug("Discovered flow point: {}.{}{}@{} - {}",
							ctx.getOwner().getInternalName(),
							ctx.getMethod().getName(),
							ctx.getMethod().getDesc(),
							ctx.getInsnPosition(),
							OpcodeUtil.opcodeToName(insn.getOpcode())
					);
					flowPoints.get(ctx).add(new FlowPoint(ctx, insn));
				}
			}
		});
		// Intercept return instructions so we can revisit flow-points
		for (int ret = IRETURN; ret <= RETURN; ret++) {
			boolean isVoid = ret == RETURN;
			boolean wide = ret == LRETURN || ret == DRETURN;
			InstructionProcessor<AbstractInsnNode> returnProcessor = vmi.getProcessor(ret);
			vmi.setProcessor(ret, (insn, ctx) -> {
				// Record initial return value so that even after all branches are visited,
				// we yield the initial result to the VM.
				if (!initialReturnValues.containsKey(ctx)) {
					Value retVal;
					if (isVoid)
						retVal = VoidValue.INSTANCE;
					else if (wide)
						retVal = ctx.getStack().getAt(1); // TODO: Validate this is correct (skip over TopValue on top)
					else
						retVal = ctx.getStack().peek();
					initialReturnValues.put(ctx, retVal);
				}
				// Pass through to base return processor (user may define some elsewhere that need to be called)
				Result parentProcessorResult = returnProcessor.execute(insn, ctx);
				if (!whitelist.test(ctx) || !flowPoints.containsKey(ctx))
					return parentProcessorResult;
				// Get remaining flow points.
				List<FlowPoint> points = flowPoints.get(ctx);
				// We want to tell any SSVM listeners that the method "exited" because it technically has.
				// But we will then "un-exit" it by restoring from a remaining flow-point.
				if (!points.isEmpty())
					vmi.getInvocationHooks(ctx.getMethod(), false).forEach(invocation -> invocation.handle(ctx));
				// Instead of completing execution, restore from a flow-point if any remain.
				while (!points.isEmpty()) {
					FlowPoint point = points.get(0);
					boolean applied = point.restoreAndVisitNext(ctx);
					if (applied)
						return Result.CONTINUE;
					// The point has no paths remaining, remove it
					flowPoints.remove(ctx, point);
				}
				// No points remain, this ABORT will trigger the natural method exit listener logic.
				ctx.setResult(initialReturnValues.get(ctx));
				return Result.ABORT;
			});
		}
	}

	/**
	 * Wrapper for a {@link org.objectweb.asm.tree.JumpInsnNode}, {@link org.objectweb.asm.tree.TableSwitchInsnNode},
	 * or {@link org.objectweb.asm.tree.LookupSwitchInsnNode} and the state <i>(local variables / stack)</i>
	 * of the VM at that point.
	 * <br>
	 * When we encounter a {@code return} instruction we call {@link #restoreAndVisitNext(ExecutionContext)}.
	 * This will restore the stack and local variables to the state when they initially hit the instruction.
	 * Then the stack is modified in order to satisfy the flow control instruction such that a previously unvisted
	 * path will be taken instead.
	 *
	 * @author Matt Coley
	 */
	private static class FlowPoint {
		private final List<Consumer<ExecutionContext>> flowPathRequirements = new ArrayList<>();
		private final int flowInsnIndex;
		private final Locals localsSnapshot;
		private final Stack stackSnapshot;

		/**
		 * Called when the {@link ExecutionContext#getInsnPosition()} is at the given flow instruction.
		 *
		 * @param ctx
		 * 		Context to pull from.
		 * @param flowInsn
		 * 		Instruction that modified the flow of the method.
		 */
		private FlowPoint(ExecutionContext ctx, AbstractInsnNode flowInsn) {
			// Allocate snapshots of locals and stack
			MethodNode node = ctx.getMethod().getNode();
			flowInsnIndex = node.instructions.indexOf(flowInsn);
			// We use a local storage per flow-point so that we don't conflict with the thread local one.
			ThreadStorage storage = SimpleThreadStorage.create();
			localsSnapshot = storage.newLocals(node.maxLocals);
			stackSnapshot = storage.newStack(node.maxStack);
			// Get state from context
			Locals locals = ctx.getLocals();
			Stack stack = ctx.getStack();
			// Copy local variable table
			Value[] table = locals.getTable();
			for (int i = 0; i < node.maxLocals; i++) {
				localsSnapshot.set(i, table[i]);
			}
			// Copy stack to snapshot via pushes
			for (int i = 0; i < stack.position(); i++) {
				Value value = stack.getAt(i);
				if (value == TopValue.INSTANCE)
					continue;
				stackSnapshot.pushGeneric(value);
			}
			// Record conditions for possible flow paths that are not already taken.
			// Consider IFEQ. If the value on the stack at the recording time is ZERO then
			// the branch condition is met. So we register when the condition result is met
			// the stack should instead be manipulated to not meet the condition.
			// Then when we restore the stack/locals/pc the path not taken originally's
			// requirements will be met and it will be visited.
			// This is repeated until no paths remain.
			switch (flowInsn.getOpcode()) {
				case TABLESWITCH:
					// TODO: All value possibilities
					break;
				case LOOKUPSWITCH:
					// TODO: All value possibilities
					break;
				case IFEQ:
					registerUnaryNumeric(v -> v.asInt() == 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(0));
						}
					});
					break;
				case IFNE:
					registerUnaryNumeric(v -> v.asInt() != 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(0));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFLT:
					registerUnaryNumeric(v -> v.asInt() < 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(-1));
						}
					});
					break;
				case IFGE:
					registerUnaryNumeric(v -> v.asInt() >= 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(-1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFGT:
					registerUnaryNumeric(v -> v.asInt() > 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(-1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFLE:
					registerUnaryNumeric(v -> v.asInt() <= 0, result -> {
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(-1));
						}
					});
					break;
				case IF_ICMPEQ:
					registerBinaryNumeric((v1, v2) -> v1.asInt() == v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IF_ICMPNE:
					registerBinaryNumeric((v1, v2) -> v1.asInt() != v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						}
					});
					break;
				case IF_ICMPLT:
					registerBinaryNumeric((v1, v2) -> v1.asInt() < v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(0));
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						}
					});
					break;
				case IF_ICMPGE:
					registerBinaryNumeric((v1, v2) -> v1.asInt() >= v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						} else {
							stack.push(ConstNumericValue.ofInt(0));
							stack.push(ConstNumericValue.ofInt(1));

						}
					});
					break;
				case IF_ICMPGT:
					registerBinaryNumeric((v1, v2) -> v1.asInt() > v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						} else {
							stack.push(ConstNumericValue.ofInt(0));
							stack.push(ConstNumericValue.ofInt(1));

						}
					});
					break;
				case IF_ICMPLE:
					registerBinaryNumeric((v1, v2) -> v1.asInt() <= v2.asInt(), result -> {
						stack.popGeneric();
						stack.popGeneric();
						if (result) {
							stack.push(ConstNumericValue.ofInt(0));
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
							stack.push(ConstNumericValue.ofInt(0));
						}
					});
					break;
				case IF_ACMPEQ:
					registerBinaryInstance((v1, v2) -> v1 == v2, result -> {
						stack.popGeneric();
						stack.popGeneric();
						Value v = ctx.getHelper().newUtf8("dummy");
						if (result) {
							stack.push(v);
							stack.push(NullValue.INSTANCE);
						} else {
							stack.push(v);
							stack.push(v);
						}
					});
					break;
				case IF_ACMPNE:
					registerBinaryInstance((v1, v2) -> v1 != v2, result -> {
						stack.popGeneric();
						stack.popGeneric();
						Value v = ctx.getHelper().newUtf8("dummy");
						if (result) {
							stack.push(v);
							stack.push(v);
						} else {
							stack.push(v);
							stack.push(NullValue.INSTANCE);
						}
					});
					break;
				case IFNONNULL:
					registerUnaryInstance(v -> !v.isNull(), result -> {
						stack.pop();
						if (result) {
							stack.push(NullValue.INSTANCE);
						} else {
							stack.push(ctx.getHelper().newUtf8("dummy"));
						}
					});
					break;
				case IFNULL:
					registerUnaryInstance(Value::isNull, result -> {
						stack.pop();
						if (result) {
							stack.push(ctx.getHelper().newUtf8("dummy"));
						} else {
							stack.push(NullValue.INSTANCE);
						}
					});
					break;
				case GOTO:
					// None
					break;
				case JSR:
				case RET:
					throw new IllegalStateException("JSR/RET unsupported");
			}
		}

		/**
		 * Restores the stack and local variables of the method execution context.
		 * The next flow path is taken, which we modify the stack after restoration in
		 * order to force the vm to take that path.
		 *
		 * @param ctx
		 * 		Execution context of a method to restore the state of.
		 *
		 * @return {@code true} if there was a path visited.
		 * {@code false} if no paths remain.
		 */
		public boolean restoreAndVisitNext(ExecutionContext ctx) {
			// No conditions left, do nothing
			if (flowPathRequirements.isEmpty()) {
				logger.debug("No remaining flow paths");
				return false;
			}
			// Restore state
			MethodNode node = ctx.getMethod().getNode();
			Locals locals = ctx.getLocals();
			Stack stack = ctx.getStack();
			Value[] table = localsSnapshot.getTable();
			for (int i = 0; i < node.maxLocals; i++) {
				locals.set(i, table[i]);
			}
			// Clear context's stack
			while (!stack.isEmpty())
				stack.popGeneric();
			// Copy stack snapshot into context's stack via pushes
			for (int i = 0; i < stackSnapshot.position(); i++) {
				Value value = stackSnapshot.getAt(i);
				if (value == TopValue.INSTANCE)
					continue;
				stack.pushGeneric(value);
			}
			// Move instruction position to index of flow instruction
			ctx.setInsnPosition(flowInsnIndex);
			// Apply requirement to visit path
			flowPathRequirements.remove(0).accept(ctx);
			logger.debug("Restored with alternative flow path, {} paths remaining in this point[{}]",
					flowPathRequirements.size(), flowInsnIndex);
			return true;
		}

		private void registerUnaryNumeric(Predicate<Value> condition, Consumer<Boolean> action) {
			Value top = stackSnapshot.peek();
			if (top instanceof NumericValue || top instanceof ConstNumericValue) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top)));
			}
		}

		private void registerBinaryNumeric(BiPredicate<Value, Value> condition, Consumer<Boolean> action) {
			Value top1 = stackSnapshot.getAt(0);
			Value top2 = stackSnapshot.getAt(1);
			if ((top1 instanceof NumericValue || top1 instanceof ConstNumericValue) &&
					(top2 instanceof NumericValue || top2 instanceof ConstNumericValue)) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top1, top2)));
			}
		}

		private void registerUnaryInstance(Predicate<Value> condition, Consumer<Boolean> action) {
			Value top = stackSnapshot.peek();
			if (top instanceof InstanceValue) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top)));
			}
		}

		private void registerBinaryInstance(BiPredicate<Value, Value> condition, Consumer<Boolean> action) {
			Value top1 = stackSnapshot.getAt(0);
			Value top2 = stackSnapshot.getAt(1);
			if (top1 instanceof InstanceValue && top2 instanceof InstanceValue) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top1, top2)));
			}
		}
	}
}