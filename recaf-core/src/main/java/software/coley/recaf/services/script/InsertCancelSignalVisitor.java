package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.CancelSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * Class visitor that inserts calls to {@link CancellationSingleton#poll()} at the start of loops.
 * <p>
 * This does not provide a perfect guarantee of cancellation, but it is a best-effort
 * to allow scripts to be stopped in a timely manner.
 *
 * @author xDark
 */
final class InsertCancelSignalVisitor extends ClassVisitor {
	private static final String SINGLETON_TYPE = Type.getInternalName(CancellationSingleton.class);
	private static final String CANCEL_SIGNAL_TYPE = Type.getInternalName(CancelSignal.class);
	private static final String POLL = "poll";
	private static final String POLL_DESC = "()V";

	/**
	 * @param cv
	 * 		Parent visitor to delegate to.
	 */
	InsertCancelSignalVisitor(@Nonnull ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
	                                 String[] exceptions) {
		MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodNode(api, access, name, descriptor, signature, exceptions) {
			@Override
			public void visitEnd() {
				insertPolls(this);
				insertCancelSignalRethrows(this);
				accept(delegate);
			}
		};
	}

	/**
	 * Inserts calls to {@link CancellationSingleton#poll()} at the start of loops.
	 *
	 * @param method
	 * 		Method to insert into.
	 */
	private static void insertPolls(@Nonnull MethodNode method) {
		// Collect label positions for loop detection.
		Map<LabelNode, Integer> positions = new IdentityHashMap<>();
		int position = 0;
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof LabelNode label)
				positions.put(label, position);
			position++;
		}

		// Collect loop heading labels.
		List<LabelNode> loopHeaders = new ArrayList<>();
		Set<LabelNode> addedLoopHeaders = Collections.newSetFromMap(new IdentityHashMap<>());
		position = 0;
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof JumpInsnNode jumpInsn) {
				if (isLoopback(positions, position, jumpInsn.label) && addedLoopHeaders.add(jumpInsn.label))
					loopHeaders.add(jumpInsn.label);
			} else if (insn instanceof LookupSwitchInsnNode switchInsn) {
				addLoopbackTarget(positions, position, switchInsn.dflt, loopHeaders, addedLoopHeaders);
				addLoopbackTargets(positions, position, switchInsn.labels, loopHeaders, addedLoopHeaders);
			} else if (insn instanceof TableSwitchInsnNode switchInsn) {
				addLoopbackTarget(positions, position, switchInsn.dflt, loopHeaders, addedLoopHeaders);
				addLoopbackTargets(positions, position, switchInsn.labels, loopHeaders, addedLoopHeaders);
			}
			position++;
		}

		// Insert polls at loop headers.
		for (LabelNode loopHeader : loopHeaders)
			method.instructions.insert(loopHeaderInsertionPoint(loopHeader), newPoll());
	}

	/**
	 * Adds loopback targets from a list of labels.
	 *
	 * @param positions
	 * 		Label positions in the method.
	 * @param sourcePosition
	 * 		Position of the jump instruction.
	 * @param labels
	 * 		List of jump target labels.
	 * @param loopHeaders
	 * 		List to add detected loop headers to.
	 * @param addedLoopHeaders
	 * 		Set to track already added loop headers and prevent duplicates.
	 */
	private static void addLoopbackTargets(@Nonnull Map<LabelNode, Integer> positions, int sourcePosition,
	                                       @Nullable List<LabelNode> labels,
	                                       @Nonnull List<LabelNode> loopHeaders,
	                                       @Nonnull Set<LabelNode> addedLoopHeaders) {
		if (labels == null)
			return;
		for (LabelNode label : labels)
			addLoopbackTarget(positions, sourcePosition, label, loopHeaders, addedLoopHeaders);
	}

	/**
	 * Adds a loopback target if it is a loop header.
	 *
	 * @param positions
	 * 		Label positions in the method.
	 * @param sourcePosition
	 * 		Position of the jump instruction.
	 * @param label
	 * 		Jump target label.
	 * @param loopHeaders
	 * 		List to add detected loop headers to.
	 * @param addedLoopHeaders
	 * 		Set to track already added loop headers and prevent duplicates.
	 */
	private static void addLoopbackTarget(@Nonnull Map<LabelNode, Integer> positions, int sourcePosition,
	                                      @Nonnull LabelNode label,
	                                      @Nonnull List<LabelNode> loopHeaders,
	                                      @Nonnull Set<LabelNode> addedLoopHeaders) {
		if (isLoopback(positions, sourcePosition, label) && addedLoopHeaders.add(label))
			loopHeaders.add(label);
	}

	/**
	 * Detects if a jump is a loopback edge.
	 *
	 * @param positions
	 * 		Label positions in the method.
	 * @param sourcePosition
	 * 		Position of the jump instruction.
	 * @param target
	 * 		Jump target label.
	 *
	 * @return {@code true} if the jump is a loopback edge, {@code false} otherwise.
	 */
	private static boolean isLoopback(@Nonnull Map<LabelNode, Integer> positions, int sourcePosition,
	                                  @Nonnull LabelNode target) {
		Integer targetPosition = positions.get(target);
		return targetPosition != null && targetPosition < sourcePosition;
	}

	/**
	 * Inserts rethrow logic for cancel signals at the start of catch blocks that catch them.
	 *
	 * @param method
	 * 		Method to insert into.
	 */
	private static void insertCancelSignalRethrows(@Nonnull MethodNode method) {
		// No catch blocks exist, so no rethrowing needed.
		if (method.tryCatchBlocks == null)
			return;

		// Insert rethrow logic at the start of any catch block that catches cancel signals.
		for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
			if (!catchesCancelSignal(tryCatchBlock.type))
				continue;
			AbstractInsnNode firstRealInsn = nextRealInstruction(tryCatchBlock.handler);
			method.instructions.insertBefore(firstRealInsn, newCancelSignalRethrow());
		}
	}

	/**
	 * @param type
	 * 		Exception handler type.
	 *
	 * @return {@code true} if the catch block catches {@link CancelSignal}.
	 */
	private static boolean catchesCancelSignal(@Nullable String type) {
		return type == null
				|| "java/lang/Throwable".equals(type)
				|| "java/lang/Error".equals(type)
				|| CANCEL_SIGNAL_TYPE.equals(type);
	}

	/**
	 * Finds the appropriate insertion point for a poll check at the start of a loop header.
	 *
	 * @param label
	 * 		Loop header label.
	 *
	 * @return Instruction to use as the insertion point for a poll check.
	 */
	@Nonnull
	private static AbstractInsnNode loopHeaderInsertionPoint(@Nonnull LabelNode label) {
		AbstractInsnNode prev = label;
		AbstractInsnNode next;
		while ((next = prev.getNext()) != null) {
			if (next.getOpcode() >= 0)
				break;
			prev = next;
		}
		return prev;
	}

	/**
	 * Finds the first real instruction after a label, skipping over any labels, line numbers, or frames.
	 *
	 * @param label
	 * 		Label to start from.
	 *
	 * @return Next real instruction after the label.
	 */
	@Nonnull
	private static AbstractInsnNode nextRealInstruction(@Nonnull LabelNode label) {
		AbstractInsnNode next = label;
		while ((next = next.getNext()) != null)
			if (next.getOpcode() >= 0)
				return next;

		// Should never happen in practice. Would imply execution falls off the end of the method.
		return label;
	}

	/**
	 * @return Block to call to {@link CancellationSingleton#poll()}.
	 */
	@Nonnull
	private static InsnList newPoll() {
		InsnList instructions = new InsnList();
		instructions.add(new MethodInsnNode(INVOKESTATIC, SINGLETON_TYPE, POLL, POLL_DESC, false));
		return instructions;
	}

	/**
	 * @return Block to rethrow an exception if it is an instance of {@link CancelSignal}.
	 */
	@Nonnull
	private static InsnList newCancelSignalRethrow() {
		InsnList instructions = new InsnList();
		LabelNode continueLabel = new LabelNode();
		instructions.add(new InsnNode(DUP));
		instructions.add(new org.objectweb.asm.tree.TypeInsnNode(INSTANCEOF, CANCEL_SIGNAL_TYPE));
		instructions.add(new JumpInsnNode(IFEQ, continueLabel));
		instructions.add(new InsnNode(ATHROW));
		instructions.add(continueLabel);
		return instructions;
	}
}
