package me.coley.recaf.ssvm.processing;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.Stack;
import dev.xdark.ssvm.execution.*;
import dev.xdark.ssvm.thread.SimpleThreadStorage;
import dev.xdark.ssvm.thread.ThreadStorage;
import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.util.VmValueUtil;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.util.Multimap;
import me.coley.recaf.util.MultimapBuilder;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * Utility to install processors that ensure all code branches are visited to a {@link VirtualMachine}.
 *
 * @author Matt Coley
 */
public class FlowRevisiting implements Opcodes {
	private static final Logger logger = Logging.get(FlowRevisiting.class);

	/**
	 * Installs processors that ensures all code branches are visited.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 */
	public static void install(VirtualMachine vm, Predicate<ExecutionContext> whitelist) {
		VMInterface vmi = vm.getInterface();
		Map<ExecutionContext, Value> initialReturnValueMap = new IdentityHashMap<>();
		Map<ExecutionContext, InstructionStateCache> stateCacheMap = new IdentityHashMap<>();
		var flowPoints = MultimapBuilder
				.<ExecutionContext, FlowPoint>hashKeys()
				.arrayValues()
				.build();
		vmi.registerInstructionInterceptor((ctx, insn) -> {
			// Skip if not whitelisted
			if (!whitelist.test(ctx))
				return Result.CONTINUE;
			logger.debug("VISIT: " + ctx.getInsnPosition() + ": " + OpcodeUtil.opcodeToName(insn.getOpcode()));
			// Skip frames/labels
			if (insn.getOpcode() == Opcodes.F_NEW)
				return Result.CONTINUE;
			// Get instruction type
			int type = insn.getType();
			// Check if we can abort execution (assuming we are revisiting some code)
			InstructionStateCache instructionStateCache = stateCacheMap.computeIfAbsent(ctx, x -> new InstructionStateCache());
			boolean seen = instructionStateCache.has(insn);
			if (seen) {
				// Abort execution of this branch when we've seen this instruction already and the
				// current state of the VM matches the old recorded state of the VM.
				// Of course, we need to know what value to return, so if we don't have one we won't abort.
				if (instructionStateCache.isSameState(ctx, insn) && initialReturnValueMap.containsKey(ctx)) {
					Value value = initialReturnValueMap.get(ctx);
					// We decrement the offset since the VM will increment it once we exit this interception callback.
					logger.debug("Encountered previous seen instruction/state, aborting re-branch, yielding original value {}", value);
					ctx.setResult(value);
					return Result.ABORT;
				}
			}
			// Record state at current instruction
			instructionStateCache.add(ctx, insn);
			// Record flow points
			if (!seen && (type == JUMP_INSN || type == TABLESWITCH_INSN || type == LOOKUPSWITCH_INSN)) {
				int op = insn.getOpcode();
				if (op != GOTO) {
					logger.debug("Discovered flow point: {}.{}{}@{} - {}",
							ctx.getOwner().getInternalName(),
							ctx.getMethod().getName(),
							ctx.getMethod().getDesc(),
							ctx.getInsnPosition(),
							OpcodeUtil.opcodeToName(insn.getOpcode())
					);
					flowPoints.put(ctx, new FlowPoint(ctx, insn));
				}
			}
			return Result.CONTINUE;
		});
		// Intercept return instructions so we can revisit flow-points
		for (int ret = IRETURN; ret <= RETURN; ret++) {
			InstructionProcessor<AbstractInsnNode> returnProcessor = vmi.getProcessor(ret);
			vmi.setProcessor(ret, (insn, ctx) -> {
				// Pass through to base return processor (user may define some elsewhere that need to be called)
				Result parentProcessorResult = returnProcessor.execute(insn, ctx);
				if (!whitelist.test(ctx) || !flowPoints.containsKey(ctx))
					return parentProcessorResult;
				// Record initial return value so that even after all branches are visited,
				// we yield the initial result to the VM.
				if (!initialReturnValueMap.containsKey(ctx)) {
					Value retVal = ctx.getResult();
					initialReturnValueMap.put(ctx, retVal);
				}
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
				ctx.setResult(initialReturnValueMap.get(ctx));
				return Result.ABORT;
			});
		}
	}

	/**
	 * A map of {@link AbstractInsnNode} to their last known states <i>(Recorded as {@link Snapshot})</i>.
	 *
	 * @author Matt Coley
	 */
	private static class InstructionStateCache {
		private final Map<AbstractInsnNode, Snapshot> map = new HashMap<>();

		/**
		 * Adds a record of the VM state at the given instruction.
		 *
		 * @param ctx
		 * 		Context to create snapshot of.
		 * @param insn
		 * 		Instruction to track.
		 */
		public void add(ExecutionContext ctx, AbstractInsnNode insn) {
			map.put(insn, new Snapshot(ctx));

		}

		/**
		 * @param insn
		 * 		Instruction to check.
		 *
		 * @return {@code true} if we've tracked that instruction already.
		 */
		public boolean has(AbstractInsnNode insn) {
			return map.containsKey(insn);
		}

