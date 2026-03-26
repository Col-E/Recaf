package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * A transformer that removes redundant try-catch blocks.
 *
 * @author Matt Coley
 */
@Dependent
public class RedundantTryCatchRemovingTransformer implements JvmClassTransformer {
	private static final String EX_NPE = "java/lang/NullPointerException";
	private static final String EX_ASE = "java/lang/ArrayStoreException";
	private static final String EX_AIOOBE = "java/lang/ArrayIndexOutOfBoundsException";
	private static final String EX_NASE = "java/lang/NegativeArraySizeException";
	private static final String EX_IMSE = "java/lang/IllegalMonitorStateException";
	private static final String EX_CCE = "java/lang/ClassCastException";
	private static final String EX_AE = "java/lang/ArithmeticException";

	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;
	private ExceptionCollectionTransformer exceptionCollector;

	@Inject
	public RedundantTryCatchRemovingTransformer(@Nonnull InheritanceGraphService graphService) {
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
		ClassNode node = context.getNode(bundle, initialClassState);
		exceptionCollector = context.getTransformer(ExceptionCollectionTransformer.class);
		for (MethodNode method : node.methods) {
			// Skip methods that have no code or no try-catch blocks, as they can't have redundant entries.
			if (method.instructions == null || method.instructions.size() == 0)
				continue;
			if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty())
				continue;

			try {
				dirty |= pruneRedundantTryCatches(context, node, method);
			} catch (TransformationException ex) {
				throw ex;
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when removing redundant try-catch blocks", t);
			}
		}

