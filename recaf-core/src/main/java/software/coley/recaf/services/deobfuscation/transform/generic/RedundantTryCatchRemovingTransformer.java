package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
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
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
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
	private static boolean REMOVE_JUNK_EXCEPTIONS;
	private static final String EX_NPE = "java/lang/NullPointerException";
	private static final String EX_ASE = "java/lang/ArrayStoreException";
	private static final String EX_AIOOBE = "java/lang/ArrayIndexOutOfBoundsException";
	private static final String EX_NASE = "java/lang/NegativeArraySizeException";
	private static final String EX_IMSE = "java/lang/IllegalMonitorStateException";
	private static final String EX_CCE = "java/lang/ClassCastException";
	private static final String EX_AE = "java/lang/ArithmeticException";
	private final Map<String, Handling> doesHandleCache = new HashMap<>();
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public RedundantTryCatchRemovingTransformer(@Nonnull WorkspaceManager workspaceManager,
	                                            @Nonnull InheritanceGraphService graphService) {
		this.workspaceManager = workspaceManager;
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
			if (method.instructions == null || method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty())
				continue;

			dirty |= pass0PruneIgnoredHandlers(context, node, method);
			dirty |= pass1PruneNeverThrown(context, workspace, node, method);
			dirty |= pass2PruneNeverThrowingOrDuplicate(context, node, method);
			dirty |= pass3CombineAdjacentTriesWithSameHandler(context, node, method);
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	/**
	 * Remove try-catch blocks that cannot possibly be utilized at runtime.
	 *
	 * @param context
	 * 		Transformer context.
	 * @param node
	 * 		Defining class.
	 * @param method
	 * 		Method to transform.
	 *
	 * @return {@code true} when one or more try-catch blocks have been removed.
	 *
	 * @throws TransformationException
	 * 		Thrown when dead code after transformation could not be pruned.
	 */
	private boolean pass0PruneIgnoredHandlers(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node, @Nonnull MethodNode method) throws TransformationException {
		// Given the following {start, end, handler, ex-type} blocks:
		//  { R, S, Q, * },
		//  { R, S, C, * },
		//  { R, S, S, Ljava/lang/ArrayIndexOutOfBoundsException; }
		// Only the first is going to be used.
		//  - It appears first, so it will be checked first by the JVM
		//  - Its range covers all possible instructions of the other two try blocks
		//  - Its handled type is more generic ("*" is catch-all)
		// See: https://github.com/openjdk/jdk21u/blob/master/src/hotspot/share/oops/method.cpp#L227
		//
		// Process:
		//  1. Collect try-catch handlers keyed by their range
		//  2. Prune handlers of narrower types in the collection
		//  3. Retain only remaining handlers in the collection
		List<TryCatchBlockNode> blocks = new ArrayList<>(method.tryCatchBlocks);
		Map<ThrowingRange, Handlers> handlersMap = new HashMap<>();
		for (TryCatchBlockNode block : blocks) {
			int start = AsmInsnUtil.indexOf(block.start);
			int end = AsmInsnUtil.indexOf(block.end);
			if (start < end) {
				ThrowingRange range = new ThrowingRange(start, end);
				handlersMap.computeIfAbsent(range, r -> new Handlers()).addBlock(block);
			}
		}
		for (Handlers handlers : handlersMap.values())
			handlers.prune(inheritanceGraph);
		Set<TryCatchBlockNode> allHandlers = handlersMap.values()
				.stream()
				.flatMap(handlers -> handlers.blocks.stream())
				.collect(Collectors.toSet());
		if (method.tryCatchBlocks.retainAll(allHandlers)) {
			// Removing handlers can mean blocks starting with an expected 'Throwable' on the stack are now invalid.
			// These should be dead code though, so if we prune code that isn't visitable these should go away.
			context.pruneDeadCode(node, method);
			return true;
		}
		return false;
	}

	/**
	 * Remove try-catch blocks that have handle exception types that are defined in the workspace
	 * but never actually constructed and thrown.
	 *
	 * @param context
	 * 		Transformer context.
	 * @param workspace
	 * 		Workspace the class is from.
	 * @param node
	 * 		Defining class.
	 * @param method
	 * 		Method to transform.
	 *
	 * @return {@code true} when one or more try-catch blocks have been removed.
	 *
	 * @throws TransformationException
	 * 		Thrown when the {@link ExceptionCollectionTransformer} cannot be found in the transformer context,
	 * 		or when dead code couldn't be pruned.
	 */
	private boolean pass1PruneNeverThrown(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace, @Nonnull ClassNode node, @Nonnull MethodNode method) throws TransformationException {
		ExceptionCollectionTransformer exceptions = context.getJvmTransformer(ExceptionCollectionTransformer.class);

		// Collect which blocks are candidates for removal.
		Set<TryCatchBlockNode> handleBlocksToRemove = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<TryCatchBlockNode> probablyNotRemovableBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			String type = block.type;
			if (type == null)
				continue;

			// Check if exception type is defined in our workspace but never actually thrown anywhere.
			final boolean isExceptionDefinedInWorkspace = workspace.findClass(false, type) != null;
			if (isExceptionDefinedInWorkspace && !exceptions.getThrownExceptions().contains(type)) {
				// We should be able to remove this try-catch, so long as another block does not also point to it.
				handleBlocksToRemove.add(block);

				// Mark this type for removal.
				// We know its defined in the workspace and never thrown so its must be useless.
				if (REMOVE_JUNK_EXCEPTIONS) context.markClassForRemoval(type);
			} else {
				probablyNotRemovableBlocks.add(block);
			}
		}

		// Remove blocks and make the handlers no-op.
		boolean removedAny = false;
		Type returnType = Type.getReturnType(method.desc);
		for (TryCatchBlockNode block : new ArrayList<>(method.tryCatchBlocks)) {
			LabelNode handler = block.handler;
			if (handleBlocksToRemove.contains(block) && !probablyNotRemovableBlocks.contains(block)) {
				if (method.tryCatchBlocks.stream().filter(b -> b.handler == handler).count() == 1) {
					// Push 'null' to stack since we will be changing the behavior at this location to not be
					// an exception handler block. The implicitly pushed value on the stack will be gone.
					// A simple 'null' replacement should suffice to keep the method stack verifiable in the
					// case there is some weird control flow here. This is mainly to keep ASM happy.
					method.instructions.insert(handler, new InsnNode(ACONST_NULL));

					// Make the handler block dead code by inserting a return.
					int returnOpcode = AsmInsnUtil.getReturnOpcode(returnType);
					switch (returnOpcode) {
						case RETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
						}
						case IRETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
							method.instructions.insert(handler, new InsnNode(ICONST_0));
						}
						case FRETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
							method.instructions.insert(handler, new InsnNode(FCONST_0));
						}
						case LRETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
							method.instructions.insert(handler, new InsnNode(LCONST_0));
						}
						case DRETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
							method.instructions.insert(handler, new InsnNode(DCONST_0));
						}
						case ARETURN -> {
							method.instructions.insert(handler, new InsnNode(returnOpcode));
							method.instructions.insert(handler, new InsnNode(ACONST_NULL));
						}
					}
				}

				// Remove the try-catch block from the method.
				method.tryCatchBlocks.remove(block);

				// Remove the handler from the removal candidates set so that we don't
				// try to remove the handler block multiple times.
				handleBlocksToRemove.remove(block);
				removedAny = true;
			}
		}

		// Because the control flow has changes we will want to remove code that is no longer accessible (dead catch handlers)
		if (removedAny)
			context.pruneDeadCode(node, method);

		return removedAny;
	}

	/**
	 * Remove try-catch blocks that are duplicates <i>(Same range/handler)</i> or are impossible to be handled
	 * <i>(example: IllegalMonitorStateException when no monitors are utilized)</i> for the following types:
	 * <ul>
	 *     <li>{@link #EX_NPE java/lang/NullPointerException}</li>
	 *     <li>{@link #EX_ASE java/lang/ArrayStoreException}</li>
	 *     <li>{@link #EX_AIOOBE java/lang/ArrayIndexOutOfBoundsException}</li>
	 *     <li>{@link #EX_NASE java/lang/NegativeArraySizeException}</li>
	 *     <li>{@link #EX_IMSE java/lang/IllegalMonitorStateException}</li>
	 *     <li>{@link #EX_CCE java/lang/ClassCastException}</li>
	 *     <li>{@link #EX_AE java/lang/ArithmeticException}</li>
	 * </ul>
	 *
	 * @param context
	 * 		Transformer context.
	 * @param node
	 * 		Defining class.
	 * @param method
	 * 		Method to transform.
	 *
	 * @return {@code true} when one or more try-catch blocks have been removed.
	 *
	 * @throws TransformationException
	 * 		Thrown when code cannot be analyzed <i>(Needed for certain checks)</i>.
	 */
	private boolean pass2PruneNeverThrowingOrDuplicate(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node, @Nonnull MethodNode method) throws TransformationException {
		InsnList instructions = method.instructions;
		List<TryCatchBlockNode> tryCatchBlocks = method.tryCatchBlocks;
		Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
		Map<TryCatchBlockNode, ThrowingRange> blockThrowingRanges = new IdentityHashMap<>();
		Set<TryCatchBlockNode> redundantBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
		for (TryCatchBlockNode tryCatch : tryCatchBlocks) {
			insnLoop:
			{
				int start = instructions.indexOf(tryCatch.start);
				int end = instructions.indexOf(tryCatch.end);

				int firstThrowable = Integer.MAX_VALUE;
				int lastThrowable = Integer.MIN_VALUE;

				for (int i = start; i < end; i++) {
					// Ensure the instruction is actually used. If there is no associated frame
					// then it is dead code and shouldn't be reachable. This means we don't
					// need to treat it as a potential throwing behavior participant.
					Frame<ReValue> frame = frames[i];
					if (frame == null)
						continue;

					ThrowingBehavior behavior = ThrowingBehavior.NONE;
					AbstractInsnNode insn = instructions.get(i);
					int op = insn.getOpcode();
					switch (op) {
						case IALOAD:
						case DALOAD:
						case FALOAD:
						case LALOAD:
						case SALOAD:
						case BALOAD:
						case AALOAD: {
							ReValue indexValue = frame.getStack(frame.getStackSize() - 1);
							ReValue arrayValue = frame.getStack(frame.getStackSize() - 2);

							// Check if:
							// - The array is null
							// - The index is beyond the array's bounds
							if (arrayValue instanceof ArrayValue av) {
								if (av.isNull() && doesHandleException(tryCatch, EX_ASE)) {
									behavior = ThrowingBehavior.ALWAYS;
									break;
								}
								if (indexValue instanceof IntValue iv && iv.hasKnownValue()) {
									if (iv.isLessThan(0) && doesHandleException(tryCatch, EX_AIOOBE)) {
										behavior = ThrowingBehavior.ALWAYS;
										break;
									}
									OptionalInt firstDimensionLength = av.getFirstDimensionLength();
									if (firstDimensionLength.isPresent()) {
										if (iv.isGreaterThanOrEqual(firstDimensionLength.getAsInt()) && doesHandleException(tryCatch, EX_AIOOBE)) {
											behavior = ThrowingBehavior.ALWAYS;
											break;
										}
									}
								}
							}

							// Otherwise we'll keep the array exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_ASE, EX_AIOOBE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case IASTORE:
						case DASTORE:
						case BASTORE:
						case FASTORE:
						case LASTORE:
						case SASTORE:
						case AASTORE: {
							ReValue toStoreValue = frame.getStack(frame.getStackSize() - 1);
							ReValue indexValue = frame.getStack(frame.getStackSize() - 2);
							ReValue arrayValue = frame.getStack(frame.getStackSize() - 3);

							// Check if:
							// - The array is null
							// - The index is beyond the array's bounds
							// - The value to store does not match the array's element type
							if (arrayValue instanceof ArrayValue av) {
								if (av.isNull() && doesHandleException(tryCatch, EX_ASE)) {
									behavior = ThrowingBehavior.ALWAYS;
									break;
								}
								if (indexValue instanceof IntValue iv && iv.hasKnownValue()) {
									if (iv.isLessThan(0) && doesHandleException(tryCatch, EX_AIOOBE)) {
										behavior = ThrowingBehavior.ALWAYS;
										break;
									}
									OptionalInt firstDimensionLength = av.getFirstDimensionLength();
									if (firstDimensionLength.isPresent()) {
										if (iv.isGreaterThanOrEqual(firstDimensionLength.getAsInt()) && doesHandleException(tryCatch, EX_AIOOBE)) {
											behavior = ThrowingBehavior.ALWAYS;
											break;
										}
									}
								}
								Type toStoreType = toStoreValue.type();
								if (toStoreType != null) {
									boolean avPrim = Types.isPrimitive(av.elementType());
									boolean tsPrim = Types.isPrimitive(toStoreType);
									if (avPrim && tsPrim) {
										// If the primitive types do not match we will throw an ArrayStoreException.
										// Values that are thinner than integer are allowed to fit into arrays of wider types though.
										int avSort = av.elementType().getSort();
										int tsSort = toStoreType.getSort();
										if (avSort != tsSort && doesHandleException(tryCatch, EX_ASE)) {
											behavior = ThrowingBehavior.ALWAYS;
											break;
										}
									} else if (!avPrim && !tsPrim) {
										// If the array element and to-store value types are both object types, then the to-store
										// value must be assignable to the array's element type.
										if (!inheritanceGraph.isAssignableFrom(av.elementType().getInternalName(), toStoreType.getInternalName())
												&& doesHandleException(tryCatch, EX_ASE)) {
											behavior = ThrowingBehavior.ALWAYS;
											break;
										}
									} else if (doesHandleException(tryCatch, EX_ASE)) {
										// If the array element and to-store value types are not both primitives, or both objects
										// then we will always throw an ArrayStoreException.
										behavior = ThrowingBehavior.ALWAYS;
										break;
									}
								}
							}

							// Otherwise we'll keep the array exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_ASE, EX_AIOOBE, EX_ASE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case NEWARRAY:
						case ANEWARRAY: {
							ReValue arraySize = frame.getStack(frame.getStackSize() - 1);

							// New array cannot be negative.
							if (arraySize instanceof IntValue iv && iv.isLessThan(0) && doesHandleException(tryCatch, EX_NASE)) {
								behavior = ThrowingBehavior.ALWAYS;
								break;
							}

							// Otherwise we'll keep the array exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NASE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case MULTIANEWARRAY: {
							MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;

							// New array cannot be negative.
							for (int j = 1; j <= manain.dims; j++) {
								ReValue arraySize = frame.getStack(frame.getStackSize() - j);
								if (arraySize instanceof IntValue iv && iv.isLessThan(0) && doesHandleException(tryCatch, EX_NASE))
									behavior = ThrowingBehavior.ALWAYS;
							}
							if (behavior == ThrowingBehavior.ALWAYS)
								break;

							// Otherwise we'll keep the array exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NASE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case ARRAYLENGTH:
						case MONITORENTER: {
							// Check if the array/monitor is known to be null.
							ReValue arrayOrMonitorValue = frame.getStack(frame.getStackSize() - 1);
							if (arrayOrMonitorValue instanceof ObjectValue ov && ov.isNull() && doesHandleException(tryCatch, EX_NPE)) {
								behavior = ThrowingBehavior.ALWAYS;
								break;
							}

							// Otherwise we'll keep the exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NPE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case MONITOREXIT: {
							// Check if the monitor is known to be null.
							ReValue monitorValue = frame.getStack(frame.getStackSize() - 1);
							if (monitorValue instanceof ObjectValue ov && ov.isNull() && doesHandleException(tryCatch, EX_NPE)) {
								behavior = ThrowingBehavior.ALWAYS;
								break;
							}

							// Otherwise we'll keep the exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NPE, EX_IMSE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case RETURN:
						case IRETURN:
						case DRETURN:
						case FRETURN:
						case LRETURN:
						case ARETURN: {
							Handling handling = getHandlingForExceptions(tryCatch, EX_IMSE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case CHECKCAST: {
							// Check if cast is guaranteed to fail
							TypeInsnNode tin = (TypeInsnNode) insn;
							ReValue value = frame.getStack(frame.getStackSize() - 1);
							if (value instanceof ObjectValue ov) {
								Type stackType = ov.type();
								if (stackType != null
										&& !inheritanceGraph.isAssignableFrom(tin.desc, stackType.getInternalName())
										&& doesHandleException(tryCatch, EX_CCE)) {
									behavior = ThrowingBehavior.ALWAYS;
									break;
								}
							}

							// Otherwise we'll keep the exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_CCE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case GETFIELD: {
							// Check if the field owner instance is known to be null.
							ReValue owner = frame.getStack(frame.getStackSize() - 1);
							if (owner instanceof ObjectValue ov && ov.isNull() && doesHandleException(tryCatch, EX_NPE)) {
								behavior = ThrowingBehavior.ALWAYS;
								break;
							}

							// Otherwise we'll keep the exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NPE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case PUTFIELD: {
							// Check if the field owner instance is known to be null.
							ReValue owner = frame.getStack(frame.getStackSize() - 2);
							if (owner instanceof ObjectValue ov && ov.isNull() && doesHandleException(tryCatch, EX_NPE)) {
								behavior = ThrowingBehavior.ALWAYS;
								break;
							}

							// Otherwise we'll keep the exceptions as being "possible" if the block has a compatible type.
							Handling handling = getHandlingForExceptions(tryCatch, EX_NPE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case IDIV:
						case FDIV:
						case DDIV:
						case LDIV:
						case IREM:
						case FREM:
						case DREM:
						case LREM: {
							// Check if "[v1, v2] --> v1/v2" will throw an error (divide by zero) where v2 is the stack top.
							// If we know v2 is non-zero we're safe from arithmetic exceptions.
							ReValue divisor = frame.getStack(frame.getStackSize() - 1);
							if (divisor.hasKnownValue() && doesHandleException(tryCatch, EX_AE)) {
								if (((divisor instanceof IntValue iv && iv.isEqualTo(0))
										|| (divisor instanceof LongValue lv && lv.isEqualTo(0))
										|| (divisor instanceof FloatValue fv && fv.isEqualTo(0))
										|| (divisor instanceof DoubleValue dv && dv.isEqualTo(0)))) {
									behavior = ThrowingBehavior.ALWAYS;
									break;
								} else {
									// Value is anything other than zero, so we're fine.
									break;
								}
							}

							Handling handling = getHandlingForExceptions(tryCatch, EX_AE);
							if (handling == Handling.CAUGHT) {
								behavior = ThrowingBehavior.POSSIBLE;
							} else if (handling == Handling.UNKNOWN) {
								break insnLoop;
							}
							break;
						}
						case INVOKEDYNAMIC:
						case INVOKEINTERFACE:
						case INVOKESPECIAL:
						case INVOKESTATIC:
						case INVOKEVIRTUAL: {
							// Unless we do a full chain analysis of methods we can't tell if they'll throw an
							// exception somewhere down the call stack. So for these cases we'll just say an
							// exception is always possible.
							behavior = ThrowingBehavior.POSSIBLE;
							break;
						}
						case ATHROW: {
							// Check for specific kinds of exceptions.
							ReValue top = frame.getStack(frame.getStackSize() - 1);
							if (top instanceof ObjectValue ov) {
								// Check for NPE handling.
								if (ov.isNull() && doesHandleException(tryCatch, EX_NPE)) {
									behavior = ThrowingBehavior.ALWAYS;
									break;
								}

								// Check if top type is handled by catch handler.
								Type topType = ov.type();
								Handling handling = getHandlingForExceptions(tryCatch, topType.getInternalName());
								if (handling == Handling.CAUGHT) {
									behavior = ThrowingBehavior.ALWAYS;
								} else if (handling == Handling.UNKNOWN) {
									break insnLoop;
								}
								break;
							}
							break;
						}
					}

					if (behavior.canThrow()) {
						firstThrowable = Math.min(i, firstThrowable);
						lastThrowable = Math.max(i, lastThrowable);

						// TODO: For 'ALWAYS' cases it would be nice to replace the offending instructions
						//  with a direct GOTO to the handler block.
						//  - If the handler block does not use the exception, we could remove the assignment/pop of the exception
						//    - Otherwise we could make a dummy instance of the target exception (tricky since not all exceptions have default constructors)
					}
				}

				if (lastThrowable < 0) {
					// If we never observed any instructions that have the potential to throw, then we should remove this block.
					redundantBlocks.add(tryCatch);
				} else {
					// Track effective range for try-catch block.
					blockThrowingRanges.merge(tryCatch, new ThrowingRange(firstThrowable, lastThrowable), ThrowingRange::merge);
				}
			}
		}

		// Compute which blocks are effectively duplicates.
		Set<Block> temp = new HashSet<>();
		for (TryCatchBlockNode tryCatch : tryCatchBlocks) {
			ThrowingRange throwingRange = blockThrowingRanges.get(tryCatch);

			// Skip if block is redundant and has no valid throwing range.
			if (throwingRange == null)
				continue;

			// Check if we already have observed a block with the same effective range for the given type.
			Block model = new Block(tryCatch.type,
					throwingRange.first,
					throwingRange.last,
					instructions.indexOf(tryCatch.handler)
			);
			if (!temp.add(model))
				redundantBlocks.add(tryCatch);
		}

		// Remove known redundant blocks.
		if (!redundantBlocks.isEmpty()) {
			for (TryCatchBlockNode redundantBlock : redundantBlocks)
				tryCatchBlocks.remove(redundantBlock);

			// Because the control flow has changes we will want to remove code that is no longer accessible (dead catch handlers)
			context.pruneDeadCode(node, method);
			return true;
		}

		return false;
	}

	private boolean pass3ConvertOpaqueThrowToDirectFlow(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node, @Nonnull MethodNode method) {
		// TODO: Rather than make this a separate pass, I think we can work the intended behavior outlined here into the prior pass
		if (true) return false;

		// TODO: Look for try blocks that end in 'throw T' with a 'catch T' handler
		//  - Must always take the path
		//  - Not always direct, can be '1 / 0' with a 'catch MathError' handler
		//  - Handler ideally does not use the exception in any meaningful way
		//     - Best case, it gets popped, meaning we can just do a goto
		//     - Worst case, it relies on stack info from the thrown exception
		//       (we can keep the 'new T' or replace the throwing '1 / 0' with 'new T')
		//  - If found, replace the throwing code with a 'goto handler'
		InsnList instructions = method.instructions;
		for (TryCatchBlockNode tryCatch : new ArrayList<>(method.tryCatchBlocks)) {
			int start = instructions.indexOf(tryCatch.start);
			int end = instructions.indexOf(tryCatch.end);

			// TODO: Validate that the block WILL flow into a 'throw' case
			//  - Same code as prior pass?
			boolean willThrow = true;
			Set<AbstractInsnNode> throwingInstructions = Collections.newSetFromMap(new IdentityHashMap<>());
			for (int i = start; i < end; i++) {
				// TODO: Mark willThow false if control flow leads to path where no-100% throwing behavior is observed
			}

			// If the try range of the block WILL throw, we can replace the offending instructions with jumps to the handler block
			if (willThrow && !throwingInstructions.isEmpty()) {
				for (AbstractInsnNode thrower : throwingInstructions) {
					// TODO: If exception is not already on stack top, replace with junk exception (null is not a good replacement)
					instructions.insertBefore(thrower, new InsnNode(ACONST_NULL));
					instructions.insertBefore(thrower, new JumpInsnNode(GOTO, tryCatch.handler));
				}
				method.tryCatchBlocks.remove(tryCatch);
			}
		}

		return false;
	}

	private boolean pass3CombineAdjacentTriesWithSameHandler(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node, @Nonnull MethodNode method) {
		// TODO: If there are a series of touching ranges (may be separated by non-throwing instructions)
		//       that all point to the same handler, they can be merged into one try-catch with the discovered catch handler types.
		//       We cannot use the common type since that could lead to 'Throwable' which would change semantics.
		//   Example:
		//      try-handler: range=[A-B] handler=D:*
		//      try-handler: range=[B-C] handler=D:*
		//      try-handler: range=[C-D] handler=D:*
		//      --- D handler ----
		//      try-handler: range=[E-F] handler=D:*
		//      try-handler: range=[F-G] handler=D:*
		//      try-handler: range=[G-H] handler=D:*
		//   Would become:
		//      try-handler: range=[A-D] handler=D:*
		//      --- D handler ----
		//      try-handler: range=[E-H] handler=D:*
		return false;
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> dependencies() {
		return Set.of(DeadCodeRemovingTransformer.class, ExceptionCollectionTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Redundant try-catch removal";
	}

	private boolean doesHandleException(@Nonnull TryCatchBlockNode block, String... possiblyThrownExceptions) {
		return getHandlingForExceptions(block, possiblyThrownExceptions) == Handling.CAUGHT;
	}

	@Nonnull
	private Handling getHandlingForExceptions(@Nonnull TryCatchBlockNode block, String... possiblyThrownExceptions) {
		// Having no type indicates the block will handle anything. It would be equivalent
		// to the type being 'Throwable'
		if (block.type == null || block.type.equals("java/lang/Throwable"))
			return Handling.CAUGHT;

		for (String exception : possiblyThrownExceptions) {
			if (block.type.equals(exception))
				return Handling.CAUGHT;
			String key = exception + ':' + block.type;
			Handling handling = doesHandleCache.computeIfAbsent(key, k -> {
				InheritanceVertex vPossiblyThrown = inheritanceGraph.getVertex(exception);
				if (vPossiblyThrown == null)
					return Handling.UNKNOWN;

				InheritanceVertex vHandled = inheritanceGraph.getVertex(block.type);
				if (vHandled == null)
					return Handling.UNKNOWN;

				return vHandled.isParentOf(vPossiblyThrown) ? Handling.CAUGHT : Handling.NOT_CAUGHT;
			});
			if (handling == Handling.CAUGHT)
				return Handling.CAUGHT;
		}
		return Handling.NOT_CAUGHT;
	}

	/**
	 * Possible states of if a {@link TryCatchBlockNode} handles some given type.
	 */
	private enum Handling {
		/** Block will handle the given exception. */
		CAUGHT,
		/** Block won't handle the given exception. */
		NOT_CAUGHT,
		/**
		 * The catch handler type, or the given exception type could not be resolved,
		 * so we cannot know if it will catch or not.
		 */
		UNKNOWN
	}

	/**
	 * Throwing behavior of some instructions / block.
	 */
	private enum ThrowingBehavior {
		/** Nothing here will throw an exception. */
		NONE,
		/** Something here can throw an exception. */
		POSSIBLE,
		/** Something here will throw an exception. */
		ALWAYS;

		/**
		 * @return {@code true} for {@link #POSSIBLE} or {@link #ALWAYS}.
		 */
		boolean canThrow() {
			return ordinal() > 0;
		}
	}

	/**
	 * Minimal model of a {@link TryCatchBlockNode} using primitives over labels.
	 *
	 * @param type
	 * 		Catch handler type.
	 * @param start
	 * 		Offset into the {@link InsnList} of the block start.
	 * @param end
	 * 		Offset into the {@link InsnList} of the block end.
	 * @param handler
	 * 		Offset into the {@link InsnList} of the block handler start.
	 */
	private record Block(@Nullable String type, int start, int end, int handler) {}

	/**
	 * Range of some code.
	 *
	 * @param start
	 * 		Start label.
	 * @param end
	 * 		End label.
	 */
	private record Range(@Nonnull LabelNode start, @Nonnull LabelNode end) {}

	/**
	 * Range of instructions that can possibly throw exceptions.
	 *
	 * @param first
	 * 		Offset into {@link InsnList} of the first potentially throwing instruction.
	 * @param last
	 * 		Offset into {@link InsnList} of the last potentially throwing instruction.
	 */
	private record ThrowingRange(int first, int last) {
		@Nonnull
		public ThrowingRange merge(@Nonnull ThrowingRange other) {
			return new ThrowingRange(Math.min(first, other.first), Math.max(last, other.last));
		}
	}

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

		/**
		 * @param block
		 * 		Block to add.
		 */
		public void addBlock(@Nonnull TryCatchBlockNode block) {
			blocks.add(block);
		}

		/**
		 * Remove entries from {@link #blocks} that are redundant.
		 *
		 * @param graph
		 * 		Inheritance graph for classes in the workspace.
		 */
		public void prune(@Nonnull InheritanceGraph graph) {
			Iterator<TryCatchBlockNode> it = blocks.iterator();
			while (it.hasNext()) {
				TryCatchBlockNode block = it.next();
				String handledType = Objects.requireNonNullElse(block.type, "java/lang/Object");
				inner:
				{
					for (String seenType : seenTypes) {
						if (graph.isAssignableFrom(seenType, handledType)) {
							it.remove();
							break inner;
						}
					}
					seenTypes.add(handledType);
				}
			}
		}
	}
}
