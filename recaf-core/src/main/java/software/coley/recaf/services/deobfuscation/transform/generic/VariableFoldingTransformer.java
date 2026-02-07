package software.coley.recaf.services.deobfuscation.transform.generic;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyNavigableSet;
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

			// Build successor and predecessor maps modeling control flow.
			Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectArrayMap<>();
			Int2ObjectMap<List<Integer>> predecessorMap = new Int2ObjectArrayMap<>();
			populateFlowMaps(method, successorMap, predecessorMap);

			// Compute liveness using iterative backward data-flow analysis.
			int size = instructions.size();
			List<Set<Integer>> inLive = new ArrayList<>(size);
			List<Set<Integer>> outLive = new ArrayList<>(size);
			populateLiveness(method, inLive, outLive, successorMap, predecessorMap);

			// Populate local access state.
			Int2ObjectMap<LocalAccessState> accessStates = new Int2ObjectArrayMap<>();
			populateVariableAccessStates(method, accessStates);

			// Fold in reverse order.
			Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
			for (int i = size - 1; i >= 0; i--) {
				AbstractInsnNode insn = instructions.get(i);
				int op = insn.getOpcode();

				// Skip if dead code (unreachable code not analyzed).
				Frame<ReValue> frame = frames[i];
				if (frame == null)
					continue;

				if (isVarLoad(op) && insn instanceof VarInsnNode vin) {
					// Fold constant loads.
					ReValue val = frame.getLocal(vin.var);
					if (val != null && val.hasKnownValue()) {
						AbstractInsnNode replacement = OpaqueConstantFoldingTransformer.toInsn(val);
						if (replacement != null) {
							instructions.set(insn, replacement);
							dirty = true;
						}
					}
				} else {
					// Get variable index and type from store/iinc.
					int var;
					Type type;
					if (insn instanceof VarInsnNode vin) {
						var = vin.var;
						type = getTypeForVarInsn(vin);
					} else if (insn instanceof IincInsnNode iinc) {
						var = iinc.var;
						type = Type.INT_TYPE;
					} else {
						continue;
					}

					// Remove dead stores/iinc.
					Set<Integer> liveAfter = outLive.get(i);
					if (!liveAfter.contains(var)) {
						if (op == IINC) {
							instructions.set(insn, new InsnNode(NOP));
						} else {
							AbstractInsnNode prev = insn.getPrevious();
							if (OpaqueConstantFoldingTransformer.isSupportedValueProducer(prev)) {
								instructions.set(prev, new InsnNode(NOP));
								instructions.set(insn, new InsnNode(NOP));
							} else {
								instructions.set(insn, new InsnNode(type.getSize() == 2 ? POP2 : POP));
							}
						}
						dirty = true;
					}
				}
			}

			// Handle redundant variable copies.
			int[] keys = accessStates.keySet().toIntArray();
			for (int keyY : keys) {
				LocalAccessState stateY = accessStates.get(keyY);
				int slotY = slotFromKey(keyY);
				int typeSort = typeSortFromKey(keyY);

				// Redundancy only applies if there is a single write to Y.
				NavigableSet<LocalAccess> writesY = stateY.getWrites();
				if (writesY.size() != 1)
					continue;

				// Get the single write instruction to Y.
				LocalAccess writeAccessY = writesY.first();
				AbstractInsnNode writeInsnY = writeAccessY.instruction;
				if (!(writeInsnY instanceof VarInsnNode vinY && isVarStore(vinY.getOpcode())))
					continue;

				// Check if the prior instruction is the copy source of 'load x'.
				AbstractInsnNode prevInsn = writeInsnY.getPrevious();
				if (!(prevInsn instanceof VarInsnNode vinX && isVarLoad(vinX.getOpcode())))
					continue;

				// Check if the source variable is different and has a known state.
				int slotX = vinX.var;
				int keyX = key(slotX, typeSort);
				if (slotX == slotY || !accessStates.containsKey(keyX))
					continue;

				// Check if the store to Y is redundant.
				if (isRedundantStore(accessStates, instructions, successorMap, writeAccessY.offset, slotX, slotY, typeSort)) {
					// Replace usages.
					replaceRedundantVariableUsage(instructions, slotX, slotY, typeSort);

					// Update state.
					stateY.getWrites().clear();
					stateY.getReads().clear();

					// Replace store with POP.
					Type varType = Types.fromSort(typeSort);
					instructions.set(writeInsnY, new InsnNode(varType.getSize() == 2 ? POP2 : POP));
					dirty = true;
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	/**
	 * Populate variable liveness for the method.
	 * <p/>
	 * Liveness is the set of variables that are live at each instruction.
	 * A good reference for this can be found <a href="https://users.cs.northwestern.edu/%7Esimonec/files/Teaching/CAT/slides/DFA_part1.pdf">here</a>.
	 *
	 * @param method
	 * 		Method to analyze.
	 * @param inLive
	 * 		Output in-live sets.
	 * @param outLive
	 * 		Output out-live sets.
	 * @param successorMap
	 * 		Flow successor map.
	 * @param predecessorMap
	 * 		Flow predecessor map.
	 */
	private static void populateLiveness(@Nonnull MethodNode method,
	                                     @Nonnull List<Set<Integer>> inLive,
	                                     @Nonnull List<Set<Integer>> outLive,
	                                     @Nonnull Int2ObjectMap<List<Integer>> successorMap,
	                                     @Nonnull Int2ObjectMap<List<Integer>> predecessorMap) {
		// Initialize empty live sets for every instruction.
		InsnList instructions = method.instructions;
		int size = instructions.size();
		for (int i = 0; i < size; i++) {
			inLive.add(new HashSet<>());
			outLive.add(new HashSet<>());
		}

		// Assume we need to check every instruction once.
		Deque<Integer> unprocessed = new ArrayDeque<>();
		for (int i = 0; i < size; i++)
			unprocessed.add(i);

		// Iterate until no changes occur.
		while (!unprocessed.isEmpty()) {
			// Next instruction to process.
			int i = unprocessed.poll();
			AbstractInsnNode insn = instructions.get(i);
			int op = insn.getOpcode();

			// A variable is live after the instruction if it is going to be used later, which can
			// be determined by looking at all successor instructions.
			Set<Integer> out = outLive.get(i);
			out.clear();
			for (int s : successorMap.getOrDefault(i, emptyList()))
				out.addAll(inLive.get(s));

			// Compute gen/kill sets for the instruction.
			// - Gen: Variables read by the instruction.
			// - Kill: Variables written to by the instruction.
			Set<Integer> gen = new HashSet<>();
			Set<Integer> kill = new HashSet<>();
			int var;
			if (insn instanceof VarInsnNode vin) {
				var = vin.var;
				if (isVarLoad(op)) gen.add(var);
				if (isVarStore(op)) kill.add(var);
			} else if (insn instanceof IincInsnNode iinc) {
				// IINC both reads and writes the variable.
				var = iinc.var;
				gen.add(var);
				kill.add(var);
			}

			// Anything read by this instruction (gen) is live before it.
			// Anything written to by this instruction (kill) is not live going forward.
			// Everything else live after the instruction (out) is also live before it.
			Set<Integer> newIn = new HashSet<>(gen);
			Set<Integer> temp = new HashSet<>(out);
			temp.removeAll(kill);
			newIn.addAll(temp);

			// If the in-live set changed we discovered new live variables.
			// We need to re-check all instructions that can reach this one.
			if (!newIn.equals(inLive.get(i))) {
				inLive.set(i, newIn);

				// Queue predecessors for re-check.
				unprocessed.addAll(predecessorMap.getOrDefault(i, emptyList()));
			}
		}
	}

	/**
	 * Populate variable access states for the method.
	 * <p/>
	 * This tracks when variables are read from and written to, which is necessary
	 * for determining when variable copies are redundant and can be folded.
	 *
	 * @param method
	 * 		Method to analyze.
	 * @param accessStates
	 * 		Output variable access states.
	 */
	private static void populateVariableAccessStates(@Nonnull MethodNode method, @Nonnull Int2ObjectMap<LocalAccessState> accessStates) {
		InsnList instructions = method.instructions;
		int size = instructions.size();
		boolean isStatic = AccessFlag.isStatic(method.access);
		int paramSlot = isStatic ? 0 : 1;

		// Add implicit 'this' if non-static.
		if (!isStatic) {
			LocalAccessState thisState = new LocalAccessState(0);
			thisState.addWrite(-1, new VarInsnNode(ASTORE, 0));
			accessStates.put(key(0, Type.OBJECT), thisState);
		}

		// Add explicit parameters.
		for (Type argType : Type.getArgumentTypes(method.desc)) {
			LocalAccessState paramState = new LocalAccessState(paramSlot);
			paramState.addWrite(-1, createVarStore(paramSlot, argType));
			accessStates.put(key(paramSlot, argType.getSort()), paramState);
			paramSlot += argType.getSize();
		}

		// Populate accesses from instructions.
		for (int i = 0; i < size; i++) {
			AbstractInsnNode insn = instructions.get(i);
			int op = insn.getOpcode();
			if (insn instanceof VarInsnNode vin) {
				// Variable load/store.
				Type type = getTypeForVarInsn(vin);
				LocalAccessState state = accessStates.computeIfAbsent(key(vin.var, type.getSort()), _ -> new LocalAccessState(vin.var));
				if (isVarLoad(op))
					state.addRead(i, vin);
				else if (isVarStore(op))
					state.addWrite(i, vin);
			} else if (op == IINC && insn instanceof IincInsnNode iinc) {
				// Increment is both a read and write.
				LocalAccessState state = accessStates.computeIfAbsent(key(iinc.var, Type.INT), _ -> new LocalAccessState(iinc.var));
				state.addRead(i, iinc);
				state.addWrite(i, iinc);
			}
		}
	}

	/**
	 * @param accessStates
	 * 		Variable access states.
	 * @param instructions
	 * 		Method instructions.
	 * @param successorMap
	 * 		Control flow successor map.
	 * @param storeIndexY
	 * 		Instructions index of the store to Y.
	 * @param slotX
	 * 		The original variable index.
	 * @param slotY
	 * 		The target variable index to check for redundancy.
	 * @param typeSort
	 * 		The variable's type sort. See {@link Type#getSort()}.
	 *
	 * @return {@code true} when the store to Y is redundant and can be replaced safely.
	 */
	private static boolean isRedundantStore(@Nonnull Int2ObjectMap<LocalAccessState> accessStates,
	                                        @Nonnull InsnList instructions,
	                                        @Nonnull Int2ObjectMap<List<Integer>> successorMap,
	                                        int storeIndexY, int slotX, int slotY, int typeSort) {
		LocalAccessState stateX = accessStates.get(key(slotX, typeSort));
		LocalAccessState stateY = accessStates.get(key(slotY, typeSort));
		if (stateX == null || stateY == null)
			return false;

		// Single write to Y.
		NavigableSet<LocalAccess> writesY = stateY.getWrites();
		if (writesY.size() != 1)
			return false;
		LocalAccess writeY = writesY.first();
		if (writeY.offset != storeIndexY)
			return false;

		// Prior is load from X.
		AbstractInsnNode storeInsnY = instructions.get(storeIndexY);
		AbstractInsnNode prev = storeInsnY.getPrevious();
		if (!(prev instanceof VarInsnNode vinX && vinX.var == slotX && isMatchingLoad(typeSort, vinX.getOpcode())))
			return false;

		// No intervening writes to X or Y between load X and store Y.
		for (int j = instructions.indexOf(prev) + 1; j < storeIndexY; j++) {
			AbstractInsnNode ins = instructions.get(j);
			if (ins instanceof VarInsnNode vin) {
				if ((vin.var == slotX || vin.var == slotY) && isVarStore(vin.getOpcode()))
					return false;
			} else if (ins instanceof IincInsnNode iinc) {
				if (iinc.var == slotX || iinc.var == slotY)
					return false;
			}
		}

		// X defined before Y.
		NavigableSet<LocalAccess> writesX = stateX.getWrites();
		if (writesX.isEmpty())
			return false;
		if (writesX.first().offset >= storeIndexY)
			return false;

		// Check no updates to X on any path from store Y to reads of Y.
		//  -1 unvisited, 0 unchanged, 1 changed
		int size = instructions.size();
		int[] state = new int[size];
		Arrays.fill(state, -1);

		// Add initial successors of store Y to the queue and mark
		// them as unchanged (0) since we haven't seen any updates to X yet.
		Deque<Integer> unprocessed = new ArrayDeque<>();
		for (int s : successorMap.getOrDefault(storeIndexY, emptyList())) {
			state[s] = 0;
			unprocessed.add(s);
		}

		// Iteratively propagate state until we reach all reads of Y.
		while (!unprocessed.isEmpty()) {
			int i = unprocessed.poll();
			AbstractInsnNode insn = instructions.get(i);
			int op = insn.getOpcode();
			boolean isXWrite = (isVarStore(op) && ((VarInsnNode) insn).var == slotX) ||
					(op == IINC && ((IincInsnNode) insn).var == slotX);
			int newState = state[i];
			if (isXWrite)
				newState = 1;
			for (int s : successorMap.getOrDefault(i, emptyList())) {
				int oldState = state[s];
				int newStatePropagated = Math.max(oldState == -1 ? newState : oldState, newState);
				if (newStatePropagated != oldState) {
					state[s] = newStatePropagated;
					unprocessed.add(s);
				}
			}
		}

		// If any read of Y is reachable from store Y without seeing an
		// update to X (state 0 - unchanged), then the store is not redundant.
		for (LocalAccess access : stateY.getReads())
			if (state[access.offset] == 1)
				return false;
		return true;
	}

	/**
	 * Replaces usage of the redundant variable with the original variable.
	 *
	 * @param instructions
	 * 		Instructions to modify.
	 * @param slotX
	 * 		The original variable index.
	 * @param slotY
	 * 		The target variable index that is redundant.
	 * @param typeSort
	 * 		The variable's type sort. See {@link Type#getSort()}.
	 */
	private static void replaceRedundantVariableUsage(@Nonnull InsnList instructions, int slotX, int slotY, int typeSort) {
		AbstractInsnNode replacement = createVarLoad(slotX, typeSort);
		for (int i = 0; i < instructions.size(); i++) {
			AbstractInsnNode insn = instructions.get(i);
			if (insn instanceof VarInsnNode vin && vin.var == slotY && isVarLoad(vin.getOpcode())) {
				instructions.set(insn, replacement.clone(null));
			} else if (insn instanceof IincInsnNode iinc && iinc.var == slotY) {
				iinc.var = slotX;
			}
		}
	}

	/**
	 * @param typeSort
	 * 		The variable's type sort. See {@link Type#getSort()}.
	 * @param opcode
	 * 		Variable load opcode.
	 *
	 * @return {@code true} when opcode matches expected type.
	 */
	private static boolean isMatchingLoad(int typeSort, int opcode) {
		return switch (typeSort) {
			case Type.INT -> opcode == ILOAD;
			case Type.FLOAT -> opcode == FLOAD;
			case Type.LONG -> opcode == LLOAD;
			case Type.DOUBLE -> opcode == DLOAD;
			case Type.OBJECT, Type.ARRAY -> opcode == ALOAD;
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
	 * @param key
	 * 		Key of typed variable.
	 *
	 * @return Variable type sort. See {@link Type#getSort()}.
	 */
	private static int typeSortFromKey(int key) {
		return key >> 16;
	}

	/**
	 * State tracking for when variables are read from and written to.
	 * <br>
	 * These states are keyed by {@link #key(int, int)} which ensures that multiple types can target the
	 * same variable index without issue.
	 */
	private static class LocalAccessState {
		private final int index;
		private NavigableSet<LocalAccess> reads;
		private NavigableSet<LocalAccess> writes;

		private LocalAccessState(int index) {
			this.index = index;
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

		@Nonnull
		public NavigableSet<LocalAccess> getReads() {
			if (reads == null)
				return emptyNavigableSet();
			return reads;
		}

		@Nonnull
		public NavigableSet<LocalAccess> getWrites() {
			if (writes == null)
				return emptyNavigableSet();
			return writes;
		}

		@Override
		public String toString() {
			return "LocalAccessState{" +
					"index=" + index +
					", reads=" + reads +
					", writes=" + writes +
					'}';
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
		public int compareTo(@Nonnull LocalAccess o) {
			return Integer.compare(offset, o.offset);
		}
	}
}