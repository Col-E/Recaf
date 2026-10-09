package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
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
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
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
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
	public static final String IDENTIFIER = "peephole.data.varfold";

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

			// Fold constant locals whose single definition remains hidden behind control flow.
			dirty |= foldSingleConstantLocals(className, method);

			// Build successor and predecessor maps modeling control flow.
			Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectMap<>();
			Int2ObjectMap<List<Integer>> predecessorMap = new Int2ObjectMap<>();
			populateFlowMaps(method, successorMap, predecessorMap);

			// Compute liveness using iterative backward data-flow analysis.
			int size = instructions.size();
			List<Set<Integer>> inLive = new ArrayList<>(size);
			List<Set<Integer>> outLive = new ArrayList<>(size);
			populateLiveness(method, inLive, outLive, successorMap, predecessorMap);

			// Populate local access state.
			Int2ObjectMap<LocalAccessState> accessStates = new Int2ObjectMap<>();
			populateVariableAccessStates(method, accessStates);

			// Find locals that are modified in a loop, as they can look constant at a converged frame despite changing on a back edge.
			Set<Integer> loopModifiedSlots = Collections.emptySet(); // TODO: find the sample loop writes broke then isolate and unit test for findLoopModifiedSlots(instructions, successorMap, predecessorMap);
			Set<Integer> handlerReachable = findHandlerReachable(method);

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
					// Handler locals merge exceptional paths that are not represented by normal liveness.
					if (handlerReachable.contains(i))
						continue;

					// Loop-carried locals can look constant at one converged frame despite changing on a back edge.
					if (loopModifiedSlots.contains(vin.var))
						continue;

					// Fold loads only when the frame proves the value on every path reaching this instruction.
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
					if (!liveAfter.contains(var) && !isProtectedByTryCatch(method, insn)) {
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

			// Handle redundant variable copies one definition at a time so later slot writes do not hide earlier copies.
			int[] keys = accessStates.keys();
			for (int keyY : keys) {
				LocalAccessState stateY = accessStates.get(keyY);
				int slotY = slotFromKey(keyY);
				int typeSort = typeSortFromKey(keyY);

				// Each write gets its own reachability range.
				// Later writes to the same slot are separate definitions.
				List<LocalAccess> writesY = new ArrayList<>(stateY.getWrites());
				for (LocalAccess writeAccessY : writesY) {
					if (writeAccessY.offset < 0 || writeAccessY.offset >= instructions.size())
						continue;

					// Get the single write instruction to Y.
					// Validate that it is still the same instruction in the list, as a prior pass may have removed it.
					AbstractInsnNode writeInsnY = writeAccessY.instruction;
					if (instructions.get(writeAccessY.offset) != writeInsnY)
						continue;
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

					// Check if this definition of Y is redundant and collect only the reads it reaches.
					Set<Integer> foldableReads = new HashSet<>();
					if (!isRedundantStore(accessStates, instructions, successorMap, writeAccessY.offset, slotX, slotY, typeSort, foldableReads))
						continue;

					// Replace only the reads reached by this definition.
					// For instance:
					//   aload source
					//   astore copy
					//   ...
					//   aload copy <----- Can be replaced with 'source'
					//   ...
					//   // Update 'copy' to hold a new value, denotes the start of a new definition
					//   invokestatic Foo.get ()Ljava/lang/Object;
					//   astore copy
					//   ...
					//   aload copy <--- CANNOT be replaced with 'source'
					replaceRedundantVariableUsage(instructions, slotX, slotY, typeSort, foldableReads);

					// Remove folded accesses so later candidates do not reuse a removed definition.
					if (stateY.writes != null)
						stateY.writes.remove(writeAccessY);
					if (stateY.reads != null)
						stateY.reads.removeIf(access -> foldableReads.contains(access.offset));

					// Replace store with POP.
					Type varType = Types.fromSort(typeSort);
					instructions.set(writeInsnY, new InsnNode(varType.getSize() == 2 ? POP2 : POP));
					dirty = true;
				}
			}
		}
		if (dirty) {
			// Removed stores and folded loads change which locals are assigned, which existing frames may still claim.
			context.setRecomputeFrames(className);
			context.setNode(bundle, initialClassState, node);
		}
	}

	/**
	 * Populate variable liveness for the method.
	 * <p>
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
	 * <p>
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
	 * @param instructions
	 * 		Instructions to analyze.
	 * @param successorMap
	 * 		Flow successor map.
	 * @param predecessorMap
	 * 		Flow predecessor map.
	 *
	 * @return Set of variable slots that are modified in a loop.
	 */
	@Nonnull
	private static Set<Integer> findLoopModifiedSlots(@Nonnull InsnList instructions,
	                                                  @Nonnull Int2ObjectMap<List<Integer>> successorMap,
	                                                  @Nonnull Int2ObjectMap<List<Integer>> predecessorMap) {
		Set<Integer> result = new HashSet<>();
		int size = instructions.size();
		for (int from = 0; from < size; from++) {
			for (int target : successorMap.getOrDefault(from, emptyList())) {
				if (target < 0 || target > from)
					continue;

				// Walk the natural loop so unrelated locals in the surrounding method stay foldable.
				Set<Integer> loopNodes = new HashSet<>();
				Deque<Integer> pending = new ArrayDeque<>();
				loopNodes.add(target);
				loopNodes.add(from);
				pending.add(from);
				while (!pending.isEmpty()) {
					int current = pending.removeFirst();
					for (int predecessor : predecessorMap.getOrDefault(current, emptyList())) {
						if (predecessor >= target && loopNodes.add(predecessor))
							pending.addLast(predecessor);
					}
				}

				// Check all instructions in the loop for variable writes.
				for (int nodeIndex : loopNodes) {
					AbstractInsnNode instruction = instructions.get(nodeIndex);
					if (instruction instanceof VarInsnNode variable && isVarStore(variable.getOpcode()))
						result.add(variable.var);
					else if (instruction instanceof IincInsnNode increment)
						result.add(increment.var);
				}
			}
		}
		return result;
	}

	/**
	 * @param method
	 * 		Method to check.
	 * @param target
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction is protected by a try/catch block, {@code false} otherwise.
	 */
	private static boolean isProtectedByTryCatch(@Nonnull MethodNode method, @Nonnull AbstractInsnNode target) {
		if (method.tryCatchBlocks == null)
			return false;
		for (TryCatchBlockNode block : method.tryCatchBlocks)
			for (AbstractInsnNode current = block.start; current != null && current != block.end; current = current.getNext())
				if (current == target)
					return true;
		return false;
	}

	/**
	 * @param method
	 * 		Method to analyze.
	 *
	 * @return Set of instruction indices that are reachable from any exception handler.
	 */
	@Nonnull
	private static Set<Integer> findHandlerReachable(@Nonnull MethodNode method) {
		if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty())
			return Collections.emptySet();

		// Follow only normal edges from each handler so shared post-handler code is treated conservatively.
		Int2ObjectMap<List<Integer>> successors = new Int2ObjectMap<>();
		populateFlowMaps(method, successors, new Int2ObjectMap<>(), false);
		Deque<Integer> pending = new ArrayDeque<>();
		Set<Integer> reachable = new HashSet<>();
		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			int handlerIndex = method.instructions.indexOf(block.handler);
			if (handlerIndex >= 0 && reachable.add(handlerIndex))
				pending.addLast(handlerIndex);
		}
		while (!pending.isEmpty()) {
			int current = pending.removeFirst();
			for (int successor : successors.getOrDefault(current, emptyList()))
				if (reachable.add(successor))
					pending.addLast(successor);
		}
		return reachable;
	}

	/**
	 * Folds locals that are written exactly once from a known constant into their reads.
	 * <p>
	 * Unlike the main frame-based pass, this {@link SourceValue} based pass can trace constants
	 * into stores even when intervening stores and control flow hide the connection.
	 *
	 * @param owner
	 * 		Owner of the method being analyzed.
	 * @param method
	 * 		Method whose local accesses should be folded.
	 *
	 * @return {@code true} when one or more local reads were replaced.
	 */
	private static boolean foldSingleConstantLocals(@Nonnull String owner, @Nonnull MethodNode method) {
		InsnList instructions = method.instructions;
		if (instructions == null || instructions.size() == 0)
			return false;

		// Track typed local definitions and uses before changing any instructions.
		Int2ObjectMap<LocalAccessState> accessStates = new Int2ObjectMap<>();
		populateVariableAccessStates(method, accessStates);

		// Source analysis identifies the constant that reaches a store even when other stores sit between them.
		Frame<SourceValue>[] frames;
		try {
			frames = new Analyzer<>(new SourceInterpreter()).analyze(owner, method);
		} catch (Throwable t) {
			// If analysis fails, we cannot safely fold any locals.
			return false;
		}

		boolean dirty = false;
		Set<Integer> handlerReachable = findHandlerReachable(method);
		for (int key : accessStates.keys()) {
			LocalAccessState state = accessStates.get(key);
			NavigableSet<LocalAccess> writes = state.getWrites();
			NavigableSet<LocalAccess> reads = state.getReads();
			if (writes.size() != 1 || reads.isEmpty())
				continue;

			// Parameters and synthetic receiver definitions do not have a constant source instruction.
			LocalAccess write = writes.first();
			if (write.offset < 0
					|| !(write.instruction instanceof VarInsnNode store)
					|| !isVarStore(store.getOpcode()))
				continue;

			// Null frames indicate unreachable code, so skip any stores that are not reachable.
			int storeIndex = write.offset;
			if (storeIndex >= frames.length || frames[storeIndex] == null)
				continue;

			// Skip if there's no value on the stack to store.
			Frame<SourceValue> frame = frames[storeIndex];
			if (frame.getStackSize() == 0)
				continue;

			// Must have a single source instruction for the value being stored,
			// otherwise we cannot safely duplicate it at every read.
			SourceValue source = frame.getStack(frame.getStackSize() - 1);
			if (source == null || source.insns.size() != 1)
				continue;

			// Skip if the source instruction is not a constant of the store's type.
			AbstractInsnNode producer = source.insns.iterator().next();
			if (!isConstantForStore(producer, store))
				continue;

			// Every read must occur after the one definition so no path can observe an uninitialized local.
			boolean ordered = true;
			for (LocalAccess read : reads) {
				if (read.offset <= storeIndex
						|| !(read.instruction instanceof VarInsnNode load)
						|| !isVarLoad(load.getOpcode())) {
					ordered = false;
					break;
				}
			}
			if (!ordered)
				continue;

			// If a read occurs in a handler-reachable instruction, we cannot safely fold it.
			// Track all non-handler reachable reads so we can fold them.
			List<LocalAccess> foldableReads = new ArrayList<>();
			for (LocalAccess read : reads) {
				if (!handlerReachable.contains(read.offset))
					foldableReads.add(read);
			}
			if (foldableReads.isEmpty())
				continue;

			// Replace each safe read with an independent constant instruction.
			for (LocalAccess read : foldableReads)
				instructions.set(read.instruction, producer.clone(Collections.emptyMap()));
			dirty = true;
		}
		return dirty;
	}

	/**
	 * @param producer
	 * 		Instruction supplying the value consumed by a local store.
	 * @param store
	 * 		Local store consuming the value.
	 *
	 * @return {@code true} when the producer is a constant of the store's type.
	 */
	private static boolean isConstantForStore(@Nonnull AbstractInsnNode producer, @Nonnull VarInsnNode store) {
		int producerOp = producer.getOpcode();
		int storeOp = store.getOpcode();
		if (!isConstValue(producerOp))
			return false;
		return switch (storeOp) {
			case ISTORE -> isConstIntValue(producer);
			case FSTORE ->
					producerOp >= FCONST_0 && producerOp <= FCONST_2 || producer instanceof LdcInsnNode ldc && ldc.cst instanceof Float;
			case LSTORE ->
					producerOp >= LCONST_0 && producerOp <= LCONST_1 || producer instanceof LdcInsnNode ldc && ldc.cst instanceof Long;
			case DSTORE ->
					producerOp >= DCONST_0 && producerOp <= DCONST_1 || producer instanceof LdcInsnNode ldc && ldc.cst instanceof Double;
			case ASTORE ->
					producerOp == ACONST_NULL || producer instanceof LdcInsnNode ldc && ldc.cst instanceof String;
			default -> false;
		};
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
	 * @param foldableReads
	 * 		Output instruction offsets reached by this definition while the copy remains valid.
	 *
	 * @return {@code true} when the store to Y is redundant and has at least one reachable use.
	 */
	private static boolean isRedundantStore(@Nonnull Int2ObjectMap<LocalAccessState> accessStates,
	                                        @Nonnull InsnList instructions,
	                                        @Nonnull Int2ObjectMap<List<Integer>> successorMap,
	                                        int storeIndexY, int slotX, int slotY, int typeSort,
	                                        @Nonnull Set<Integer> foldableReads) {
		foldableReads.clear();
		LocalAccessState stateX = accessStates.get(key(slotX, typeSort));
		if (stateX == null)
			return false;

		// Prior is load from X.
		AbstractInsnNode storeInsnY = instructions.get(storeIndexY);
		AbstractInsnNode prev = storeInsnY.getPrevious();
		if (!(prev instanceof VarInsnNode vinX && vinX.var == slotX && isMatchingLoad(typeSort, vinX.getOpcode())))
			return false;

		// No intervening writes to X or Y between load X and store Y.
		int sourceSize = Types.fromSort(typeSort).getSize();
		int targetSize = sourceSize;
		for (int j = instructions.indexOf(prev) + 1; j < storeIndexY; j++) {
			AbstractInsnNode ins = instructions.get(j);
			if (isOverlappingVariableWrite(ins, slotX, sourceSize) || isOverlappingVariableWrite(ins, slotY, targetSize))
				return false;
		}

		// X defined before Y.
		NavigableSet<LocalAccess> writesX = stateX.getWrites();
		if (writesX.isEmpty())
			return false;
		if (writesX.first().offset >= storeIndexY)
			return false;

		// Propagate the copy fact from this definition until a later write to Y kills it.
		//
		// State values are bitmasks:
		//   0b0000 means unreachable
		//   0b0010 means the source was changed
		//   0b0100 means the target was killed
		//
		// Multiple bits mean paths with different states merged at a control-flow join,
		// such as one path reading Y before another path overwrites it. Examples:
		//
		//   validState (0b0001):
		//       iload x
		//       istore y
		//       iload y
		//
		//   sourceChangedState (0b0010):
		//       iload x
		//       istore y
		//       iinc x 1 // <-- changes the copy
		//       iload y
		//
		//   targetKilledState (0b0100):
		//       iload x
		//       istore y
		//       iconst_0
		//       istore y // <-- kills the copy
		//       iload y
		//
		//   validState | targetKilledState (0b0101) at a control-flow merge:
		//       iload x
		//       istore y
		//       iload condition
		//       ifeq OVERWRITE
		//       iload y
		//       pop
		//       goto JOIN
		//      OVERWRITE:
		//       iconst_0
		//       istore y // <-- kills the copy locally
		//      JOIN:
		//       iload y // <-- merges the copy-valid path with the copy-killed path
		final int validState = 1;
		final int sourceChangedState = 1 << 1;
		final int targetKilledState = 1 << 2;
		int size = instructions.size();
		int[] state = new int[size];

		// Add initial successors of store Y to the queue and mark them as unchanged.
		Deque<Integer> unprocessed = new ArrayDeque<>();
		for (int s : successorMap.getOrDefault(storeIndexY, emptyList())) {
			state[s] |= validState;
			unprocessed.add(s);
		}

		// Follow only the part of the graph where this particular definition still reaches Y.
		while (!unprocessed.isEmpty()) {
			int i = unprocessed.poll();
			AbstractInsnNode insn = instructions.get(i);
			int op = insn.getOpcode();
			int currentState = state[i];
			boolean copyIsValid = (currentState & validState) != 0;
			boolean sourceChanged = (currentState & sourceChangedState) != 0;
			boolean targetKilled = (currentState & targetKilledState) != 0;

			// IINC reads Y before writing it, so it can be remapped when safe, but it kills the copy afterward.
			if (typeSort == Type.INT && insn instanceof IincInsnNode iinc && iinc.var == slotY) {
				if (sourceChanged || (copyIsValid && targetKilled))
					return false;
				if (copyIsValid)
					foldableReads.add(i);

				for (int s : successorMap.getOrDefault(i, emptyList())) {
					int oldState = state[s];
					int newState = oldState | targetKilledState;
					if (newState != oldState) {
						state[s] = newState;
						unprocessed.add(s);
					}
				}
				continue;
			}

			// Any other write to Y, including a different type or an overlapping wide write, kills this definition.
			if (isOverlappingVariableWrite(insn, slotY, targetSize)) {
				for (int s : successorMap.getOrDefault(i, emptyList())) {
					int oldState = state[s];
					int newState = oldState | targetKilledState;
					if (newState != oldState) {
						state[s] = newState;
						unprocessed.add(s);
					}
				}
				continue;
			}

			// A matching load from Y consumes the value defined by this copy.
			if (insn instanceof VarInsnNode variable && variable.var == slotY && isMatchingLoad(typeSort, op)) {
				if (sourceChanged || (copyIsValid && targetKilled))
					return false;
				if (copyIsValid)
					foldableReads.add(i);
			}

			// Writes to X invalidate the copy but still propagate so unsafe reads can reject the candidate.
			int newState = currentState;
			if (copyIsValid && isOverlappingVariableWrite(insn, slotX, sourceSize))
				newState = (newState & ~validState) | sourceChangedState;
			for (int s : successorMap.getOrDefault(i, emptyList())) {
				int oldState = state[s];
				int newStatePropagated = oldState | newState;
				if (newStatePropagated != oldState) {
					state[s] = newStatePropagated;
					unprocessed.add(s);
				}
			}
		}

		// No reachable use means the normal liveness pass is responsible for removing the dead store.
		return !foldableReads.isEmpty();
	}

	/**
	 * @param instruction
	 * 		Instruction to check.
	 * @param slot
	 * 		Beginning of the local slot range.
	 * @param size
	 * 		Number of slots occupied by the range.
	 *
	 * @return {@code true} when the instruction writes any slot in the range.
	 */
	private static boolean isOverlappingVariableWrite(@Nonnull AbstractInsnNode instruction, int slot, int size) {
		int end = slot + size;
		if (instruction instanceof VarInsnNode variable && isVarStore(variable.getOpcode())) {
			int variableEnd = variable.var + getTypeForVarInsn(variable).getSize();
			return variable.var < end && variableEnd > slot;
		}
		return instruction instanceof IincInsnNode iinc
				&& iinc.var >= slot
				&& iinc.var < end;
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
	 * @param foldableReads
	 * 		Instruction offsets to replace.
	 */
	private static void replaceRedundantVariableUsage(@Nonnull InsnList instructions, int slotX, int slotY, int typeSort,
	                                                  @Nonnull Set<Integer> foldableReads) {
		AbstractInsnNode replacement = createVarLoad(slotX, typeSort);
		for (int offset : foldableReads) {
			if (offset < 0 || offset >= instructions.size())
				continue;
			AbstractInsnNode insn = instructions.get(offset);
			if (insn instanceof VarInsnNode vin && vin.var == slotY && isMatchingLoad(typeSort, vin.getOpcode())) {
				instructions.set(insn, replacement.clone(null));
			} else if (typeSort == Type.INT && insn instanceof IincInsnNode iinc && iinc.var == slotY) {
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
	public String identifier() {
		return IDENTIFIER;
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
		int normalizedSort = Types.getNormalizedSort(typeSort);
		return slot | (normalizedSort << 16);
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