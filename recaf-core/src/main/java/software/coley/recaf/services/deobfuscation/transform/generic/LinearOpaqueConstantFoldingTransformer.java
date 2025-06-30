package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * A transformer that folds <i>basic</i> linear constant usages.
 *
 * @author Matt Coley
 */
@Dependent
public class LinearOpaqueConstantFoldingTransformer implements JvmClassTransformer {
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public LinearOpaqueConstantFoldingTransformer(@Nonnull WorkspaceManager workspaceManager, @Nonnull InheritanceGraphService graphService) {
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
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;
			try {
				Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
				for (int i = 1; i < method.instructions.size() - 1; i++) {
					// We must know the contents of the next frame, and it must have 1 or more values on the stack.
					Frame<ReValue> nextFrame = frames[i + 1];
					if (nextFrame == null || nextFrame.getStackSize() == 0)
						continue;

					// The next frame's stack top is the result of the operation.
					ReValue nextFrameStackTop = nextFrame.getStack(nextFrame.getStackSize() - 1);
					if (!nextFrameStackTop.hasKnownValue())
						continue;

					// Handle folding for specific instructions.
					AbstractInsnNode instruction = method.instructions.get(i);
					int opcode = instruction.getOpcode();
					switch (opcode) {
						case IADD:
						case FADD:
						case ISUB:
						case FSUB:
						case IMUL:
						case FMUL:
						case IDIV:
						case FDIV:
						case IREM:
						case FREM:
						case ISHL:
						case ISHR:
						case IUSHR:
						case IAND:
						case IXOR:
						case IOR:
						case DREM:
						case DDIV:
						case DMUL:
						case DSUB:
						case DADD:
						case LUSHR:
						case LSHR:
						case LSHL:
						case LREM:
						case LDIV:
						case LMUL:
						case LSUB:
						case LADD:
						case LAND:
						case LOR:
						case LXOR:
						case FCMPL:
						case FCMPG:
						case LCMP:
						case DCMPL:
						case DCMPG: {
							// Get instruction of the top stack's contributing instruction.
							AbstractInsnNode argument2 = method.instructions.get(i - 1);
							while (argument2 != null && argument2.getOpcode() == NOP)
								argument2 = argument2.getPrevious();
							if (argument2 == null)
								continue;

							// Get instruction of the 2nd-to-top stack's contributing instruction.
							AbstractInsnNode argument1 = argument2.getPrevious();
							while (argument1 != null && argument1.getOpcode() == NOP)
								argument1 = argument1.getPrevious();
							if (argument1 == null)
								continue;

							// Both argument instructions must be value producers.
							if (!isSupportedValueProducer(argument1) || !isSupportedValueProducer(argument2))
								continue;

							// We must have a viable replacement to offer.
							AbstractInsnNode replacement = toInsn(nextFrameStackTop);
							if (replacement == null)
								continue;

							// fix dup/dup2
							boolean arg1IsDup = argument1.getOpcode() == DUP || argument1.getOpcode() == DUP2;
							boolean arg2IsDup = argument2.getOpcode() == DUP || argument2.getOpcode() == DUP2;
							if (arg1IsDup) {
								// in the dup or dup2 position replace it with the value
								AbstractInsnNode orig1 = argument1.getPrevious();
								while (orig1 != null && orig1.getOpcode() == NOP)
									orig1 = orig1.getPrevious();
								if (orig1 != null && isSupportedValueProducer(orig1)) {
									AbstractInsnNode restore = toInsn(nextFrame.getStack(nextFrame.getStackSize() - 3));
									if (restore != null) {
										method.instructions.set(argument1, restore);
									} else {
										method.instructions.set(argument1, new InsnNode(NOP));
									}
								} else {
									method.instructions.set(argument1, new InsnNode(NOP));
								}
							} else {
								method.instructions.set(argument1, new InsnNode(NOP));
							}
							if (arg2IsDup) {
								AbstractInsnNode orig2 = argument2.getPrevious();
								while (orig2 != null && orig2.getOpcode() == NOP)
									orig2 = orig2.getPrevious();
								if (orig2 != null && isSupportedValueProducer(orig2)) {
									AbstractInsnNode restore = toInsn(nextFrame.getStack(nextFrame.getStackSize() - 2));
									if (restore != null) {
										method.instructions.set(argument2, restore);
									} else {
										method.instructions.set(argument2, new InsnNode(NOP));
									}
								} else {
									method.instructions.set(argument2, new InsnNode(NOP));
								}
							} else {
								method.instructions.set(argument2, new InsnNode(NOP));
							}
							method.instructions.set(instruction, replacement);
							dirty = true;
							break;
						}
						case INEG:
						case FNEG:
						case DNEG:
						case LNEG:
						case I2L:
						case I2F:
						case I2D:
						case F2I:
						case F2L:
						case F2D:
						case I2B:
						case I2C:
						case I2S:
						case D2I:
						case D2L:
						case D2F:
						case L2I:
						case L2F:
						case L2D: {
							// Get instruction of the top stack's contributing instruction.
							// It must also be a value producing instruction.
							AbstractInsnNode argument = method.instructions.get(i - 1);
							while (argument != null && argument.getOpcode() == NOP)
								argument = argument.getPrevious();
							if (argument == null || !isSupportedValueProducer(argument))
								continue;

							// We must have a viable replacement to offer.
							AbstractInsnNode replacement = toInsn(nextFrameStackTop);
							if (replacement == null)
								continue;

							// Replace the argument and operation instructions with the replacement const value.
							method.instructions.set(instruction, replacement);
							method.instructions.set(argument, new InsnNode(NOP));
							dirty = true;
							break;
						}
						case INVOKESPECIAL:
						case INVOKEINTERFACE:
						case INVOKEVIRTUAL:
						case INVOKESTATIC: {
							// We'll have some loops further below that will set this flag to indicate to skip this
							// instruction after the loop completes.
							boolean skip = false;

							// Get the contributing instructions.
							MethodInsnNode min = (MethodInsnNode) instruction;
							Type methodType = Type.getMethodType(min.desc);
							List<AbstractInsnNode> argumentInstructions = new ArrayList<>(methodType.getArgumentCount());
							int start = opcode == INVOKESTATIC ? 1 : 0; // non-static methods start at zero to include the method instance host.
							for (int arg = methodType.getArgumentCount(); arg >= start; arg--) {
								// Get the contributing instruction for this argument (or method instance host for non-static methods when arg == 0)
								AbstractInsnNode lastArgumentInsn = argumentInstructions.isEmpty() ? null : argumentInstructions.getLast();
								AbstractInsnNode argument = lastArgumentInsn == null ?
										method.instructions.get(i - 1) : lastArgumentInsn.getPrevious();

								// Argument must be a value producing instruction.
								while (argument != null && argument.getOpcode() == NOP)
									argument = argument.getPrevious();
								if (argument == null || !isSupportedValueProducer(argument)) {
									skip = true;
									break;
								}
								argumentInstructions.add(argument);
							}
							if (skip)
								continue;

							// We must have a viable replacement to offer.
							AbstractInsnNode replacement = toInsn(nextFrameStackTop);
							if (replacement == null)
								continue;

							// Replace the arguments and invoke instructions with the replacement const value.
							method.instructions.set(instruction, replacement);
							for (AbstractInsnNode argument : argumentInstructions)
								method.instructions.set(argument, new InsnNode(NOP));
							dirty = true;
							break;
						}
						case GETSTATIC:
						case GETFIELD: {
							// We'll have some loops further below that will set this flag to indicate to skip this
							// instruction after the loop completes.
							boolean skip = false;

							// Get the contributing instructions.
							FieldInsnNode fin = (FieldInsnNode) instruction;
							if (opcode == GETFIELD) {
								// Get instruction of the top stack's contributing instruction.
								AbstractInsnNode argumentValue = method.instructions.get(i - 1);
								while (argumentValue != null && argumentValue.getOpcode() == NOP)
									argumentValue = argumentValue.getPrevious();
								if (argumentValue == null)
									continue;

								// Get instruction of the 2nd-to-top stack's contributing instruction.
								AbstractInsnNode argumentContext = argumentValue.getPrevious();
								while (argumentContext != null && argumentContext.getOpcode() == NOP)
									argumentContext = argumentContext.getPrevious();
								if (argumentContext == null)
									continue;

								// Both argument instructions must be value producers.
								if (!isSupportedValueProducer(argumentValue) || !isSupportedValueProducer(argumentContext))
									continue;

								// We must have a viable replacement to offer.
								AbstractInsnNode replacement = toInsn(nextFrameStackTop);
								if (replacement == null)
									continue;

								// Replace the arguments and field instructions with the replacement const value.
								method.instructions.set(instruction, replacement);
								method.instructions.set(argumentValue, new InsnNode(NOP));
								method.instructions.set(argumentContext, new InsnNode(NOP));
								dirty = true;
							} else {
								// Get instruction of the top stack's contributing instruction.
								// It must also be a value producing instruction.
								AbstractInsnNode argumentValue = method.instructions.get(i - 1);
								while (argumentValue != null && argumentValue.getOpcode() == NOP)
									argumentValue = argumentValue.getPrevious();
								if (argumentValue == null || !isSupportedValueProducer(argumentValue))
									continue;

								// We must have a viable replacement to offer.
								AbstractInsnNode replacement = toInsn(nextFrameStackTop);
								if (replacement == null)
									continue;

								// Replace the argument and field instructions with the replacement const value.
								method.instructions.set(instruction, replacement);
								method.instructions.set(argumentValue, new InsnNode(NOP));
								dirty = true;
								break;
							}
							break;
						}
					}
				}
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when folding constants", t);
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public String name() {
		return "Opaque constant folding";
	}

	/**
	 * Check if the instruction is responsible for providing some value we can possibly fold.
	 * This method doesn't tell us if the value is known though. The next frame after this
	 * instruction should have the provided value on the stack top.
	 *
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when the instruction will produce a single value.
	 */
	protected static boolean isSupportedValueProducer(@Nonnull AbstractInsnNode insn) {
		// Skip if this instruction consumes a value off the stack.
		if (AsmInsnUtil.getSizeConsumed(insn) > 0)
			return false;

		// The following cases are supported:
		//  - constants
		//  - variable loads (context will determine if value in variable is constant at the given position)
		//  - dup / dup2
		//  - static field gets (context will determine if value in field is constant/known)
		//  - static method calls with 0 args (context will determine if returned value of method is constant/known)
		int op = insn.getOpcode();
		if (AsmInsnUtil.isConstValue(op))
			return true;
		if (op >= ILOAD && op <= ALOAD)
			return true;
		if (op == GETSTATIC)
			return true;
		if (op == DUP || op == DUP2)
			return true;
		return op == INVOKESTATIC
				&& insn instanceof MethodInsnNode min
				&& min.desc.startsWith("()")
				&& !min.desc.endsWith(")V");
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Instruction representing the value,
	 * or {@code null} if we don't/can't provide a mapping for the value content.
	 */
	@Nullable
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private static AbstractInsnNode toInsn(@Nonnull ReValue value) {
		// Skip if value is not known.
		if (!value.hasKnownValue())
			return null;

		// Map known value types to constant value instructions.
		return switch (value) {
			case IntValue intValue -> AsmInsnUtil.intToInsn(intValue.value().getAsInt());
			case FloatValue floatValue -> AsmInsnUtil.floatToInsn((float) floatValue.value().getAsDouble());
			case DoubleValue doubleValue -> AsmInsnUtil.doubleToInsn(doubleValue.value().getAsDouble());
			case LongValue longValue -> AsmInsnUtil.longToInsn(longValue.value().getAsLong());
			case StringValue stringValue -> new LdcInsnNode(stringValue.getText().get());
			default -> null;
		};
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		return Set.of(NopRemovingTransformer.class);
	}
}
