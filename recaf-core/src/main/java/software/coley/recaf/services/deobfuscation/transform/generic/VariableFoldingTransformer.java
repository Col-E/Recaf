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
import org.objectweb.asm.tree.MethodNode;
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
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static software.coley.recaf.util.AsmInsnUtil.*;

/**
 * A transformer that folds redundant variable use.
 * <br>
 * You should use {@link StackOperationFoldingTransformer} after using this for {@code POP} cleanup.
 *
 * @author Matt Coley
 */
@Dependent
public class VariableFoldingTransformer implements JvmClassTransformer {
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public VariableFoldingTransformer(@Nonnull WorkspaceManager workspaceManager,
	                                  @Nonnull InheritanceGraphService graphService) {
		this.workspaceManager = workspaceManager;
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
				boolean controlFlowObserved = false;
				for (int i = 0; i < instructions.size(); i++) {
					AbstractInsnNode insn = instructions.get(i);
					int op = insn.getOpcode();

					// Record variable reads
					if (isVarLoad(op) && insn instanceof VarInsnNode vin) {
						Frame<ReValue> frame = frames[i];
						if (frame == null)
							continue; // Skip dead code
						ReValue top = frame.getStack(frame.getStackSize() - 1);
						LocalAccessState state = states.computeIfAbsent(key(vin.var, getTypeForVarInsn(vin).getSort()), k -> new LocalAccessState(vin.var));
						state.addRead(i, vin);
					}

					// Record variable writes
					else if (isVarStore(op) && insn instanceof VarInsnNode vin) {
						Frame<ReValue> frame = frames[i];
						if (frame == null)
							continue; // Skip dead code
						ReValue top = frame.getStack(frame.getStackSize() - 1);
						LocalAccessState state = states.computeIfAbsent(key(vin.var, getTypeForVarInsn(vin).getSort()), k -> new LocalAccessState(vin.var));
						state.addWrite(i, vin);

						// If we're moving forward in the method from the beginning, and haven't seen
						// the variable being used yet or any kind of control flow, then we can safely
						// just replace the state with the value on the stack instead of merging it.
						if (state.getReadsUpTo(i).isEmpty() && !controlFlowObserved)
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
						state.mergeState(iinc);
					}

					// Other state tracking which can change how we merge state
					else if (isFlowControl(insn)) {
						controlFlowObserved = true;
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
						Frame<ReValue> frame = frames[i];
						if (frame == null)
							continue; // Skip dead code
						ReValue localValue = frame.getLocal(vin.var);
						if (localValue.hasKnownValue()) {
							AbstractInsnNode replacement = switch (localValue) {
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
					}
				} else if (isVarStore(op) && insn instanceof VarInsnNode vin) {
					// Remove variable writes if:
					// - They are never read from
					// - They are read from, but only used with a single known constant value (which we inline above).
					Type varType = getTypeForVarInsn(vin);
					LocalAccessState state = states.get(key(vin.var, varType.getSort()));
					if (state != null && (state.getReads().isEmpty() || state.isEffectiveConstant())) {
						instructions.set(vin, new InsnNode(varType.getSize() == 1 ? POP : POP2));
						dirty = true;
					}
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		// This transformer results in the creation of a lot of POP/POP2 instructions.
		// The stack-operation folding transformer can clean up afterward.
		return Collections.singleton(StackOperationFoldingTransformer.class);
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
	 * State tracking for when variables are read from and written to, along with the common merged value.
	 * <br>
	 * These states are keyed by {@link #key(int, int)} which ensures that multiple types can target the
	 * same variable index without issue, should there be attempts to observe the merged {@link ReValue}.
	 * Since the key takes in {@link Type#getSort()} there should never be invalid merge operations of
	 * incompatible value types.
	 */
	private static class LocalAccessState {
		private final int index;
		private SortedSet<LocalAccess> reads;
		private SortedSet<LocalAccess> writes;
		private ReValue mergedValue;

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
			mergedValue = value;
		}

		public void mergeState(@Nonnull ReValue value) throws IllegalValueException {
			if (mergedValue == null)
				mergedValue = value;
			else
				mergedValue = mergedValue.mergeWith(value);
		}

		public void mergeState(@Nonnull IincInsnNode iinc) throws IllegalValueException {
			if (mergedValue == null)
				mergeState(IntValue.UNKNOWN);
			else if (mergedValue instanceof IntValue mergedIntValue)
				mergeState(mergedIntValue.add(iinc.incr));
			else if (mergedValue != UninitializedValue.UNINITIALIZED_VALUE)
				throw new IllegalValueException("Cannot merge iinc into value: " + mergedValue);
		}

		@Nonnull
		public SortedSet<LocalAccess> getReads() {
			if (reads == null)
				return Collections.emptySortedSet();
			return reads;
		}

		@Nonnull
		public SortedSet<LocalAccess> getReadsUpTo(int offset) {
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
		public SortedSet<LocalAccess> getWrites() {
			if (writes == null)
				return Collections.emptySortedSet();
			return writes;
		}

		/**
		 * @return {@code true} when the observed values can be represented as a constant.
		 */
		public boolean isEffectiveConstant() {
			return mergedValue != null && mergedValue.hasKnownValue();
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