		/**
		 * @param ctx
		 * 		Context to check with.
		 * @param insn
		 * 		Instruction / offset.
		 *
		 * @return {@code true} if the prior held record of the VM state at that instruction matches the
		 * current state of the VM.
		 *
		 * @see Snapshot#isSameState(ExecutionContext)
		 */
		public boolean isSameState(ExecutionContext ctx, AbstractInsnNode insn) {
			Snapshot c = map.get(insn);
			if (c == null)
				return false;
			return c.isSameState(ctx);
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
	private static class FlowPoint extends Snapshot {
		private final List<Consumer<ExecutionContext>> flowPathRequirements = new ArrayList<>();
		private final int flowInsnIndex;

		/**
		 * Called when the {@link ExecutionContext#getInsnPosition()} is at the given flow instruction.
		 *
		 * @param ctx
		 * 		Context to pull from.
		 * @param flowInsn
		 * 		Instruction that modified the flow of the method.
		 */
		private FlowPoint(ExecutionContext ctx, AbstractInsnNode flowInsn) {
			super(ctx);
			this.flowInsnIndex = ctx.getInsnPosition();
			// Get state from context
			Stack stack = ctx.getStack();
			// Record conditions for possible flow paths that are not already taken.
			// Consider IFEQ. If the value on the stack at the recording time is ZERO then
			// the branch condition is met. So we register when the condition result is met
			// the stack should instead be manipulated to not meet the condition.
			// Then when we restore the stack/locals/pc the path not taken originally's
			// requirements will be met and it will be visited.
			// This is repeated until no paths remain.
			switch (flowInsn.getOpcode()) {
				case TABLESWITCH: {
					Value top = stackSnapshot.peek();
					if (top instanceof NumericValue || top instanceof ConstNumericValue) {
						int key = top.asInt();
						TableSwitchInsnNode tableSwitch = (TableSwitchInsnNode) flowInsn;
						for (int i = tableSwitch.min; i < tableSwitch.max; i++) {
							if (key != i) {
								int switchCase = i;
								flowPathRequirements.add(c -> {
									stack.pop();
									stack.push(ConstNumericValue.ofInt(switchCase));
								});
							}
						}
						flowPathRequirements.add(c -> {
							stack.pop();
							stack.push(ConstNumericValue.ofInt(tableSwitch.max + 1));
						});
					}
					break;
				}
				case LOOKUPSWITCH: {
					Value top = stackSnapshot.peek();
					if (top instanceof NumericValue || top instanceof ConstNumericValue) {
						int defaultKey = -1;
						int key = top.asInt();
						LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) flowInsn;
						for (int otherKey : lookup.keys) {
							defaultKey = Math.max(defaultKey, otherKey) + 1;
							if (key != otherKey) {
								flowPathRequirements.add(c -> {
									stack.pop();
									stack.push(ConstNumericValue.ofInt(otherKey));
								});
							}
						}
						int defaultCopy = defaultKey;
						flowPathRequirements.add(c -> {
							stack.pop();
							stack.push(ConstNumericValue.ofInt(defaultCopy));
						});
					}
				}
				case IFEQ:
					registerUnaryNumeric(v -> v.asInt() == 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(0));
						}
					});
					break;
				case IFNE:
					registerUnaryNumeric(v -> v.asInt() != 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(0));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFLT:
					registerUnaryNumeric(v -> v.asInt() < 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(-1));
						}
					});
					break;
				case IFGE:
					registerUnaryNumeric(v -> v.asInt() >= 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(-1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFGT:
					registerUnaryNumeric(v -> v.asInt() > 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(-1));
						} else {
							stack.push(ConstNumericValue.ofInt(1));
						}
					});
					break;
				case IFLE:
					registerUnaryNumeric(v -> v.asInt() <= 0, result -> {
						stack.pop();
						if (result) {
							stack.push(ConstNumericValue.ofInt(1));
						} else {
							stack.push(ConstNumericValue.ofInt(-1));
						}
					});
					break;
				case IF_ICMPEQ:
					registerBinaryNumeric((v1, v2) -> v1.asInt() == v2.asInt(), result -> {
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
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
						stack.pop();
						stack.pop();
						Value v = ctx.getHelper().newUtf8("dummy");
						if (result) {
							stack.push(v);
							stack.push(ctx.getMemoryManager().nullValue());
						} else {
							stack.push(v);
							stack.push(v);
						}
					});
					break;
				case IF_ACMPNE:
					registerBinaryInstance((v1, v2) -> v1 != v2, result -> {
						stack.pop();
						stack.pop();
						Value v = ctx.getHelper().newUtf8("dummy");
						if (result) {
							stack.push(v);
							stack.push(v);
						} else {
							stack.push(v);
							stack.push(ctx.getMemoryManager().nullValue());
						}
					});
					break;
				case IFNONNULL:
					registerUnaryInstance(v -> !v.isNull(), result -> {
						stack.pop();
						if (result) {
							stack.push(ctx.getMemoryManager().nullValue());
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
							stack.push(ctx.getMemoryManager().nullValue());
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
				// Uninitialized values are null, this is intentional
				if (table[i] == null)
					continue;
				locals.set(i, table[i]);
			}
			// Clear context's stack
			stack.clear();
			// Copy stack snapshot into context's stack via pushes
			for (int i = 0; i < stackSnapshot.position(); i++) {
				Value value = stackSnapshot.getAt(i);
				if (value == TopValue.INSTANCE)
					continue;
				stack.pushGeneric(value);
			}
			// Move instruction position to index of flow instruction.
			// Because this is run inside the RETURN processor, we decrement the offset.
			// When the VM finishes the processor, it gets incremented again to the correct
			// index of the flow instruction.
			ctx.setInsnPosition(flowInsnIndex - 1);
			// Apply requirement to visit path
			flowPathRequirements.remove(0).accept(ctx);
			logger.debug("Restored with alternative flow path, {} paths remaining in this point[{}]",
					flowPathRequirements.size(), flowInsnIndex);
			return true;
		}

		private void registerUnaryNumeric(Predicate<Value> condition, Consumer<Boolean> action) {
			Value top = stackSnapshot.peek();
			if (top instanceof NumericValue || top instanceof ConstNumericValue) {
				if (areAllArgsConstant(top)) {
					// If the arguments for the flow operation are all constants, we will NEVER have a situation
					// where the alternative flow path is taken. So we won't bother registering it.
					logger.info("Skipping registering alternative flow paths since all inputs are constants");
					return;
				}
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top)));
			}
		}

		private void registerBinaryNumeric(BiPredicate<Value, Value> condition, Consumer<Boolean> action) {
			Value top1 = stackSnapshot.getAt(stackSnapshot.position() - 1);
			Value top2 = stackSnapshot.getAt(stackSnapshot.position() - 2);
			if ((top1 instanceof NumericValue || top1 instanceof ConstNumericValue) &&
					(top2 instanceof NumericValue || top2 instanceof ConstNumericValue)) {
				if (areAllArgsConstant(top1, top2)) {
					// If the arguments for the flow operation are all constants, we will NEVER have a situation
					// where the alternative flow path is taken. So we won't bother registering it.
					logger.info("Skipping registering alternative flow paths since all inputs are constants");
					return;
				}
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top1, top2)));
			}
		}

		private void registerUnaryInstance(Predicate<Value> condition, Consumer<Boolean> action) {
			Value top = stackSnapshot.peek();
			if (top instanceof ObjectValue) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top)));
			}
		}

		private void registerBinaryInstance(BiPredicate<Value, Value> condition, Consumer<Boolean> action) {
			Value top1 = stackSnapshot.getAt(stackSnapshot.position() - 1);
			Value top2 = stackSnapshot.getAt(stackSnapshot.position() - 2);
			if (top1 instanceof ObjectValue && top2 instanceof ObjectValue) {
				flowPathRequirements.add((ctx) -> action.accept(condition.test(top1, top2)));
			}
		}

		private static boolean areAllArgsConstant(Value... values) {
			for (Value value : values)
				if (!VmValueUtil.isConstant(value))
					return false;
			return true;
		}
	}

	/**
	 * Common base for any type that needs to snapshot the state of the method.
	 * Records the {@link Stack} and {@link Locals} of a {@link ExecutionContext} at the current execution point.
	 *
	 * @author Matt Coley
	 */
	public static class Snapshot {
		protected final Locals localsSnapshot;
		protected final Stack stackSnapshot;

		/**
		 * Creates a snapshot at the {@link ExecutionContext#getInsnPosition() current point}.
		 *
		 * @param ctx
		 * 		Context to pull from.
		 */
		public Snapshot(ExecutionContext ctx) {
			// Get state from context
			Locals locals = ctx.getLocals();
			Stack stack = ctx.getStack();
			// Allocate snapshots of locals and stack
			MethodNode node = ctx.getMethod().getNode();
			// We use a local storage per snapshot so that we don't conflict with the thread local one.
			ThreadStorage storage = SimpleThreadStorage.create(node.maxLocals + stack.position());
			localsSnapshot = storage.newLocals(node.maxLocals);
			stackSnapshot = storage.newStack(stack.position());
			// Copy local variable table
			Value[] table = locals.getTable();
			for (int i = 0; i < node.maxLocals; i++) {
				// Uninitialized values are null, this is intentional
				if (table[i] == null)
					continue;
				localsSnapshot.set(i, table[i]);
			}
			// Copy stack to snapshot via pushes
			for (int i = 0; i < stack.position(); i++) {
				Value value = stack.getAt(i);
				if (value == TopValue.INSTANCE)
					continue;
				stackSnapshot.pushGeneric(value);
			}
		}

		/**
		 * @param ctx
		 * 		Context to compare to.
		 *
		 * @return {@code true} when the {@link Locals} and {@link Stack} of the given context match what
		 * is stored in the local snapshot.
		 */
		public boolean isSameState(ExecutionContext ctx) {
			// Get state from context
			Locals locals = ctx.getLocals();
			Stack stack = ctx.getStack();
			// Copy local variable table
			return locals.equals(localsSnapshot) && stack.equals(stackSnapshot);
		}
	}
}