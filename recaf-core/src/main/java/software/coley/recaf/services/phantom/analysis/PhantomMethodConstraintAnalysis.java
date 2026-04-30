package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Walks a method with a small simulated frame so phantom subtype requirements can be inferred from
 * stack and local variable usage. This is much more narrow in scope than what a full {@link Analyzer}
 * pass would be as we only care about reference-typed values.
 *
 * @author Matt Coley
 */
public class PhantomMethodConstraintAnalysis {
	private final PhantomGenerationContext context;

	/**
	 * @param context
	 * 		Shared analysis state for the current generation run.
	 */
	public PhantomMethodConstraintAnalysis(@Nonnull PhantomGenerationContext context) {
		this.context = context;
	}

	/**
	 * Collects subtype requirements implied by the method.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param ownerAccess
	 * 		Declaring type access flags.
	 * @param method
	 * 		Method to inspect.
	 */
	public void collect(@Nonnull String ownerName, int ownerAccess, @Nonnull MethodNode method) {
		if (method.instructions == null || method.instructions.size() == 0)
			return;

		// Map label to offsets.
		Map<LabelNode, Integer> labelIndices = new IdentityHashMap<>();
		AbstractInsnNode[] insns = method.instructions.toArray();
		for (int i = 0; i < insns.length; i++)
			if (insns[i] instanceof LabelNode labelNode)
				labelIndices.put(labelNode, i);

		// Collect CFG successors so we can propagate state across basic blocks.
		Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectMap<>(insns.length);
		AsmInsnUtil.populateFlowMaps(method, successorMap, new Int2ObjectMap<>(), false);
		Int2ObjectMap<List<ExceptionHandlerEdge>> exceptionHandlerMap = buildExceptionHandlerMap(method, insns, labelIndices);

		// Start with the initial state at the method entry and propagate forward, merging at control flow joins until we reach a fixed point.
		Map<Integer, SubtypeValue> initialLocals = initialLocals(ownerName, method, insns, labelIndices);
		SubtypeFrameState[] incomingStates = new SubtypeFrameState[insns.length];
		incomingStates[0] = new SubtypeFrameState(initialLocals, List.of());

		// Queue up propagation/simulation of state across the CFG until we reach a fixed point.
		// We'll cap the number of iterations to avoid infinite loops in any weird/contrived cases.
		int maxIterations = insns.length * 10;
		Deque<Integer> unprocessed = new ArrayDeque<>();
		unprocessed.add(0);
		while (!unprocessed.isEmpty() && (maxIterations-- > 0)) {
			int index = unprocessed.removeFirst();

			// Skip if we don't have any state for this instruction.
			// Will happen for any dead code that isn't reachable from the entry instruction.
			SubtypeFrameState incoming = incomingStates[index];
			if (incoming == null)
				continue;

			// Update constraints based on the current instruction and state.
			AbstractInsnNode insn = insns[index];
			collectConstraintsFromState(ownerName, method, index, insn, incoming, labelIndices);

			// Exception edges preserve locals from the throwing instruction, but replace the stack with the caught exception.
			for (ExceptionHandlerEdge exceptionHandler : exceptionHandlerMap.getOrDefault(index, List.of()))
				if (mergeState(incomingStates, exceptionHandler.handlerIndex(), exceptionState(incoming, exceptionHandler)))
					unprocessed.add(exceptionHandler.handlerIndex());

			// Continue the simulation, then merge state into successors and add them to the worklist if they changed.
			SubtypeFrameState outgoing = incoming.copy();
			simulateInstruction(insn, outgoing);
			for (int successor : successorMap.getOrDefault(index, List.of()))
				if (mergeState(incomingStates, successor, outgoing))
					unprocessed.add(successor);
		}
	}