		// If we changed anything, we need to update the class node and mark frames for recomputation.
		if (dirty) {
			context.setRecomputeFrames(initialClassState.getName());
			context.setNode(bundle, initialClassState, node);
		}
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> dependencies() {
		return Set.of(ExceptionCollectionTransformer.class, DeadCodeRemovingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Redundant try-catch removal";
	}

	/**
	 * Removes redundant try-catch entries from the given method.
	 *
	 * @param context
	 * 		Transformation context.
	 * @param declaringClass
	 * 		Class declaring the method.
	 * @param method
	 * 		Method to transform.
	 *
	 * @return {@code true} when the method was changed.
	 *
	 * @throws TransformationException
	 * 		When dead-code pruning fails.
	 */
	private boolean pruneRedundantTryCatches(@Nonnull JvmTransformerContext context,
	                                         @Nonnull ClassNode declaringClass,
	                                         @Nonnull MethodNode method) throws TransformationException {
		InsnList instructions = method.instructions;

		// Snapshot the original state of the try-catch blocks so we can check if we made any changes at the end.
		List<TryCatchState> originalState = snapshotStates(instructions, method.tryCatchBlocks);

		// Pruning occurs in multiple passes to allow later passes to take advantage of the results of earlier ones.
		List<TryCatchBlockNode> tryCatches = mergeContinuousRanges(instructions, method.tryCatchBlocks);
		tryCatches = removeExactDuplicates(instructions, tryCatches);
		tryCatches = removeShadowedRanges(instructions, tryCatches);

		// Last pass requires frame analysis, so we do it after the cheaper passes to minimize the number of frames we need to analyze.
		Frame<ReValue>[] frames = context.analyze(inheritanceGraph, declaringClass, method);
		tryCatches = removeImpossibleCatches(instructions, frames, tryCatches);

		// If the final state is the same as the original state, we don't need to update anything.
		List<TryCatchState> updatedState = snapshotStates(instructions, tryCatches);
		if (originalState.equals(updatedState))
			return false;

		// Update the method's try-catch blocks and prune any now-unreachable code.
		method.tryCatchBlocks.clear();
		method.tryCatchBlocks.addAll(tryCatches);
		context.pruneDeadCode(declaringClass, method);
		return true;
	}

	/**
	 * Removes ranges that cannot possibly be utilized at runtime.
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatches
	 * 		Method try-catches.
	 *
	 * @return Deduplicated try-catch list.
	 */
	@Nonnull
	private List<TryCatchBlockNode> removeIgnoredRanges(@Nonnull InsnList instructions, @Nonnull List<TryCatchBlockNode> tryCatches) {
		// Collect all try-catch handlers keyed by their range
		Map<TryRange, List<TryCatchBlockNode>> handlersMap = new HashMap<>();
		for (TryCatchBlockNode tryCatch : tryCatches) {
			int start = codeBoundaryIndex(instructions, tryCatch.start);
			int end = codeBoundaryIndex(instructions, tryCatch.end);
			if (start < end) {
				TryRange range = new TryRange(start, end);
				handlersMap.computeIfAbsent(range, r -> new ArrayList<>()).add(tryCatch);
			}
		}

		// Prune handlers of narrower (or equal) types in the collection
		//  - Gives preference to handlers that appear first in the list, since the JVM will check them first.
		new HashMap<>(handlersMap).forEach((range, blocks) -> {
			Set<String> seenTypes = new HashSet<>();
			Iterator<TryCatchBlockNode> it = blocks.iterator();
			while (it.hasNext()) {
				TryCatchBlockNode block = it.next();
				String handledType = Objects.requireNonNullElse(block.type, "java/lang/Object");
				inner:
				{
					for (String seenType : seenTypes) {
						if (inheritanceGraph.isAssignableFrom(seenType, handledType)) {
							it.remove();
							break inner;
						}
					}
					seenTypes.add(handledType);
				}
			}
		});

		// Retain only remaining handlers in the collection
		Set<TryCatchBlockNode> allHandlers = handlersMap.values()
				.stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
		List<TryCatchBlockNode> retained = new ArrayList<>(tryCatches);
		retained.retainAll(tryCatches.stream()
				.filter(allHandlers::contains)
				.toList());
		return retained;
	}

	/**
	 * Merges adjacent ranges with identical handler targets and catch types.
	 * Take this example scenario where we have multiple try-catch blocks that all
	 * catch the same exception type and point to the same handler:
	 * <pre>{@code
	 *      try-handler: range=[A-B] handler=D:*
	 *      try-handler: range=[B-C] handler=D:*
	 *      try-handler: range=[C-D] handler=D:*
	 *      --- D handler ----
	 *      try-handler: range=[E-F] handler=D:*
	 *      try-handler: range=[F-G] handler=D:*
	 *      try-handler: range=[G-H] handler=D:*
	 * }</pre>
	 * This can be simplified to:
	 * <pre>{@code
	 *      try-handler: range=[A-D] handler=D:*
	 *      --- D handler ----
	 *      try-handler: range=[E-H] handler=D:*
	 * }</pre>
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatches
	 * 		Method try-catches.
	 *
	 * @return Condensed try-catch list.
	 */
	@Nonnull
	private static List<TryCatchBlockNode> mergeContinuousRanges(@Nonnull InsnList instructions, @Nonnull List<TryCatchBlockNode> tryCatches) {
		// Skip if there is only one entry, as it can't be merged with anything else.
		if (tryCatches.size() <= 1)
			return new ArrayList<>(tryCatches);

		// Compare each entry with the previous one to see if the ranges are adjacent and have the same handler and catch type.
		// We can keep merging as long as the entries are continuous, so we update the "previous" entry's end to merge the ranges together.
		// This results in intermediate try-catch entries being removed from the yielded list.
		List<TryCatchBlockNode> merged = new ArrayList<>(tryCatches.size());
		TryCatchBlockNode previous = null;
		for (TryCatchBlockNode current : tryCatches) {
			if (previous != null &&
					Objects.equals(previous.type, current.type) &&
					codeBoundaryIndex(instructions, previous.end) == codeBoundaryIndex(instructions, current.start) &&
					codeBoundaryIndex(instructions, previous.handler) == codeBoundaryIndex(instructions, current.handler)) {
				previous.end = current.end;
				continue;
			}

			merged.add(current);
			previous = current;
		}
		return merged;
	}

	/**
	 * Removes exact duplicate entries while preserving the first occurrence.
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatches
	 * 		Method try-catches.
	 *
	 * @return Deduplicated try-catch list.
	 */
	@Nonnull
	private static List<TryCatchBlockNode> removeExactDuplicates(@Nonnull InsnList instructions, @Nonnull List<TryCatchBlockNode> tryCatches) {
		// Skip if there is only one entry, as it can't be merged with anything else.
		if (tryCatches.size() <= 1)
			return new ArrayList<>(tryCatches);

		// Simple merge pass that uses a set to track seen entries.
		// We can use the snapshot state as a unique identifier for try-catch blocks, since it captures all relevant properties of the block.
		Set<TryCatchState> seen = new HashSet<>(tryCatches.size());
		List<TryCatchBlockNode> kept = new ArrayList<>(tryCatches.size());
		for (TryCatchBlockNode tryCatch : tryCatches)
			if (seen.add(snapshotState(instructions, tryCatch)))
				kept.add(tryCatch);
		return kept;
	}

	/**
	 * Removes entries that are fully shadowed by an earlier, broader entry in the exception table.
	 * Given the following {@code { start, end, handler, ex-type } } blocks:
	 * <pre>{@code
	 * { R, S, Q, * },
	 * { R, S, C, * },
	 * { R, S, S, Ljava/lang/ArrayIndexOutOfBoundsException; }
	 * }</pre>
	 * Only the first is going to be used.
	 * <ul>
	 *     <li>It appears first, so it will be checked first by the JVM</li>
	 *     <li>Its range covers all possible instructions of the other two try blocks</li>
	 *     <li>Its handled type is more generic <i>({@code "*"} is catch-all/null)</i></li>
	 * </ul>
	 * See: <a href="https://github.com/openjdk/jdk21u/blob/master/src/hotspot/share/oops/method.cpp#L227">method.cpp#fast_exception_handler_bci_for</a>
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatches
	 * 		Method try-catches.
	 *
	 * @return Try-catch list without shadowed entries.
	 */
	@Nonnull
	private List<TryCatchBlockNode> removeShadowedRanges(@Nonnull InsnList instructions, @Nonnull List<TryCatchBlockNode> tryCatches) {
		List<TryCatchBlockNode> kept = new ArrayList<>(tryCatches.size());
		for (TryCatchBlockNode tryCatch : tryCatches) {
			TryCatchState state = snapshotState(instructions, tryCatch);

			// Compare this try-catch block against all previously-kept blocks to see if
			// it is fully covered by any of them and has a more specific catch type.
			boolean shadowed = false;
			for (TryCatchBlockNode previous : kept) {
				TryCatchState previousState = snapshotState(instructions, previous);
				if (previousState.covers(state) && catchesSameOrBroaderException(previous.type, tryCatch.type)) {
					shadowed = true;
					break;
				}
			}

			// If the block is not shadowed by any previous block, we keep it for the final list.
			if (!shadowed)
				kept.add(tryCatch);
		}
		return kept;
	}

	/**
	 * Removes entries that cannot be matched by any reachable instruction in their protected range.
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param frames
	 * 		Method stack frames.
	 * @param tryCatches
	 * 		Method try-catches.
	 *
	 * @return Filtered try-catch list.
	 */
	@Nonnull
	private List<TryCatchBlockNode> removeImpossibleCatches(@Nonnull InsnList instructions,
	                                                        @Nonnull Frame<ReValue>[] frames,
	                                                        @Nonnull List<TryCatchBlockNode> tryCatches) {
		List<TryCatchBlockNode> kept = new ArrayList<>(tryCatches.size());
		for (TryCatchBlockNode tryCatch : tryCatches)
			if (canCatchBeUsed(instructions, frames, tryCatch))
				kept.add(tryCatch);
		return kept;
	}

	/**
	 * @param instructions
	 * 		Method instructions.
	 * @param frames
	 * 		Method stack frames.
	 * @param tryCatch
	 * 		Try-catch entry to inspect.
	 *
	 * @return {@code true} when the try-catch has a reachable protected instruction that may match it.
	 */
	private boolean canCatchBeUsed(@Nonnull InsnList instructions,
	                               @Nonnull Frame<ReValue>[] frames,
	                               @Nonnull TryCatchBlockNode tryCatch) {
		// If the catch type is a type defined in the workspace, but never thrown in the workspace,
		// then it can't be caught at runtime, and we can remove the try-catch block.
		String catchType = tryCatch.type;
		if (catchType != null && isWorkspaceExceptionNeverThrown(catchType))
			return false;

		int start = codeBoundaryIndex(instructions, tryCatch.start);
		int end = codeBoundaryIndex(instructions, tryCatch.end);
		int handler = codeBoundaryIndex(instructions, tryCatch.handler);

		// If the start and end are the same, or the start is beyond the end,
		// then there are no instructions protected by this try-catch, so it can't be used.
		if (start >= end)
			return false;

		// Determine which instructions in the protected range are reachable by normal control-flow (ignoring exception edges).
		boolean[] visited = computeVisitedInstructions(instructions, frames, start, end);

		// Check each instruction in the protected range to see if any of them can
		// throw an exception that would be caught by this try-catch block.
		for (int i = start; i < end; i++) {
			// not reachable by normal flow within the protected range
			if (!visited[i])
				continue;

			// If there is no frame for this instruction, it means the instruction is unreachable, so we can skip it.
			Frame<ReValue> frame = i < frames.length ? frames[i] : null;
			if (frame == null)
				continue;

			// If the catch type is null, we check for any exception throwing potential.
			// Otherwise, we only check for the handler's caught type.
			AbstractInsnNode insn = instructions.get(i);
			if (catchType == null) {
				if (canInsnThrowAnyException(insn, frame))
					return true;
			} else if (canInsnThrowCaughtException(insn, frame, catchType)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Compute which instructions in the protected range of a try-catch block
	 * are reachable by normal control-flow <i>(ignoring exception edges)</i>.
	 * <p>
	 * This is necessary for some edge cases where the protected range may
	 * include instructions that are not actually reachable without an exception being thrown.
	 * For example, consider the following code snippet:
	 * <pre>{@code
	 * .method public static example ()V {
	 *     exceptions: {
	 *         { A, C, B, Ljava/lang/RuntimeException; }
	 *      },
	 *     code: {
	 *     A:
	 *         // try-start - protected by B and nothing in here can throw RuntimeException
	 *         goto C
	 *     B:
	 *         // try-handler - but also inside the range A-C
	 *         //               we should not consider any instruction here as reachable by normal flow
	 *         dup
	 *         invokevirtual java/lang/RuntimeException.printStackTrace ()V
	 *         checkcast java/lang/Throwable
	 *         athrow
	 *     C:
	 *         // try-end
	 *         goto END
	 *     END:
	 *         return
	 *     }
	 * }
	 * }</pre>
	 *
	 * @param instructions
	 * 		Method instructions.
	 * @param frames
	 * 		Method stack frames.
	 * @param start
	 * 		Protected range start.
	 * @param end
	 * 		Protected range end.
	 *
	 * @return Boolean array of the same length as instructions,
	 * where each index is the visited state within the protected range.
	 */
	private static boolean[] computeVisitedInstructions(@Nonnull InsnList instructions,
	                                                    @Nonnull Frame<ReValue>[] frames,
	                                                    int start, int end) {
		// Build normal control-flow adjacency using the shared helper (no exception edges).
		Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectMap<>();
		Int2ObjectMap<List<Integer>> predecessorMap = new Int2ObjectMap<>();

		// Wrap instructions into a temporary MethodNode so populateFlowMaps can operate.
		MethodNode temp = new MethodNode();
		temp.instructions = instructions;
		temp.tryCatchBlocks = Collections.emptyList();

		// Populate flow maps without exception edges.
		AsmInsnUtil.populateFlowMaps(temp, successorMap, predecessorMap, false);

		// Determine entry nodes into the [start, end) range:
		// any node in range that has a predecessor outside the range, or the range start itself.
		int size = instructions.size();
		Deque<Integer> queue = new ArrayDeque<>();
		boolean[] visited = new boolean[size];
		for (int i = start; i < end && i < size; i++) {
			boolean hasOutsidePredecessor = false;
			for (int predecessor : predecessorMap.getOrDefault(i, Collections.emptyList())) {
				if (predecessor < start || predecessor >= end) {
					hasOutsidePredecessor = true;
					break;
				}
			}
			if (hasOutsidePredecessor || i == start) {
				queue.add(i);
				visited[i] = true;
			}
		}

		// If we found no entry but the start instruction is reachable according to frames, include it.
		if (queue.isEmpty() && start < frames.length && frames[start] != null) {
			queue.add(start);
			visited[start] = true;
		}

		// BFS within the protected range following normal control-flow only.
		while (!queue.isEmpty()) {
			int cur = queue.removeFirst();
			for (int to : successorMap.getOrDefault(cur, Collections.emptyList())) {
				if (to >= start && to < end && !visited[to]) {
					visited[to] = true;
					queue.addLast(to);
				}
			}
		}

		return visited;
	}

	/**
	 * @param tryCatchType
	 * 		Catch type from the exception table.
	 *
	 * @return {@code true} when the catch type belongs to the primary resource and is never thrown there.
	 */
	private boolean isWorkspaceExceptionNeverThrown(@Nonnull String tryCatchType) {
		InheritanceVertex vertex = inheritanceGraph.getVertex(tryCatchType);
		if (vertex == null || vertex.isLibraryVertex() || vertex.isModule())
			return false;
		return !exceptionCollector.getThrownExceptions().contains(tryCatchType);
	}

	/**
	 * @param insn
	 * 		Instruction to inspect.
	 * @param frame
	 * 		Stack frame before the instruction.
	 *
	 * @return {@code true} when the instruction may throw any exception relevant to this transformer.
	 */
	private boolean canInsnThrowAnyException(@Nonnull AbstractInsnNode insn, @Nonnull Frame<ReValue> frame) {
		// Most method calls can throw exceptions.
		// Since we are looking for *any* potential exception, we can just assume all method calls can throw.
		if (insn instanceof MethodInsnNode)
			return true;

		return switch (insn.getOpcode()) {
			case ATHROW, FDIV, FREM, DDIV, DREM -> true;
			case ARRAYLENGTH, MONITORENTER, GETFIELD -> isReferencePossiblyNull(peekStack(frame, 0));
			case PUTFIELD -> isReferencePossiblyNull(peekStack(frame, 1));
			case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD ->
					canArrayAccessThrow(peekStack(frame, 1), peekStack(frame, 0));
			case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE ->
					canArrayStoreThrow(insn.getOpcode(), peekStack(frame, 2), peekStack(frame, 1), peekStack(frame, 0));
			case IDIV, IREM, LDIV, LREM -> isZeroDivisorPossible(peekStack(frame, 0));
			case NEWARRAY, ANEWARRAY -> isNegativeSizePossible(peekStack(frame, 0));
			case MULTIANEWARRAY -> isNegativeMultiArraySizePossible((MultiANewArrayInsnNode) insn, frame);
			case CHECKCAST -> canCheckCastThrow((TypeInsnNode) insn, peekStack(frame, 0));
			case MONITOREXIT ->
					isReferencePossiblyNull(peekStack(frame, 0)) || !isReferenceKnownNull(peekStack(frame, 0));
			default -> false;
		};
	}

	/**
	 * @param insn
	 * 		Instruction to inspect.
	 * @param frame
	 * 		Stack frame before the instruction.
	 * @param catchType
	 * 		Caught exception type.
	 *
	 * @return {@code true} when the instruction may throw an exception assignable to the catch type.
	 */
	private boolean canInsnThrowCaughtException(@Nonnull AbstractInsnNode insn, @Nonnull Frame<ReValue> frame, @Nonnull String catchType) {
		// While we may be able to generalize that some methods are unlikely to throw certain exceptions,
		// it's safer to assume that all method calls can throw something that would be caught by the catch block.
		// - If we wanted to add a heuristic here, we would check if the method's declaring class is a library class
		//   and if the exception type is defined in the workspace as a checked exception.
		// - Library methods are unlikely to throw user-defined exceptions, especially checked ones.
		if (insn instanceof MethodInsnNode min)
			return true;

		return switch (insn.getOpcode()) {
			case ATHROW -> canAthrowThrow(catchType, peekStack(frame, 0));
			case GETFIELD, ARRAYLENGTH, MONITORENTER -> canThrowNullPointer(catchType, peekStack(frame, 0));
			case PUTFIELD -> canThrowNullPointer(catchType, peekStack(frame, 1));
			case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD ->
					canArrayAccessThrow(catchType, peekStack(frame, 1), peekStack(frame, 0));
			case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE ->
					canArrayStoreThrow(catchType, insn.getOpcode(), peekStack(frame, 2), peekStack(frame, 1), peekStack(frame, 0));
			case IDIV, IREM, LDIV, LREM, FDIV, FREM, DDIV, DREM ->
					canArithmeticThrow(catchType, insn.getOpcode(), peekStack(frame, 0));
			case NEWARRAY, ANEWARRAY ->
					isCaughtException(catchType, EX_NASE) && isNegativeSizePossible(peekStack(frame, 0));
			case MULTIANEWARRAY ->
					isCaughtException(catchType, EX_NASE) && isNegativeMultiArraySizePossible((MultiANewArrayInsnNode) insn, frame);
			case CHECKCAST ->
					isCaughtException(catchType, EX_CCE) && canCheckCastThrow((TypeInsnNode) insn, peekStack(frame, 0));
			case MONITOREXIT -> canMonitorExitThrow(catchType, peekStack(frame, 0));
			default -> false;
		};
	}

	/**
	 * @param arrayValue
	 * 		Array reference.
	 * @param indexValue
	 * 		Array index.
	 *
	 * @return {@code true} when an array read may throw NPE or AIOOBE.
	 */
	private static boolean canArrayAccessThrow(@Nullable ReValue arrayValue, @Nullable ReValue indexValue) {
		return isReferencePossiblyNull(arrayValue) || canArrayIndexThrow(arrayValue, indexValue);
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param arrayValue
	 * 		Array reference.
	 * @param indexValue
	 * 		Array index.
	 *
	 * @return {@code true} when an array read may throw an exception matched by the catch.
	 */
	private boolean canArrayAccessThrow(@Nonnull String catchType, @Nullable ReValue arrayValue, @Nullable ReValue indexValue) {
		return canThrowNullPointer(catchType, arrayValue) ||
				(isCaughtException(catchType, EX_AIOOBE) &&
						canArrayIndexThrow(arrayValue, indexValue));
	}

	/**
	 * @param opcode
	 * 		Array-store opcode.
	 * @param arrayValue
	 * 		Array reference.
	 * @param indexValue
	 * 		Array index.
	 * @param storedValue
	 * 		Value being stored.
	 *
	 * @return {@code true} when an array store may throw any relevant exception.
	 */
	private boolean canArrayStoreThrow(int opcode, @Nullable ReValue arrayValue, @Nullable ReValue indexValue, @Nullable ReValue storedValue) {
		if (isReferencePossiblyNull(arrayValue) || canArrayIndexThrow(arrayValue, indexValue))
			return true;
		return opcode == AASTORE && canArrayStoreTypeThrow(arrayValue, storedValue);
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param opcode
	 * 		Array-store opcode.
	 * @param arrayValue
	 * 		Array reference.
	 * @param indexValue
	 * 		Array index.
	 * @param storedValue
	 * 		Value being stored.
	 *
	 * @return {@code true} when an array store may throw an exception matched by the catch.
	 */
	private boolean canArrayStoreThrow(@Nonnull String catchType, int opcode, @Nullable ReValue arrayValue, @Nullable ReValue indexValue, @Nullable ReValue storedValue) {
		if (canThrowNullPointer(catchType, arrayValue))
			return true;
		if (isCaughtException(catchType, EX_AIOOBE) && canArrayIndexThrow(arrayValue, indexValue))
			return true;
		return opcode == AASTORE
				&& isCaughtException(catchType, EX_ASE)
				&& canArrayStoreTypeThrow(arrayValue, storedValue);
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param opcode
	 * 		Arithmetic opcode.
	 * @param divisor
	 * 		Divisor or remainder operand.
	 *
	 * @return {@code true} when the arithmetic instruction may throw an exception matched by the catch.
	 */
	private boolean canArithmeticThrow(@Nonnull String catchType, int opcode, @Nullable ReValue divisor) {
		// Skip if the catch doesn't even catch ArithmeticException.
		if (!isCaughtException(catchType, EX_AE))
			return false;

		// For floating point division and remainder, the JVM does not throw an exception on division by zero.
		// - 1F / 0F -> Infinity
		// - 1F % 0F -> NaN
		if (opcode == FDIV || opcode == FREM || opcode == DDIV || opcode == DREM)
			return false;

		// For integer division and remainder, the JVM throws ArithmeticException on division by zero.
		return isZeroDivisorPossible(divisor);
	}

	/**
	 * @param value
	 * 		Divisor value.
	 *
	 * @return {@code true} when the divisor is unknown or zero.
	 */
	private static boolean isZeroDivisorPossible(@Nullable ReValue value) {
		if (value instanceof IntValue intValue)
			return intValue.value().isEmpty() || intValue.value().getAsInt() == 0;
		if (value instanceof LongValue longValue)
			return longValue.value().isEmpty() || longValue.value().getAsLong() == 0L;
		return true;
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param objectValue
	 * 		Thrown object value.
	 *
	 * @return {@code true} when {@code athrow} may be matched by the catch.
	 */
	private boolean canAthrowThrow(@Nonnull String catchType, @Nullable ReValue objectValue) {
		if (objectValue instanceof ObjectValue object) {
			// 'athrow' with a null reference will throw NPE, so we need to check for that possibility first.
			if (object.isNull())
				return isCaughtException(catchType, EX_NPE);

			// If the reference is not null, but we don't know its type, we have to assume it could be anything,
			// including a type that would be caught by the catch block.
			if (!object.isNotNull() && isCaughtException(catchType, EX_NPE))
				return true;

			// If we know the reference is not null, and we know its type,
			// we can check if that type could be caught by the catch block.
			Type valueType = object.type();
			if (valueType.getSort() != Type.OBJECT)
				return true;
			return canReferenceRuntimeTypeMatch(valueType, catchType);
		}

		// If we don't know anything about the reference, lets just assume it can throw.
		return true;
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param monitorValue
	 * 		Monitor reference.
	 *
	 * @return {@code true} when {@code monitorexit} may throw an exception matched by the catch.
	 */
	private boolean canMonitorExitThrow(@Nonnull String catchType, @Nullable ReValue monitorValue) {
		return canThrowNullPointer(catchType, monitorValue)
				|| (isCaughtException(catchType, EX_IMSE)
				&& !isReferenceKnownNull(monitorValue));
	}

	/**
	 * @param cast
	 * 		Cast instruction.
	 * @param value
	 * 		Value being cast.
	 *
	 * @return {@code true} when the cast may throw {@link ClassCastException}.
	 */
	private boolean canCheckCastThrow(@Nonnull TypeInsnNode cast, @Nullable ReValue value) {
		// Null can be cast to any type.
		if (isReferenceKnownNull(value))
			return false;

		// If we don't know what type the value is, we can't safely assume the cast will succeed.
		Type sourceType = value == null ? null : value.type();
		if (sourceType == null)
			return true;

		// Sanity check to ensure the value is actually a reference type, since only reference types can be cast.
		int sourceSort = sourceType.getSort();
		if (sourceSort != Type.OBJECT && sourceSort != Type.ARRAY)
			return false;

		// Finally check if the target type is assignable from the source type.
		Type targetType = Type.getObjectType(cast.desc);
		return !isAssignable(targetType, sourceType);
	}

	/**
	 * @param arrayValue
	 * 		Array reference.
	 * @param storedValue
	 * 		Value being stored.
	 *
	 * @return {@code true} when {@code aastore} may throw {@link ArrayStoreException}.
	 */
	private boolean canArrayStoreTypeThrow(@Nullable ReValue arrayValue, @Nullable ReValue storedValue) {
		if (isReferenceKnownNull(arrayValue))
			return false;
		if (storedValue instanceof ObjectValue object && object.isNull())
			return false;
		if (!(arrayValue instanceof ArrayValue array))
			return true;

		Type arrayType = array.type();
		if (arrayType.getSort() != Type.ARRAY)
			return true;

		Type componentType = Types.undimension(arrayType);
		Type valueType = storedValue == null ? null : storedValue.type();
		if (valueType == null)
			return true;

		return !isAssignable(componentType, valueType);
	}

	/**
	 * @param targetType
	 * 		Target type.
	 * @param valueType
	 * 		Value type.
	 *
	 * @return {@code true} when the value type is assignable to the target type.
	 */
	private boolean isAssignable(@Nonnull Type targetType, @Nonnull Type valueType) {
		// Base case, same type.
		if (targetType.equals(valueType))
			return true;

		// Arrays can only be assigned to Object, and Object can be assigned from any array.
		int targetSort = targetType.getSort();
		int valueSort = valueType.getSort();
		if (targetSort == Type.ARRAY || valueSort == Type.ARRAY)
			return targetSort == Type.OBJECT && Types.OBJECT_TYPE.equals(targetType);

		// For non-object types, these are not assignable between one another.
		// This method is used strictly for checking casts and object type operations.
		//
		// If either type is not an object, then the cast is only valid if both types are the same primitive type,
		// which is already handled by the equality check above.
		if (targetSort != Type.OBJECT || valueSort != Type.OBJECT)
			return false;

		// Check inheritance graph for assignability of reference types.
		return inheritanceGraph.isAssignableFrom(targetType.getInternalName(), valueType.getInternalName());
	}

	/**
	 * @param value
	 * 		Array size value.
	 *
	 * @return {@code true} when the size is unknown or negative.
	 */
	private static boolean isNegativeSizePossible(@Nullable ReValue value) {
		if (value instanceof IntValue intValue && intValue.value().isPresent())
			return intValue.value().getAsInt() < 0;
		return true;
	}

	/**
	 * @param multiArray
	 * 		Multi-array instruction.
	 * @param frame
	 * 		Stack frame before the instruction.
	 *
	 * @return {@code true} when any dimension is unknown or negative.
	 */
	private static boolean isNegativeMultiArraySizePossible(@Nonnull MultiANewArrayInsnNode multiArray, @Nonnull Frame<ReValue> frame) {
		for (int i = 0; i < multiArray.dims; i++)
			if (isNegativeSizePossible(peekStack(frame, i)))
				return true;
		return false;
	}

	/**
	 * @param arrayValue
	 * 		Array reference.
	 * @param indexValue
	 * 		Array index.
	 *
	 * @return {@code true} when the index is unknown or outside known bounds of a non-null array.
	 */
	private static boolean canArrayIndexThrow(@Nullable ReValue arrayValue, @Nullable ReValue indexValue) {
		// This would be a NPE instead.
		if (isReferenceKnownNull(arrayValue))
			return false;

		// If we don't know the index, or we don't know the array length, then we have to assume the index could be out of bounds.
		if (!(indexValue instanceof IntValue index) || index.value().isEmpty())
			return true;
		if (!(arrayValue instanceof ArrayValue array))
			return true;
		OptionalInt length = array.getFirstDimensionLength();
		if (length.isEmpty())
			return true;

		// Check if the index is outside the bounds of the array length.
		int literalIndex = index.value().getAsInt();
		return literalIndex < 0 || literalIndex >= length.getAsInt();
	}

	/**
	 * @param catchType
	 * 		Caught exception type.
	 * @param value
	 * 		Reference value.
	 *
	 * @return {@code true} when the reference may trigger a caught {@link NullPointerException}.
	 */
	private boolean canThrowNullPointer(@Nonnull String catchType, @Nullable ReValue value) {
		return isCaughtException(catchType, EX_NPE) && isReferencePossiblyNull(value);
	}

	/**
	 * @param type
	 * 		Thrown exception type.
	 * @param catchType
	 * 		Caught exception type.
	 *
	 * @return {@code true} when the runtime reference may still match the catch.
	 */
	private boolean canReferenceRuntimeTypeMatch(@Nonnull Type type, @Nonnull String catchType) {
		if (type.getSort() != Type.OBJECT)
			return false;

		String typeName = type.getInternalName();
		return isCaughtException(catchType, typeName)
				|| inheritanceGraph.isAssignableFrom(typeName, catchType);
	}

	/**
	 * @param catchType
	 * 		Catch type.
	 * @param thrownType
	 * 		Thrown type.
	 *
	 * @return {@code true} when the catch type can handle the thrown type.
	 */
	private boolean isCaughtException(@Nonnull String catchType, @Nonnull String thrownType) {
		return catchType.equals(thrownType)
				|| inheritanceGraph.isAssignableFrom(catchType, thrownType);
	}

	/**
	 * @param broaderType
	 * 		Possibly broader catch type.
	 * @param narrowerType
	 * 		Possibly narrower catch type.
	 *
	 * @return {@code true} when the first type catches the same or broader set of exceptions.
	 */
	private boolean catchesSameOrBroaderException(@Nullable String broaderType, @Nullable String narrowerType) {
		if (broaderType == null)
			return true;
		if (narrowerType == null)
			return false;
		return broaderType.equals(narrowerType)
				|| inheritanceGraph.isAssignableFrom(broaderType, narrowerType);
	}

	/**
	 * @param value
	 * 		Reference candidate.
	 *
	 * @return {@code true} when the value may be {@code null}.
	 */
	private static boolean isReferencePossiblyNull(@Nullable ReValue value) {
		return !(value instanceof ObjectValue object) || !object.isNotNull();
	}

	/**
	 * @param value
	 * 		Reference candidate.
	 *
	 * @return {@code true} when the value is definitely {@code null}.
	 */
	private static boolean isReferenceKnownNull(@Nullable ReValue value) {
		return value instanceof ObjectValue object && object.isNull();
	}

	/**
	 * Reads a stack value relative to the stack top.
	 *
	 * @param frame
	 * 		Method frame before an instruction.
	 * @param offsetFromTop
	 * 		Offset from the stack top.
	 *
	 * @return Stack value, or {@code null} when unavailable.
	 */
	@Nullable
	private static ReValue peekStack(@Nonnull Frame<ReValue> frame, int offsetFromTop) {
		int index = frame.getStackSize() - 1 - offsetFromTop;
		if (index < 0)
			return null;
		return frame.getStack(index);
	}

	/**
	 * @param instructions
	 * 		Method instructions.
	 * @param label
	 * 		Boundary label.
	 *
	 * @return Index of the first executable instruction at or after the label.
	 */
	private static int codeBoundaryIndex(@Nonnull InsnList instructions, @Nonnull LabelNode label) {
		AbstractInsnNode current = label;
		current = AsmInsnUtil.getNextInsn(current);
		return current == null ?
				instructions.size() :
				instructions.indexOf(current);
	}

	/**
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatch
	 * 		Try-catch entry.
	 *
	 * @return Snapshot of the try-catch's effective range and handler.
	 */
	@Nonnull
	private static TryCatchState snapshotState(@Nonnull InsnList instructions, @Nonnull TryCatchBlockNode tryCatch) {
		return new TryCatchState(codeBoundaryIndex(instructions, tryCatch.start),
				codeBoundaryIndex(instructions, tryCatch.end),
				codeBoundaryIndex(instructions, tryCatch.handler),
				tryCatch.type);
	}

	/**
	 * @param instructions
	 * 		Method instructions.
	 * @param tryCatches
	 * 		Try-catch entries.
	 *
	 * @return Snapshots of all entries in declaration order.
	 */
	@Nonnull
	private static List<TryCatchState> snapshotStates(@Nonnull InsnList instructions, @Nonnull List<TryCatchBlockNode> tryCatches) {
		List<TryCatchState> states = new ArrayList<>(tryCatches.size());
		for (TryCatchBlockNode tryCatch : tryCatches)
			states.add(snapshotState(instructions, tryCatch));
		return states;
	}

	/**
	 * Simplified try-catch signature used for hashing/comparisons without {@link LabelNode} references.
	 *
	 * @param start
	 * 		Protected range start.
	 * @param end
	 * 		Protected range end.
	 * @param handler
	 * 		Handler range start.
	 * @param type
	 * 		Handled exception type. Can be {@code null} for catch-all handlers.
	 */
	private record TryCatchState(int start, int end, int handler, @Nullable String type) {
		private boolean covers(@Nonnull TryCatchState other) {
			return start <= other.start && end >= other.end;
		}
	}

	/**
	 * Simplified range.
	 *
	 * @param start
	 * 		Protected range start.
	 * @param end
	 * 		Protected range end.
	 */
	private record TryRange(int start, int end) {}

	/**
	 * Collection of try catch blocks.
	 *
	 * @param blocks
	 * 		Wrapped list of blocks.
	 * @param seenTypes
	 * 		Observed types handled by the blocks.
	 */
	private record Handlers(@Nonnull List<TryCatchBlockNode> blocks, @Nonnull Set<String> seenTypes) {
		private Handlers() {
			this(new ArrayList<>(), new HashSet<>());
		}
	}
}
