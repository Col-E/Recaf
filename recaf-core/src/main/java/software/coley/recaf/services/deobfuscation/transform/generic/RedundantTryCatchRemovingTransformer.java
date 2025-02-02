package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.workspace.WorkspaceManager;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

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
	private final Map<String, Handling> doesHandleCache = new HashMap<>();
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public RedundantTryCatchRemovingTransformer(@Nonnull WorkspaceManager workspaceManager, @Nonnull InheritanceGraphService graphService) {
		this.workspaceManager = workspaceManager;
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = workspace == workspaceManager.getCurrent() ?
				graphService.getCurrentWorkspaceInheritanceGraph() :
				graphService.newInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			InsnList instructions = method.instructions;
			List<TryCatchBlockNode> tryCatchBlocks = method.tryCatchBlocks;
			if (instructions == null || tryCatchBlocks == null || tryCatchBlocks.isEmpty())
				continue;

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
						ThrowingBehavior behavior = ThrowingBehavior.NONE;
						AbstractInsnNode insn = instructions.get(i);
						int op = insn.getOpcode();
						switch (op) {
							case IALOAD:
							case DALOAD:
							case FALOAD:
							case LALOAD:
							case SALOAD:
							case AALOAD: {
								Frame<ReValue> frame = frames[i];
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
							case FASTORE:
							case LASTORE:
							case SASTORE:
							case AASTORE: {
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
								Frame<ReValue> frame = frames[i];
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
				frames = context.analyze(inheritanceGraph, node, method);
				for (int i = instructions.size() - 1; i >= 0; i--) {
					AbstractInsnNode insn = instructions.get(i);
					if (frames[i] == null || insn.getOpcode() == NOP)
						instructions.remove(insn);
				}

				dirty = true;
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
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
}