	/**
	 * Reads the current state at one instruction and records any subtype requirements implied by that use.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param method
	 * 		Method being analyzed.
	 * @param instructionIndex
	 * 		Current instruction index.
	 * @param insn
	 * 		Current instruction.
	 * @param state
	 * 		Current simulated frame state.
	 * @param labelIndices
	 * 		Instruction index lookup for labels.
	 */
	private void collectConstraintsFromState(@Nonnull String ownerName,
	                                         @Nonnull MethodNode method,
	                                         int instructionIndex,
	                                         @Nonnull AbstractInsnNode insn,
	                                         @Nonnull SubtypeFrameState state,
	                                         @Nonnull Map<LabelNode, Integer> labelIndices) {
		List<SubtypeValue> stack = state.getStack();
		Map<Integer, SubtypeValue> locals = state.getLocals();
		switch (insn.getOpcode()) {
			case Opcodes.ASTORE -> {
				SubtypeValue value = peek(stack, 0);
				Type localTarget = localTargetType(ownerName, method, instructionIndex,
						((VarInsnNode) insn).var, labelIndices, locals);
				if (localTarget != null)
					addSubtypeConstraints(value, localTarget);
			}
			case Opcodes.GETFIELD ->
					addSubtypeConstraints(peek(stack, 0), Type.getObjectType(((FieldInsnNode) insn).owner));
			case Opcodes.PUTFIELD -> {
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				addSubtypeConstraints(peek(stack, 1), Type.getObjectType(fieldInsn.owner));
				addSubtypeConstraints(peek(stack, 0), Type.getType(fieldInsn.desc));
			}
			case Opcodes.PUTSTATIC -> addSubtypeConstraints(peek(stack, 0), Type.getType(((FieldInsnNode) insn).desc));
			case Opcodes.AASTORE -> {
				SubtypeValue value = peek(stack, 0);
				SubtypeValue arrayValue = peek(stack, 2);
				if (arrayValue.getType() != null && arrayValue.getType().getSort() == Type.ARRAY)
					addSubtypeConstraints(value, arrayValue.getType().getElementType());
			}
			case Opcodes.ARETURN -> addSubtypeConstraints(peek(stack, 0), Type.getReturnType(method.desc));
			case Opcodes.ATHROW -> addSubtypeConstraints(peek(stack, 0), Type.getObjectType("java/lang/Throwable"));
			case Opcodes.INVOKEINTERFACE, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				Type[] argumentTypes = Type.getArgumentTypes(methodInsn.desc);
				int stackOffset = 0;
				for (int arg = argumentTypes.length - 1; arg >= 0; arg--) {
					addSubtypeConstraints(peek(stack, stackOffset), argumentTypes[arg]);
					stackOffset++;
				}
				if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
					SubtypeValue receiver = peek(stack, stackOffset);
					if (insn.getOpcode() == Opcodes.INVOKEINTERFACE || insn.getOpcode() == Opcodes.INVOKEVIRTUAL ||
							(insn.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(methodInsn.name))) {
						addSubtypeConstraints(receiver, Type.getObjectType(methodInsn.owner));
					}
				}
			}
			default -> {
				// Other instructions do not add new subtype requirements.
			}
		}
	}

	/**
	 * Adds inheritance requirements for any phantom references tracked by the given value.
	 *
	 * @param source
	 * 		Value that carries the source phantom names.
	 * @param targetType
	 * 		Target type the value is being used as.
	 */
	private void addSubtypeConstraints(@Nullable SubtypeValue source, @Nullable Type targetType) {
		// Skip if there's no reference evidence or the target isn't a reference type.
		if (source == null || !isReferenceType(targetType))
			return;
		if (targetType.getSort() != Type.OBJECT)
			return;

		// Add a "source must extend/implement target" requirement for each phantom reference tracked by the value.
		String targetInternalName = targetType.getInternalName();
		for (String sourceInternalName : source.getReferenceNames()) {
			if (sourceInternalName.equals(targetInternalName))
				continue;
			PhantomClassConstraint sourceConstraint = context.getOrCreateConstraint(sourceInternalName);
			if (sourceConstraint == null)
				continue;
			sourceConstraint.markClass();
			sourceConstraint.addRequiredSupertype(targetInternalName);
		}
	}

	/**
	 * Builds the starting local state for a method.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param method
	 * 		Method being analyzed.
	 * @param insns
	 * 		Method instructions.
	 * @param labelIndices
	 * 		Instruction index lookup for labels.
	 *
	 * @return Initial local value map.
	 */
	@Nonnull
	private static Map<Integer, SubtypeValue> initialLocals(@Nonnull String ownerName,
	                                                        @Nonnull MethodNode method,
	                                                        @Nonnull AbstractInsnNode[] insns,
	                                                        @Nonnull Map<LabelNode, Integer> labelIndices) {
		Map<Integer, SubtypeValue> locals = new HashMap<>();
		int slot = 0;

		// Add 'this' reference for non-static methods.
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			locals.put(0, SubtypeValue.typed(Type.getObjectType(ownerName), Set.of(ownerName)));
			slot = 1;
		}

		// Add parameter types.
		for (Type argumentType : Type.getArgumentTypes(method.desc)) {
			locals.put(slot, SubtypeValue.typed(argumentType));
			slot += argumentType.getSize();
		}

		// Fill in anything we can from the local variable table.
		if (method.localVariables != null) {
			for (LocalVariableNode localVariable : method.localVariables) {
				Integer start = labelIndices.get(localVariable.start);
				if (start != null && start == 0) {
					Type localType = Type.getType(localVariable.desc);
					if (isReferenceType(localType))
						locals.put(localVariable.index, SubtypeValue.typed(localType));
				}
			}
		}

		// Collect any reference-typed local slots.
		List<Integer> observedReferenceLocals = new ArrayList<>();
		for (AbstractInsnNode insn : insns) {
			if (insn instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD &&
					!observedReferenceLocals.contains(varInsn.var)) {
				observedReferenceLocals.add(varInsn.var);
			}
		}

		// Collect parameter slot information.
		List<ParameterLocal> referenceParameters = new ArrayList<>();
		int parameterSlot = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
		for (Type parameterType : Type.getArgumentTypes(method.desc)) {
			if (isReferenceType(parameterType))
				referenceParameters.add(new ParameterLocal(parameterSlot, parameterType));
			parameterSlot += parameterType.getSize();
		}

		// For any observed reference-typed local slots that aren't already filled in, try to map them to parameters.
		if (!referenceParameters.isEmpty() && !observedReferenceLocals.isEmpty()) {
			int parameterIndex = 0;
			for (int observedSlot : observedReferenceLocals) {
				// Abort when we've run out of parameters.
				if (parameterIndex >= referenceParameters.size())
					break;

				// Skip parameters that are observed as reference-typed but don't match the current parameter index.
				ParameterLocal parameter = referenceParameters.get(parameterIndex);
				if (observedSlot == parameter.slot()) {
					parameterIndex++;
					continue;
				}

				// Skip observed slots that are already filled in from the local variable table.
				if (locals.containsKey(observedSlot))
					continue;

				// Map the observed slot to the parameter type.
				locals.put(observedSlot, SubtypeValue.typed(parameter.type()));
				parameterIndex++;
			}
		}
		return locals;
	}

	/**
	 * Finds the best available type for a local slot at the current instruction.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param method
	 * 		Method being analyzed.
	 * @param instructionIndex
	 * 		Current instruction index.
	 * @param varIndex
	 * 		Local slot index.
	 * @param labelIndices
	 * 		Instruction index lookup for labels.
	 * @param locals
	 * 		Current local value state.
	 *
	 * @return Best known type for the local, or {@code null} when none is known.
	 */
	@Nullable
	private static Type localTargetType(@Nonnull String ownerName,
	                                    @Nonnull MethodNode method,
	                                    int instructionIndex,
	                                    int varIndex,
	                                    @Nonnull Map<LabelNode, Integer> labelIndices,
	                                    @Nonnull Map<Integer, SubtypeValue> locals) {
		// Look in the local variable table first.
		Type localVariableType = activeLocalVariableType(method, instructionIndex, varIndex, labelIndices);
		if (localVariableType != null)
			return localVariableType;

		// Look in the method descriptor to match a parameter or 'this'.
		Type declaredType = declaredSlotType(ownerName, method.access, method.desc, varIndex);
		if (declaredType != null)
			return declaredType;

		// Fall back to the simulated local state.
		return localValueType(locals.get(varIndex));
	}

	/**
	 * Reads the local variable table for the type that is active at the current instruction.
	 *
	 * @param method
	 * 		Method being analyzed.
	 * @param instructionIndex
	 * 		Current instruction index.
	 * @param varIndex
	 * 		Local slot index.
	 * @param labelIndices
	 * 		Instruction index lookup for labels.
	 *
	 * @return Active local variable type, or {@code null} when not found.
	 */
	@Nullable
	private static Type activeLocalVariableType(@Nonnull MethodNode method,
	                                            int instructionIndex,
	                                            int varIndex,
	                                            @Nonnull Map<LabelNode, Integer> labelIndices) {
		// No table? No type.
		if (method.localVariables == null)
			return null;

		// Check for a variable that covers the current instruction and slot.
		for (LocalVariableNode localVariable : method.localVariables) {
			if (localVariable.index != varIndex)
				continue;
			Integer start = labelIndices.get(localVariable.start);
			Integer end = labelIndices.get(localVariable.end);
			if (start != null && end != null && instructionIndex >= start && instructionIndex < end)
				return Type.getType(localVariable.desc);
		}
		return null;
	}

	/**
	 * Derive the variable type by matching it to a parameter or this variable slot.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param methodAccess
	 * 		Method access flags.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param varIndex
	 * 		Local slot index.
	 *
	 * @return Declared slot type, or {@code null} when the slot is not a declared parameter.
	 */
	@Nullable
	private static Type declaredSlotType(@Nonnull String ownerName, int methodAccess,
	                                     @Nonnull String descriptor, int varIndex) {
		int slot = 0;
		if ((methodAccess & Opcodes.ACC_STATIC) == 0) {
			if (varIndex == 0)
				return Type.getObjectType(ownerName);
			slot = 1;
		}
		for (Type argumentType : Type.getArgumentTypes(descriptor)) {
			if (varIndex == slot)
				return argumentType;
			slot += argumentType.getSize();
		}
		return null;
	}

	/**
	 * Reads the current simulated value type for a local slot.
	 *
	 * @param localValue
	 * 		Current local value.
	 *
	 * @return Local type when it still looks like a reference, or {@code null}.
	 */
	@Nullable
	private static Type localValueType(@Nullable SubtypeValue localValue) {
		Type type = localValue == null ? null : localValue.getType();
		return isReferenceType(type) ? type : null;
	}

	/**
	 * Peek at a value on the stack.
	 *
	 * @param stack
	 * 		Current stack state.
	 * @param offsetFromTop
	 * 		Distance from the top of the stack.
	 *
	 * @return Stack value, or an uninitialized placeholder when the stack is too small.
	 */
	@Nonnull
	private static SubtypeValue peek(@Nonnull List<SubtypeValue> stack, int offsetFromTop) {
		int stackIndex = stack.size() - 1 - offsetFromTop;
		return stackIndex < 0 ? SubtypeValue.uninitialized() : stack.get(stackIndex);
	}

	/**
	 * Populate a map of exception control flow edges.
	 *
	 * @param method
	 * 		Method being analyzed.
	 * @param insns
	 * 		Method instructions.
	 * @param labelIndices
	 * 		Map of label nodes to instruction indices.
	 *
	 * @return Map of instruction indices to outgoing exception handler edges.
	 */
	@Nonnull
	private static Int2ObjectMap<List<ExceptionHandlerEdge>> buildExceptionHandlerMap(@Nonnull MethodNode method,
	                                                                                  @Nonnull AbstractInsnNode[] insns,
	                                                                                  @Nonnull Map<LabelNode, Integer> labelIndices) {
		Int2ObjectMap<List<ExceptionHandlerEdge>> exceptionHandlerMap = new Int2ObjectMap<>(insns.length);
		if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty())
			return exceptionHandlerMap;

		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			Integer start = labelIndices.get(block.start);
			Integer end = labelIndices.get(block.end);
			Integer handler = labelIndices.get(block.handler);
			if (start == null || end == null || handler == null)
				continue;

			ExceptionHandlerEdge edge = new ExceptionHandlerEdge(handler, caughtExceptionValue(block));
			for (int i = start; i < end && i < insns.length; i++)
				if (AsmInsnUtil.canThrow(insns[i]))
					exceptionHandlerMap.computeIfAbsent(i, _ -> new ArrayList<>()).add(edge);
		}

		return exceptionHandlerMap;
	}

	/**
	 * @param source
	 * 		Current state at the throwing instruction.
	 * @param exceptionHandler
	 * 		Exception handler edge being followed.
	 *
	 * @return New state for the exception handler, preserving locals but replacing the stack with the caught exception.
	 */
	@Nonnull
	private static SubtypeFrameState exceptionState(@Nonnull SubtypeFrameState source,
	                                                @Nonnull ExceptionHandlerEdge exceptionHandler) {
		return new SubtypeFrameState(source.getLocals(), List.of(exceptionHandler.exceptionValue()));
	}

	/**
	 * @param block
	 * 		Try-catch block being analyzed.
	 *
	 * @return Value representing the caught exception type, or a catch-all Throwable when the block is untyped.
	 */
	@Nonnull
	private static SubtypeValue caughtExceptionValue(@Nonnull TryCatchBlockNode block) {
		String internalName = block.type == null ? "java/lang/Throwable" : block.type;
		return SubtypeValue.typed(Type.getObjectType(internalName));
	}

	/**
	 * Merges a new incoming state into the stored state for one instruction.
	 *
	 * @param states
	 * 		Known states by instruction index.
	 * @param index
	 * 		Instruction index being updated.
	 * @param incoming
	 * 		New incoming state.
	 *
	 * @return {@code true} when the stored state changed and the instruction should be revisited.
	 */
	private static boolean mergeState(@Nonnull SubtypeFrameState[] states, int index, @Nonnull SubtypeFrameState incoming) {
		SubtypeFrameState existing = states[index];

		// No existing state --> store the incoming state.
		if (existing == null) {
			states[index] = incoming.copy();
			return true;
		}

		// Already have a state, merge incoming state into it.
		SubtypeFrameState merged = existing.merge(incoming);
		if (merged.equals(existing))
			return false;
		states[index] = merged;
		return true;
	}

	/**
	 * Simulate stack/local changes for the given instruction.
	 *
	 * @param insn
	 * 		Instruction to simulate.
	 * @param state
	 * 		Mutable state to update.
	 */
	private static void simulateInstruction(@Nonnull AbstractInsnNode insn, @Nonnull SubtypeFrameState state) {
		List<SubtypeValue> stack = state.getStack();
		Map<Integer, SubtypeValue> locals = state.getLocals();
		int op = insn.getOpcode();
		switch (op) {
			case -1,
			     Opcodes.NOP -> {
			}
			case Opcodes.ACONST_NULL -> push(stack, SubtypeValue.nullValue());
			case Opcodes.ICONST_M1,
			     Opcodes.ICONST_0,
			     Opcodes.ICONST_1,
			     Opcodes.ICONST_2,
			     Opcodes.ICONST_3,
			     Opcodes.ICONST_4,
			     Opcodes.ICONST_5,
			     Opcodes.BIPUSH,
			     Opcodes.SIPUSH -> push(stack, SubtypeValue.typed(Type.INT_TYPE));
			case Opcodes.LCONST_0,
			     Opcodes.LCONST_1 -> push(stack, SubtypeValue.typed(Type.LONG_TYPE));
			case Opcodes.FCONST_0,
			     Opcodes.FCONST_1,
			     Opcodes.FCONST_2 -> push(stack, SubtypeValue.typed(Type.FLOAT_TYPE));
			case Opcodes.DCONST_0,
			     Opcodes.DCONST_1 -> push(stack, SubtypeValue.typed(Type.DOUBLE_TYPE));
			case Opcodes.NEW -> {
				String desc = ((TypeInsnNode) insn).desc;
				push(stack, SubtypeValue.typed(Type.getObjectType(desc), Set.of(desc)));
			}
			case Opcodes.DUP -> push(stack, peek(stack, 0));
			case Opcodes.DUP_X1 -> {
				if (stack.size() < 2) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				SubtypeValue value2 = pop(stack);
				if (value1.getSize() != 1 || value2.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value1);
				push(stack, value2);
				push(stack, value1);
			}
			case Opcodes.DUP_X2 -> {
				if (stack.size() < 2) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				if (value1.getSize() != 1) {
					stack.clear();
					return;
				}
				SubtypeValue value2 = pop(stack);
				if (value2.getSize() == 2) {
					push(stack, value1);
					push(stack, value2);
					push(stack, value1);
					return;
				}
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value3 = pop(stack);
				if (value2.getSize() != 1 || value3.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value1);
				push(stack, value3);
				push(stack, value2);
				push(stack, value1);
			}
			case Opcodes.DUP2 -> {
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				if (value1.getSize() == 2) {
					push(stack, value1);
					push(stack, value1);
					return;
				}
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value2 = pop(stack);
				if (value2.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value2);
				push(stack, value1);
				push(stack, value2);
				push(stack, value1);
			}
			case Opcodes.DUP2_X1 -> {
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				if (value1.getSize() == 2) {
					if (stack.isEmpty()) {
						stack.clear();
						return;
					}
					SubtypeValue value2 = pop(stack);
					if (value2.getSize() != 1) {
						stack.clear();
						return;
					}
					push(stack, value1);
					push(stack, value2);
					push(stack, value1);
					return;
				}
				if (stack.size() < 2) {
					stack.clear();
					return;
				}
				SubtypeValue value2 = pop(stack);
				SubtypeValue value3 = pop(stack);
				if (value2.getSize() != 1 || value3.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value2);
				push(stack, value1);
				push(stack, value3);
				push(stack, value2);
				push(stack, value1);
			}
			case Opcodes.DUP2_X2 -> {
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				if (value1.getSize() == 2) {
					if (stack.isEmpty()) {
						stack.clear();
						return;
					}
					SubtypeValue value2 = pop(stack);
					if (value2.getSize() == 2) {
						push(stack, value1);
						push(stack, value2);
						push(stack, value1);
						return;
					}
					if (stack.isEmpty()) {
						stack.clear();
						return;
					}
					SubtypeValue value3 = pop(stack);
					if (value2.getSize() != 1 || value3.getSize() != 1) {
						stack.clear();
						return;
					}
					push(stack, value1);
					push(stack, value3);
					push(stack, value2);
					push(stack, value1);
					return;
				}
				if (stack.size() < 2) {
					stack.clear();
					return;
				}
				SubtypeValue value2 = pop(stack);
				if (value2.getSize() != 1 || stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value3 = pop(stack);
				if (value3.getSize() == 2) {
					push(stack, value2);
					push(stack, value1);
					push(stack, value3);
					push(stack, value2);
					push(stack, value1);
					return;
				}
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value4 = pop(stack);
				if (value3.getSize() != 1 || value4.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value2);
				push(stack, value1);
				push(stack, value4);
				push(stack, value3);
				push(stack, value2);
				push(stack, value1);
			}
			case Opcodes.POP -> pop(stack);
			case Opcodes.POP2 -> {
				if (stack.isEmpty()) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				if (value1.getSize() == 2)
					return;
				if (stack.isEmpty() || peek(stack, 0).getSize() != 1) {
					stack.clear();
					return;
				}
				pop(stack);
			}
			case Opcodes.SWAP -> {
				if (stack.size() < 2) {
					stack.clear();
					return;
				}
				SubtypeValue value1 = pop(stack);
				SubtypeValue value2 = pop(stack);
				if (value1.getSize() != 1 || value2.getSize() != 1) {
					stack.clear();
					return;
				}
				push(stack, value1);
				push(stack, value2);
			}
			case Opcodes.ALOAD ->
					push(stack, locals.getOrDefault(((VarInsnNode) insn).var, SubtypeValue.uninitialized()));
			case Opcodes.ASTORE -> locals.put(((VarInsnNode) insn).var, pop(stack));
			case Opcodes.GETFIELD -> {
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				pop(stack);
				push(stack, SubtypeValue.typed(Type.getType(fieldInsn.desc)));
			}
			case Opcodes.PUTFIELD -> {
				pop(stack);
				pop(stack);
			}
			case Opcodes.GETSTATIC -> push(stack, SubtypeValue.typed(Type.getType(((FieldInsnNode) insn).desc)));
			case Opcodes.PUTSTATIC -> pop(stack);
			case Opcodes.BALOAD,
			     Opcodes.CALOAD,
			     Opcodes.SALOAD,
			     Opcodes.IALOAD,
			     Opcodes.LALOAD,
			     Opcodes.FALOAD,
			     Opcodes.DALOAD,
			     Opcodes.AALOAD -> {
				pop(stack);
				SubtypeValue arrayValue = pop(stack);
				if (op == Opcodes.AALOAD) {
					Type arrayType = arrayValue.getType();
					if (arrayType != null && arrayType.getSort() == Type.ARRAY) {
						push(stack, SubtypeValue.typed(arrayType.getElementType()));
						return;
					}
				}
				push(stack, SubtypeValue.typed(Types.fromArrayOpcode(op)));
			}
			case Opcodes.BASTORE,
			     Opcodes.CASTORE,
			     Opcodes.SASTORE,
			     Opcodes.IASTORE,
			     Opcodes.LASTORE,
			     Opcodes.FASTORE,
			     Opcodes.DASTORE,
			     Opcodes.AASTORE -> {
				pop(stack);
				pop(stack);
				pop(stack);
			}
			case Opcodes.ARETURN,
			     Opcodes.ATHROW -> pop(stack);
			case Opcodes.CHECKCAST -> {}
			case Opcodes.LDC -> push(stack, SubtypeValue.typed(Types.fromLdc((LdcInsnNode) insn)));
			case Opcodes.ANEWARRAY -> {
				pop(stack);
				push(stack, SubtypeValue.typed(Type.getType("[L" + ((TypeInsnNode) insn).desc + ";")));
			}
			case Opcodes.NEWARRAY -> {
				pop(stack);
				push(stack, SubtypeValue.typed(Types.array(Types.newArrayElementType(((IntInsnNode) insn).operand), 1)));
			}
			case Opcodes.MULTIANEWARRAY -> {
				MultiANewArrayInsnNode arrayInsn = (MultiANewArrayInsnNode) insn;
				for (int dim = 0; dim < arrayInsn.dims; dim++)
					pop(stack);
				push(stack, SubtypeValue.typed(Type.getType(arrayInsn.desc)));
			}
			case Opcodes.INSTANCEOF -> {
				pop(stack);
				push(stack, SubtypeValue.typed(Type.INT_TYPE));
			}
			case Opcodes.INVOKEDYNAMIC -> {
				InvokeDynamicInsnNode indyInsn = (InvokeDynamicInsnNode) insn;
				for (Type ignored : Type.getArgumentTypes(indyInsn.desc))
					pop(stack);
				Type returnType = Type.getReturnType(indyInsn.desc);
				if (!Type.VOID_TYPE.equals(returnType))
					push(stack, SubtypeValue.typed(returnType));
			}
			case Opcodes.INVOKEINTERFACE, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				for (Type ignored : Type.getArgumentTypes(methodInsn.desc))
					pop(stack);
				if (op != Opcodes.INVOKESTATIC)
					pop(stack);
				Type returnType = Type.getReturnType(methodInsn.desc);
				if (!Type.VOID_TYPE.equals(returnType))
					push(stack, SubtypeValue.typed(returnType));
			}
			case Opcodes.ILOAD,
			     Opcodes.FLOAD,
			     Opcodes.LLOAD,
			     Opcodes.DLOAD -> push(stack, SubtypeValue.typed(Types.fromVarOpcode(op)));
			case Opcodes.ISTORE,
			     Opcodes.FSTORE,
			     Opcodes.LSTORE,
			     Opcodes.DSTORE -> pop(stack);
			case Opcodes.IINC,
			     Opcodes.RETURN,
			     Opcodes.GOTO -> {}
			case Opcodes.IFEQ,
			     Opcodes.IFNE,
			     Opcodes.IFLT,
			     Opcodes.IFGE,
			     Opcodes.IFGT,
			     Opcodes.IFLE,
			     Opcodes.IFNULL,
			     Opcodes.IFNONNULL,
			     Opcodes.IRETURN,
			     Opcodes.FRETURN,
			     Opcodes.LRETURN,
			     Opcodes.DRETURN,
			     Opcodes.TABLESWITCH,
			     Opcodes.LOOKUPSWITCH -> pop(stack);
			case Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE,
			     Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE -> {
				pop(stack);
				pop(stack);
			}
			default -> {
				// If the stack change is unclear, drop the current stack view instead of guessing and
				// creating subtype rules from a bad value later in the walk.
				stack.clear();
			}
		}
	}

	/**
	 * Pops one value from the stack.
	 *
	 * @param stack
	 * 		Mutable stack state.
	 *
	 * @return Removed value, or an uninitialized placeholder when the stack is empty.
	 */
	@Nonnull
	private static SubtypeValue pop(@Nonnull List<SubtypeValue> stack) {
		return stack.isEmpty() ? SubtypeValue.uninitialized() : stack.remove(stack.size() - 1);
	}

	/**
	 * Pushes one value onto the stack.
	 *
	 * @param stack
	 * 		Mutable stack state.
	 * @param value
	 * 		Value to push.
	 */
	private static void push(@Nonnull List<SubtypeValue> stack, @Nullable SubtypeValue value) {
		stack.add(value == null ? SubtypeValue.uninitialized() : value);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} when the type behaves like a reference.
	 */
	private static boolean isReferenceType(@Nullable Type type) {
		return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
	}

	/**
	 * Variable slot wrapper with type information.
	 *
	 * @param slot
	 * 		Local slot index.
	 * @param type
	 * 		Parameter type.
	 */
	private record ParameterLocal(int slot, @Nonnull Type type) {}

	/**
	 * Exception flow target and the caught exception value that enters it.
	 *
	 * @param handlerIndex
	 * 		Instruction index of the catch handler entry.
	 * @param exceptionValue
	 * 		Caught exception value present on the handler stack.
	 */
	private record ExceptionHandlerEdge(int handlerIndex, @Nonnull SubtypeValue exceptionValue) {}
}
