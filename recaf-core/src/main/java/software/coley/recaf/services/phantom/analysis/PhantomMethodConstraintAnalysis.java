package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
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
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
	private static final Logger logger = Logging.get(PhantomMethodConstraintAnalysis.class);
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
		Map<Integer, SubtypeValue> initialLocals = initialLocals(ownerName, method, labelIndices);
		JoinStates joins = new JoinStates(ownerName, method, insns.length);
		joins.set(0, new SubtypeFrameState(initialLocals, List.of()));

		// Queue up propagation/simulation of state across the CFG until we reach a fixed point.
		// Every join changes only so often, so the loop terminates within the visit budget (see visitBudget).
		// If we exceed the budget something is wrong with the analysis process, and we have bigger problems about correctness.
		int stackLimit = Math.max(method.maxStack, 1);
		long visitBudget = visitBudget(ownerName, method, insns, stackLimit);
		long visits = 0;
		boolean stackOverflowLogged = false;
		Deque<Integer> unprocessed = new ArrayDeque<>();
		unprocessed.add(0);
		while (!unprocessed.isEmpty()) {
			if (++visits > visitBudget)
				throw new IllegalStateException("Subtype analysis of '" + ownerName + "." + method.name + method.desc
						+ "' exceeded its proven visit bound, which indicates an analysis bug");
			int index = unprocessed.removeFirst();

			// Skip if we don't have any state for this instruction.
			// Will happen for any dead code that isn't reachable from the entry instruction.
			SubtypeFrameState incoming = joins.get(index);
			if (incoming == null)
				continue;

			// Update constraints based on the current instruction and state.
			AbstractInsnNode insn = insns[index];
			collectConstraintsFromState(ownerName, method, index, insn, incoming, labelIndices);

			// Exception edges preserve locals from the throwing instruction, but replace the stack with the caught exception.
			for (ExceptionHandlerEdge exceptionHandler : exceptionHandlerMap.getOrDefault(index, List.of()))
				if (joins.merge(exceptionHandler.handlerIndex(), exceptionState(incoming, exceptionHandler)))
					unprocessed.add(exceptionHandler.handlerIndex());

			// Continue the simulation, then merge state into successors and add them to the worklist if they changed.
			SubtypeFrameState outgoing = incoming.copy();
			simulateInstruction(insn, outgoing);
			if (outgoing.getStack().size() > stackLimit) {
				// Only malformed methods overflow max_stack.
				if (!stackOverflowLogged) {
					stackOverflowLogged = true;
					logger.warn("Operand stack exceeds max_stack at an instruction in '{}.{}{}'," +
									" so its stack view is dropped and its subtype constraints may be incomplete",
							ownerName, method.name, method.desc);
				}

				// Drop the stack so we don't propagate a malformed state to successors. Locals are still preserved.
				outgoing.getStack().clear();
			}
			for (int successor : successorMap.getOrDefault(index, List.of()))
				if (joins.merge(successor, outgoing))
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
						((VarInsnNode) insn).var, labelIndices);
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
			case Opcodes.INVOKEDYNAMIC -> {
				InvokeDynamicInsnNode dynamicInsn = (InvokeDynamicInsnNode) insn;
				Type[] argumentTypes = Type.getArgumentTypes(dynamicInsn.desc);
				int stackOffset = 0;
				for (int arg = argumentTypes.length - 1; arg >= 0; arg--) {
					addSubtypeConstraints(peek(stack, stackOffset), argumentTypes[arg]);
					stackOffset++;
				}
			}
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
					addSubtypeConstraints(receiver, Type.getObjectType(methodInsn.owner));
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
		// Skip if the target isn't a reference type.
		if (!isReferenceType(targetType))
			return;

		// A target may exist only in debug metadata, so collect it before recording subtype evidence.
		context.collectType(targetType);
		if (source == null || targetType.getSort() != Type.OBJECT)
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
	 * @param labelIndices
	 * 		Instruction index lookup for labels.
	 *
	 * @return Initial local value map.
	 */
	@Nonnull
	private static Map<Integer, SubtypeValue> initialLocals(@Nonnull String ownerName,
	                                                        @Nonnull MethodNode method,
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

		return locals;
	}

	/**
	 * Finds the declared type of a local slot at the current instruction.
	 * <p>
	 * Only declared types are used. The value currently simulated in a slot is not a declaration, since javac reuses
	 * slots for unrelated types when no local variable table is present.
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
	 *
	 * @return Declared type for the local, or {@code null} when none is declared.
	 */
	@Nullable
	private static Type localTargetType(@Nonnull String ownerName,
	                                    @Nonnull MethodNode method,
	                                    int instructionIndex,
	                                    int varIndex,
	                                    @Nonnull Map<LabelNode, Integer> labelIndices) {
		// Look in the local variable table first.
		Type localVariableType = activeLocalVariableType(method, instructionIndex, varIndex, labelIndices);
		if (localVariableType != null)
			return localVariableType;

		// Look in the method descriptor to match a parameter or 'this'.
		return declaredSlotType(ownerName, method.access, method.desc, varIndex);
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
				if (canThrowForPhantomAnalysis(insns[i]))
					exceptionHandlerMap.computeIfAbsent(i, _ -> new ArrayList<>()).add(edge);
		}

		return exceptionHandlerMap;
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when the instruction can throw an exception, which means it may have an exception handler edge.
	 */
	private static boolean canThrowForPhantomAnalysis(@Nonnull AbstractInsnNode insn) {
		if (AsmInsnUtil.canThrow(insn))
			return true;

		// These aren't marked as 'can-throw' by the shared utility, as that is used by transformers that
		// currently don't bail out on the rare case of class initialization throwing errors.
		// But for phantom analysis, we want to model this possible LinkageError/NoClassDefFoundError.
		//
 		// iconst_0
		// istore x
		// try:
		//     <potentially throwing operation>
		//     iconst_1
		//     istore x
		//     goto normal
		// catch Throwable:
		//     pop
		//     iload x
		//     ireturn
		// normal:
		//     iload x
		//     ireturn
		//
		int opcode = insn.getOpcode();
		if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC)
			return true;
		if (opcode == Opcodes.LDC) {
			Object constant = ((LdcInsnNode) insn).cst;
			return constant instanceof Type || constant instanceof Handle || constant instanceof ConstantDynamic;
		}

		return false;
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
	 * Upper bound on the worklist visits one method can take.
	 * <p>
	 * An instruction's incoming state only changes by growing. Reference names are added, types move toward
	 * {@link Types#OBJECT_TYPE}, and an empty stack takes the height of its first non-empty arrival. An arrival with a
	 * conflicting stack height freezes the instruction, so nothing changes after that. Each stack position and local slot
	 * therefore changes at most {@code names + 3} times, and each stack height is set at most once. Every change enqueues
	 * one visit.
	 * <p>
	 * Stacks never exceed {@code stackLimit}, the method's {@code max_stack} (at least one). Collection drops any stack
	 * that overflows it, so the bound also holds for malformed methods. With the stack limit fixed, the bound is linear
	 * in the instruction count.
	 *
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param method
	 * 		Method being analyzed.
	 * @param insns
	 * 		Method instructions.
	 * @param stackLimit
	 * 		Largest operand stack height the analysis stores, see {@code max_stack}.
	 *
	 * @return Upper bound on visits, including the initial visit of the entry instruction.
	 */
	private static long visitBudget(@Nonnull String ownerName, @Nonnull MethodNode method, @Nonnull AbstractInsnNode[] insns, int stackLimit) {
		long instructions = insns.length;
		long changesPerInstruction = 2L + ((long) localSlotBound(method, insns) + stackLimit) * (referenceNameBound(ownerName, method, insns) + 3L);
		return 1L + instructions * changesPerInstruction;
	}

	/**
	 * @param method
	 * 		Method being analyzed.
	 * @param insns
	 * 		Method instructions.
	 *
	 * @return Upper bound on the distinct local slots the simulation can store values into.
	 */
	private static int localSlotBound(@Nonnull MethodNode method, @Nonnull AbstractInsnNode[] insns) {
		Set<Integer> slots = new HashSet<>();
		int slot = 0;

		// This
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			slots.add(0);
			slot = 1;
		}

		// Parameters
		for (Type argumentType : Type.getArgumentTypes(method.desc)) {
			slots.add(slot);
			slot += argumentType.getSize();
		}

		// Local variables
		if (method.localVariables != null)
			for (LocalVariableNode localVariable : method.localVariables)
				slots.add(localVariable.index);
		for (AbstractInsnNode insn : insns)
			if (insn instanceof VarInsnNode varInsn)
				slots.add(varInsn.var);

		return slots.size();
	}

	/**
	 * @param ownerName
	 * 		Internal name of the declaring type.
	 * @param method
	 * 		Method being analyzed.
	 * @param insns
	 * 		Method instructions.
	 *
	 * @return Upper bound on the distinct reference names in the analysis.
	 */
	private static int referenceNameBound(@Nonnull String ownerName, @Nonnull MethodNode method, @Nonnull AbstractInsnNode[] insns) {
		Set<String> internalNames = new HashSet<>();
		List<String> descriptors = new ArrayList<>();

		internalNames.add(ownerName);
		internalNames.add(Types.OBJECT_TYPE.getInternalName());
		internalNames.add(Types.STRING_TYPE.getInternalName());
		internalNames.add(Types.CLASS_TYPE.getInternalName());
		internalNames.add(Types.METHOD_HANDLE_TYPE.getInternalName());
		internalNames.add("java/lang/Throwable");

		descriptors.add(method.desc);
		if (method.localVariables != null)
			for (LocalVariableNode localVariable : method.localVariables)
				descriptors.add(localVariable.desc);
		if (method.tryCatchBlocks != null)
			for (TryCatchBlockNode block : method.tryCatchBlocks)
				if (block.type != null)
					internalNames.add(block.type);
		for (AbstractInsnNode insn : insns) {
			if (insn instanceof TypeInsnNode typeInsn) {
				internalNames.add(typeInsn.desc);
			} else if (insn instanceof FieldInsnNode fieldInsn) {
				internalNames.add(fieldInsn.owner);
				descriptors.add(fieldInsn.desc);
			} else if (insn instanceof MethodInsnNode methodInsn) {
				internalNames.add(methodInsn.owner);
				descriptors.add(methodInsn.desc);
			} else if (insn instanceof InvokeDynamicInsnNode indyInsn) {
				descriptors.add(indyInsn.desc);
			} else if (insn instanceof MultiANewArrayInsnNode arrayInsn) {
				descriptors.add(arrayInsn.desc);
			} else if (insn instanceof LdcInsnNode ldcInsn && ldcInsn.cst instanceof ConstantDynamic dynamic) {
				descriptors.add(dynamic.getDescriptor());
			}
		}
		int bound = internalNames.size();
		for (String descriptor : descriptors)
			for (int i = 0; i < descriptor.length(); i++)
				if (descriptor.charAt(i) == 'L')
					bound++;
		return bound;
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
			case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
			     Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB,
			     Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
			     Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV,
			     Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM,
			     Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR,
			     Opcodes.IAND, Opcodes.LAND, Opcodes.IOR, Opcodes.LOR, Opcodes.IXOR, Opcodes.LXOR -> {
				pop(stack);
				pop(stack);
				push(stack, SubtypeValue.typed(numericResultType(op)));
			}
			case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG,
			     Opcodes.I2L, Opcodes.I2F, Opcodes.I2D, Opcodes.L2I, Opcodes.L2F, Opcodes.L2D,
			     Opcodes.F2I, Opcodes.F2L, Opcodes.F2D, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F,
			     Opcodes.I2B, Opcodes.I2C, Opcodes.I2S -> {
				pop(stack);
				push(stack, SubtypeValue.typed(numericResultType(op)));
			}
			case Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG -> {
				pop(stack);
				pop(stack);
				push(stack, SubtypeValue.typed(Type.INT_TYPE));
			}
			case Opcodes.ARRAYLENGTH -> {
				pop(stack);
				push(stack, SubtypeValue.typed(Type.INT_TYPE));
			}
			case Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> pop(stack);
			default -> {
				// Only the legacy subroutine opcodes (JSR and RET) reach here. Their stack effect is not modeled,
				// so drop the current stack view instead of guessing and creating subtype rules from a bad value later.
				stack.clear();
			}
		}
	}

	/**
	 * @param op
	 * 		Arithmetic, shift, bitwise, negation, or conversion opcode.
	 *
	 * @return Type of the value the opcode produces. Long and double results keep their two-slot width.
	 */
	@Nonnull
	private static Type numericResultType(int op) {
		return switch (op) {
			case Opcodes.IADD,
			     Opcodes.ISUB,
			     Opcodes.IMUL,
			     Opcodes.IDIV,
			     Opcodes.IREM,
			     Opcodes.INEG,
			     Opcodes.ISHL,
			     Opcodes.ISHR,
			     Opcodes.IUSHR,
			     Opcodes.IAND,
			     Opcodes.IOR,
			     Opcodes.IXOR,
			     Opcodes.L2I,
			     Opcodes.F2I,
			     Opcodes.D2I,
			     Opcodes.I2B,
			     Opcodes.I2C,
			     Opcodes.I2S -> Type.INT_TYPE;
			case Opcodes.LADD,
			     Opcodes.LSUB,
			     Opcodes.LMUL,
			     Opcodes.LDIV,
			     Opcodes.LREM,
			     Opcodes.LNEG,
			     Opcodes.LSHL,
			     Opcodes.LSHR,
			     Opcodes.LUSHR,
			     Opcodes.LAND,
			     Opcodes.LOR,
			     Opcodes.LXOR,
			     Opcodes.I2L,
			     Opcodes.F2L,
			     Opcodes.D2L -> Type.LONG_TYPE;
			case Opcodes.FADD,
			     Opcodes.FSUB,
			     Opcodes.FMUL,
			     Opcodes.FDIV,
			     Opcodes.FREM,
			     Opcodes.FNEG,
			     Opcodes.I2F,
			     Opcodes.L2F,
			     Opcodes.D2F -> Type.FLOAT_TYPE;
			case Opcodes.DADD,
			     Opcodes.DSUB,
			     Opcodes.DMUL,
			     Opcodes.DDIV,
			     Opcodes.DREM,
			     Opcodes.DNEG,
			     Opcodes.I2D,
			     Opcodes.L2D,
			     Opcodes.F2D -> Type.DOUBLE_TYPE;
			default -> throw new IllegalArgumentException("Not a numeric opcode: " + op);
		};
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
	 * Exception flow target and the caught exception value that enters it.
	 *
	 * @param handlerIndex
	 * 		Instruction index of the catch handler entry.
	 * @param exceptionValue
	 * 		Caught exception value present on the handler stack.
	 */
	private record ExceptionHandlerEdge(int handlerIndex, @Nonnull SubtypeValue exceptionValue) {}

	/**
	 * Incoming state of every instruction, merged across all paths that reach it.
	 */
	private static final class JoinStates {
		private final SubtypeFrameState[] states;
		private final boolean[] frozen;
		private final String ownerName;
		private final MethodNode method;
		private boolean conflictLogged;

		private JoinStates(@Nonnull String ownerName, @Nonnull MethodNode method, int size) {
			this.states = new SubtypeFrameState[size];
			this.frozen = new boolean[size];
			this.ownerName = ownerName;
			this.method = method;
		}

		/**
		 * @param index
		 * 		Instruction index being read.
		 *
		 * @return Current state for the instruction, or {@code null} when no state has been stored yet.
		 */
		@Nullable
		private SubtypeFrameState get(int index) {
			return states[index];
		}

		/**
		 * Sets the state for one instruction, replacing any existing state.
		 *
		 * @param index
		 * 		Instruction index being updated.
		 * @param state
		 * 		New state to store.
		 */
		private void set(int index, @Nonnull SubtypeFrameState state) {
			states[index] = state;
		}

		/**
		 * Merges an arriving state into the stored state for one instruction.
		 * <p>
		 * An arrival whose operand stack height conflicts with the stored one freezes the instruction. Its stored state
		 * is kept, and later arrivals are ignored. This is what keeps {@link #visitBudget} a valid bound.
		 *
		 * @param index
		 * 		Instruction index being updated.
		 * @param incoming
		 * 		Arriving state.
		 *
		 * @return {@code true} when the stored state changed and the instruction should be revisited.
		 */
		private boolean merge(int index, @Nonnull SubtypeFrameState incoming) {
			if (frozen[index])
				return false;

			// No existing state? Just store the incoming one.
			SubtypeFrameState existing = states[index];
			if (existing == null) {
				states[index] = incoming.copy();
				return true;
			}

			// Conflicting stack heights? Freeze the instruction and log a warning.
			if (existing.stackHeightsConflict(incoming)) {
				frozen[index] = true;
				if (!conflictLogged) {
					conflictLogged = true;
					logger.warn("Operand stack heights disagree at an instruction in '{}.{}{}'," +
									" so its state is frozen and its subtype constraints may be incomplete",
							ownerName, method.name, method.desc);
				}
				return false;
			}

			// Merge the incoming state into the existing one, and store it if it changed.
			SubtypeFrameState merged = existing.merge(incoming);
			if (merged.equals(existing))
				return false;
			states[index] = merged;
			return true;
		}
	}

}
