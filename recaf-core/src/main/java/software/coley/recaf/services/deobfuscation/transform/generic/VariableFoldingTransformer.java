package software.coley.recaf.services.deobfuscation.transform.generic;

import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static software.coley.recaf.util.AsmInsnUtil.*;

/**
 * A transformer that folds redundant variable use.
 * <br>
 * You should use {@link OpaqueConstantFoldingTransformer} after using this for {@code POP} cleanup.
 *
 * @author Matt Coley
 */
@Dependent
public class VariableFoldingTransformer implements JvmClassTransformer {
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public VariableFoldingTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
	}

	@Override
	@SuppressWarnings({"IfCanBeSwitch", "OptionalGetWithoutIsPresent"}) // switch changes semantics slightly
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			// Skip if abstract.
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;

			// Populate local access state.
			Int2ObjectMap<LocalAccessState> states = new Int2ObjectArrayMap<>();
			Int2BooleanArrayMap canOverwrite = new Int2BooleanArrayMap();

			// Populate local state from parameters.
			boolean isStatic = AccessFlag.isStatic(method.access);
			if (!isStatic) {
				// implicit 'this'
				ObjectValue thisValue = ObjectValue.object(Type.getObjectType(node.name), Nullness.NOT_NULL);
				LocalAccessState thisState = new LocalAccessState(0, thisValue);
				thisState.addWrite(-1, new VarInsnNode(ASTORE, 0));
				states.put(key(0, Type.OBJECT), thisState);
			}
			int paramSlot = isStatic ? 0 : 1;
			for (Type argumentType : Type.getArgumentTypes(method.desc)) {
				// explicit parameters
				ReValue paramValue = Unchecked.get(() -> ReValue.ofType(argumentType, Nullness.UNKNOWN));
				LocalAccessState paramState = new LocalAccessState(paramSlot, paramValue);
				paramState.addWrite(-1, createVarStore(paramSlot, argumentType));
				states.put(key(paramSlot, argumentType.getSort()), paramState);
				paramSlot += argumentType.getSize();
			}

			// Populate the rest of the state from the method code.
			Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
			try {
				// TODO: It would be smarter to track where we see these observations
				//  - Variables can check if the observations occur outside of their scope and thus are irrelevant
				boolean controlFlowObserved = false;
				boolean throwingObserved = false;
				for (int i = 0; i < instructions.size(); i++) {
					AbstractInsnNode insn = instructions.get(i);
					int op = insn.getOpcode();

					// Record variable reads
					if (isVarLoad(op) && insn instanceof VarInsnNode vin) {
						Frame<ReValue> frame = frames[i];
						if (frame == null)
							continue; // Skip dead code
						LocalAccessState state = states.computeIfAbsent(key(vin.var, getTypeForVarInsn(vin).getSort()), k -> new LocalAccessState(vin.var));
						state.addRead(i, vin);
					}

					// Record variable writes
					else if (isVarStore(op) && insn instanceof VarInsnNode vin) {
						Frame<ReValue> frame = frames[i];
						if (frame == null)
							continue; // Skip dead code
						LocalAccessState state = states.computeIfAbsent(key(vin.var, getTypeForVarInsn(vin).getSort()), k -> new LocalAccessState(vin.var));
						state.addWrite(i, vin);

						// If we're moving forward in the method from the beginning, and haven't seen
						// the variable being used yet or any kind of control flow, then we can safely
						// just replace the state with the value on the stack instead of merging it.
						ReValue top = frame.getStack(frame.getStackSize() - 1);
						if (state.getReadsUpTo(i).isEmpty() && !controlFlowObserved && !throwingObserved)
							state.setState(top);
						else
							state.mergeState(top);
					}

					// Record variable mutations (iinc is a fun edge case...)
					else if (op == IINC && insn instanceof IincInsnNode iinc) {
						if (frames[i] == null)
							continue; // Skip dead code
						LocalAccessState state = states.computeIfAbsent(key(iinc.var, Type.INT), k -> new LocalAccessState(iinc.var));

						// Edge case: iinc counts as a read and write at the same time.
						state.addRead(i, iinc);
						state.addWrite(i, iinc);

						// If we're moving forward in the method from the beginning,
						// and haven't seen any kind of control flow, then we can safely
						// just replace the state with the incremented value.
						if (state.mergedValue instanceof IntValue mergedIntValue &&
								state.getReadsUpTo(i).isEmpty() && !controlFlowObserved && !throwingObserved) {
							state.setState(mergedIntValue.add(iinc.incr));
						} else {
							state.mergeState(IntValue.UNKNOWN);
						}
					}

					// Other state tracking which can change how we merge state.
					else if (!controlFlowObserved && isFlowControl(insn))
						// Control flow means somewhere else in the method the variable can be mutated.
						controlFlowObserved = true;
					else if (!throwingObserved) {
						// TODO: Rather than just saying "throwing observed" it may be better to detail
						//  points in the code where exceptions can be thrown. That way there can still
						//  be some inlining in areas not affected by the potential of skipping "try { ... }" contents.
						//  - This works for now though. It won't inline reads that could be inlined in some edge cases
						//    but at the benefit of reducing incorrectly inlined cases due to possible throwing operations
						//    interrupting the control flow of assigning multiple values to a variable.
						TryCatchBlockNode tryBlock = getContainingTryBlock(method, insn);
						if (tryBlock != null) {
							// Instance field interactions can throw NPE.
							if ((op == GETFIELD || op == PUTFIELD)
									&& (tryBlock.type == null
									|| tryBlock.type.equals("java/lang/Throwable")
									|| tryBlock.type.equals("java/lang/Exception")
									|| tryBlock.type.equals("java/lang/NullPointerException")))
								throwingObserved = true;

							// Most external method calls can throw *anything* so without expensive
							// throwing analysis we'll just assume it can happen.
							if (insn instanceof MethodInsnNode)
								throwingObserved = true;
						}
					}
				}
			} catch (IllegalValueException ex) {
				throw new TransformationException("Failed to create merged state for locals", ex);
			}

			// Remove locals that are not being used.
			for (int i = instructions.size() - 1; i >= 0; i--) {
				AbstractInsnNode insn = instructions.get(i);
				int op = insn.getOpcode();
				if (isVarLoad(op) && insn instanceof VarInsnNode vin) {
					// Replace variable loads with constant values if possible.
					Type varType = getTypeForVarInsn(vin);
					LocalAccessState state = states.get(key(vin.var, varType.getSort()));
					if (state != null && state.isEffectiveConstant()) {
						// Get replacement value of known state constant.
						AbstractInsnNode replacement = switch (state.mergedValue) {
							case DoubleValue doubleValue -> doubleToInsn(doubleValue.value().getAsDouble());
							case FloatValue floatValue -> floatToInsn((float) floatValue.value().getAsDouble());
							case IntValue intValue -> intToInsn(intValue.value().getAsInt());
							case LongValue longValue -> longToInsn(longValue.value().getAsLong());
							case StringValue stringValue -> new LdcInsnNode(stringValue.getText().get());
							default -> null;
						};

						// Mark dirty if the instruction was replaced.
						if (replacement != null) {
							instructions.set(vin, replacement);
							dirty = true;
						}
					}
				} else if (isVarStore(op) && insn instanceof VarInsnNode vin) {
					// Remove variable writes if:
					// - They are never read from
					// - They are inlinable values (arrays for instance cannot be inlined as a single value providing instruction)
					// - They are read from, but only used with a single known constant value (which we inline above)
					Type varType = getTypeForVarInsn(vin);
					int varSort = varType.getSort();
					int varKey = key(vin.var, varSort);
					LocalAccessState state = states.get(varKey);
					if (state != null && state.isInlinableValue() && (state.getReads().isEmpty() || state.isEffectiveConstant())) {
						AbstractInsnNode previous = insn.getPrevious();
						if (OpaqueConstantFoldingTransformer.isSupportedValueProducer(previous)) {
							// If the previous instruction is a supported value producer (consumes nothing on the stack, provides a supported value)
							// then we can just nop both this store and the producer.
							instructions.set(previous, new InsnNode(NOP));
							instructions.set(vin, new InsnNode(NOP));
						} else {
							// The previous instruction cannot be safely replaced, so we just replace the store with a pop.
							instructions.set(vin, new InsnNode(varType.getSize() == 1 ? POP : POP2));
						}
						dirty = true;
					}

					// Remove variable writes if:
					// - This write is never read from, even if the variable is read from later after a different assignment.
					//   Between the potential unused write (this one) and the following write preceding the first read
					//   there must not be any control-flow altering instructions.
					//    TODO: Implement this (probably requires better state tracking refactoring)

					// Remove variable writes + usages if:
					// - The variable is a redundant copy of an existing other variable
					// TODO: Enable this after fixing the 'doNotFoldVarUsedInComparisonIfOriginalPossiblyUpdates' test
					if (false) for (int keyX : states.keySet()) {
						// Check if the current store is redundant with another variable.
						int slotX = slotFromKey(keyX);
						if (slotX != vin.var && isRedundantStore(states, instructions, i, slotX, vin.var, varSort)) {
							LocalAccessState stateY = states.get(varKey);

							// Replace usage of the redundant variable.
							if (!stateY.getReads().isEmpty()) {
								replaceRedundantVariableUsage(instructions, slotX, vin.var, varSort);
								stateY.getReads().clear();
							}

							// Update the tracking state of the redundant variable.
							stateY.getWrites().removeIf(l -> l.instruction == vin);

							// Replace store instruction with POP.
							if (instructions.indexOf(vin) >= 0)
								instructions.set(vin, new InsnNode(Types.isWide(varType) ? POP2 : POP));
							dirty = true;
						}
					}
				} else if (op == IINC && insn instanceof IincInsnNode iinc) {
					// Remove variable increments if:
					// - They value is known at all points of variable use
					// - They value is read from, but only by other iinc operations
					LocalAccessState state = states.get(key(iinc.var, Type.INT));
					if (state != null && (state.isEffectiveConstant() || state.getReads().stream().noneMatch(r -> r.instruction.getOpcode() != IINC))) {
						instructions.set(iinc, new InsnNode(NOP));
						dirty = true;
					}
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	/**
	 * Determines if a store operation to a variable is redundant, meaning it mirrors
	 * the value of another variable, and neither variable is modified between their usage.
	 *
	 * @param states
	 * 		Variable access states.
	 * @param instructions
	 * 		Method instructions.
	 * @param slotX
	 * 		The original variable index.
	 * @param slotY
	 * 		The target variable index to check for redundancy.
	 * @param typeSort
	 * 		The type sort of the variable, to handle all primitives + object types.
	 *
	 * @return {@code true} when the store to slotY is redundant.
	 */
	private static boolean isRedundantStore(@Nonnull Int2ObjectMap<LocalAccessState> states,
	                                        @Nonnull InsnList instructions,
	                                        int insnIndex, int slotX, int slotY, int typeSort) {
		LocalAccessState stateX = states.get(key(slotX, typeSort));
		LocalAccessState stateY = states.get(key(slotY, typeSort));

		// If no state exists for either slot, we cannot conclude redundancy.
		if (stateX == null || stateY == null)
			return false;

		// Redundancy only applies if there is a single write to Y.
		NavigableSet<LocalAccess> writesToY = stateY.getWrites();
		if (writesToY.size() != 1)
			return false;

		// Get the single write instruction to Y.
		LocalAccess writeToY = writesToY.first();
		AbstractInsnNode writeInsnY = writeToY.instruction;
		if (!(writeInsnY instanceof VarInsnNode writeVin))
			return false;

		// Check if the instruction type matches the expected type.
		if (!isMatchingStore(typeSort, writeVin.getOpcode()))
			return false;

		// Check if the prior instruction is the copy source of 'load x'.
		AbstractInsnNode previousInsn = writeInsnY.getPrevious();
		if (!(previousInsn instanceof VarInsnNode prevVin
				&& isVarLoad(prevVin.getOpcode())
				&& prevVin.var == slotX))
			return false;

		// Verify there are no intervening writes to X or Y between the observed instructions.
		int writeIndexY = instructions.indexOf(writeInsnY);
		for (int i = instructions.indexOf(previousInsn) + 1; i < writeIndexY; i++) {
			AbstractInsnNode insn = instructions.get(i);

			// Check if there are any writes to X or Y in the scope.
			// Any intermediate modification invalidates redundancy.
			if (insn instanceof VarInsnNode vin) {
				int var = vin.var;
				int opcode = vin.getOpcode();
				if ((var == slotX || var == slotY) && isVarStore(opcode))
					return false;
			} else if (insn instanceof IincInsnNode iinc) {
				int var = iinc.var;
				if ((var == slotX || var == slotY))
					return false;
			}
		}

		// Check that X is defined before Y.
		// A variable defined later cannot make an earlier variable redundant.
		NavigableSet<LocalAccess> writesToX = stateX.getWrites();
		if (writesToX.isEmpty())
			return false;
		LocalAccess firstWriteToX = writesToX.first(); // First write to X
		int writeIndexX = instructions.indexOf(firstWriteToX.instruction);
		if (writeIndexX >= writeIndexY)
			return false;

		// The store to Y is redundant. Every use case of it can be replaced with X.
		return true;
	}

	/**
	 * Replaces usage of the redundant variable with the original variable.
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param slotX
	 * 		The original variable index.
	 * @param slotY
	 * 		The target variable index that is redundant.
	 * @param typeSort
	 * 		The variable's type sort.
	 */
	private static void replaceRedundantVariableUsage(@Nonnull InsnList instructions, int slotX, int slotY, int typeSort) {
		AbstractInsnNode replacement = AsmInsnUtil.createVarLoad(slotX, typeSort);
		for (int i = 0; i < instructions.size(); i++) {
			AbstractInsnNode insn = instructions.get(i);
			if (insn instanceof VarInsnNode vin && vin.var == slotY) {
				int op = vin.getOpcode();
				if (isVarLoad(op))
					instructions.set(insn, replacement.clone(null));
			}
		}
	}

	/**
	 * @param typeSort
	 * 		The variable type sort.
	 * @param opcode
	 * 		The opcode to check.
	 *
	 * @return {@code true} if the opcode matches the respective store operation for the type.
	 */
	private static boolean isMatchingStore(int typeSort, int opcode) {
		return switch (typeSort) {
			case Type.INT -> opcode == ISTORE;
			case Type.FLOAT -> opcode == FSTORE;
			case Type.LONG -> opcode == LSTORE;
			case Type.DOUBLE -> opcode == DSTORE;
			case Type.OBJECT -> opcode == ASTORE;
			default -> false;
		};
	}

	/**
	 * @param typeSort
	 * 		The variable type sort.
	 * @param opcode
	 * 		The opcode to check.
	 *
	 * @return {@code true} if the opcode matches the respective load operation for the type.
	 */
	private static boolean isMatchingLoad(int typeSort, int opcode) {
		return switch (typeSort) {
			case Type.INT -> opcode == ILOAD;
			case Type.FLOAT -> opcode == FLOAD;
			case Type.LONG -> opcode == LLOAD;
			case Type.DOUBLE -> opcode == DLOAD;
			case Type.OBJECT -> opcode == ALOAD;
			default -> false;
		};
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		// This transformer results in the creation of a lot of POP/POP2 instructions.
		// The stack-operation folding transformer can clean up afterward.
		return Collections.singleton(OpaqueConstantFoldingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Variable folding";
	}

	/**
	 * @param slot
	 * 		Variable index.
	 * @param typeSort
	 * 		Variable type sort. See {@link Type#getSort()}.
	 *
	 * @return Key of typed variable.
	 */
	private static int key(int slot, int typeSort) {
		return slot | (typeSort << 16);
	}

	/**
	 * @param key
	 * 		Key of typed variable.
	 *
	 * @return Variable index stored in the key.
	 */
	private static int slotFromKey(int key) {
		return key & 0xFFFF;
	}

	/**
	 * State tracking for when variables are read from and written to, along with the common merged value.
	 * <br>
	 * These states are keyed by {@link #key(int, int)} which ensures that multiple types can target the
	 * same variable index without issue, should there be attempts to observe the merged {@link ReValue}.
	 * Since the key takes in {@link Type#getSort()} there should never be invalid merge operations of
	 * incompatible value types.
	 */
	private static class LocalAccessState {
		private final int index;
		private NavigableSet<LocalAccess> reads;
		private NavigableSet<LocalAccess> writes;
		private ReValue mergedValue;
		private boolean isArray;

		private LocalAccessState(int index) {
			this(index, null);
		}

		private LocalAccessState(int index, @Nullable ReValue initialValue) {
			this.index = index;
			mergedValue = initialValue;
		}

		public void addRead(int offset, @Nonnull AbstractInsnNode instruction) {
			if (reads == null)
				reads = new TreeSet<>();
			reads.add(new LocalAccess(offset, instruction));
		}

		public void addWrite(int offset, @Nonnull AbstractInsnNode instruction) {
			if (writes == null)
				writes = new TreeSet<>();
			writes.add(new LocalAccess(offset, instruction));
		}

		public void setState(@Nonnull ReValue value) {
			if (value instanceof ArrayValue)
				isArray = true;
			mergedValue = value;
		}

		public void mergeState(@Nonnull ReValue value) throws IllegalValueException {
			if (value instanceof ArrayValue)
				isArray = true;
			if (mergedValue == null)
				mergedValue = value;
			else
				// TODO: Type checking we normally do in ReInterpreter not done here
				//  IE: A String can be "mergeWith" an Integer which throws an exception
				mergedValue = mergedValue.mergeWith(value);
		}

		@Nonnull
		public NavigableSet<LocalAccess> getReads() {
			if (reads == null)
				return Collections.emptyNavigableSet();
			return reads;
		}

		@Nonnull
		public NavigableSet<LocalAccess> getReadsUpTo(int offset) {
			// TODO: This is a linear check, which can be defeated by basic flow obfuscation
			//  - We need to rewalk the control flow and find all the paths that lead to this instruction/offset
			//    then get any local accesses that are along that path.
			//    - Can take a look at the recent 'natural flow' fix in 'goto-inlining-transformer' to copy/paste from
			//    - Then we can remove the 'controlFlowObserved' where this method is being used
			return getReads().stream()
					.filter(l -> l.offset < offset)
					.collect(Collectors.toCollection(TreeSet::new));
		}

		@Nonnull
		public NavigableSet<LocalAccess> getWritesUpTo(int offset) {
			// TODO: Same linear check problem as above
			return getWrites().stream()
					.filter(l -> l.offset < offset)
					.collect(Collectors.toCollection(TreeSet::new));
		}

		@Nonnull
		public NavigableSet<LocalAccess> getWrites() {
			if (writes == null)
				return Collections.emptyNavigableSet();
			return writes;
		}

		/**
		 * @return {@code true} when the observed values can be represented as a constant.
		 */
		public boolean isEffectiveConstant() {
			return mergedValue != null && mergedValue.hasKnownValue();
		}

		/**
		 * @return {@code true} when the {@link #mergedValue} represents an inlinable value.
		 */
		public boolean isInlinableValue() {
			return !isArray;
		}
	}

	/**
	 * Model of an instruction at some code offset that accesses a variable.
	 *
	 * @param offset
	 * 		Instruction index in {@link MethodNode#instructions}.
	 * @param instruction
	 * 		Instruction accessing a local variable.
	 */
	private record LocalAccess(int offset, @Nonnull AbstractInsnNode instruction) implements Comparable<LocalAccess> {
		@Override
		public int compareTo(LocalAccess o) {
			return Integer.compare(offset, o.offset);
		}
	}
}
