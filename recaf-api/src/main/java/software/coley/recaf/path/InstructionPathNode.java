package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Path node for instructions within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class InstructionPathNode extends AbstractPathNode<ClassMember, AbstractInsnNode> {
	/**
	 * Node without parent.
	 *
	 * @param insn
	 * 		Instruction value.
	 */
	public InstructionPathNode(@Nonnull AbstractInsnNode insn) {
		this(null, insn);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param insn
	 * 		Instruction value.
	 *
	 * @see ClassMemberPathNode#childInsn(AbstractInsnNode)
	 */
	public InstructionPathNode(@Nullable ClassMemberPathNode parent, @Nonnull AbstractInsnNode insn) {
		super("catch", parent, AbstractInsnNode.class, insn);
	}

	@Override
	public ClassMemberPathNode getParent() {
		return (ClassMemberPathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof InstructionPathNode node) {
			// TODO: Comparison should be based on index in the method instructions, but we don't track that yet.
			return toString(getValue()).compareTo(toString(node.getValue()));
		}
		return 0;
	}

	private static String toString(AbstractInsnNode instruction) {
		int opcode = instruction.getOpcode();
		Textifier textifier = new Textifier();
		switch (instruction.getType()) {
			case AbstractInsnNode.INSN -> textifier.visitInsn(opcode);
			case AbstractInsnNode.INT_INSN -> {
				IntInsnNode intInsnNode = (IntInsnNode) instruction;
				textifier.visitIntInsn(opcode, intInsnNode.operand);
			}
			case AbstractInsnNode.VAR_INSN -> {
				VarInsnNode varInsnNode = (VarInsnNode) instruction;
				textifier.visitVarInsn(opcode, varInsnNode.var);
			}
			case AbstractInsnNode.TYPE_INSN -> {
				TypeInsnNode typeInsnNode = (TypeInsnNode) instruction;
				textifier.visitTypeInsn(opcode, typeInsnNode.desc);
			}
			case AbstractInsnNode.FIELD_INSN -> {
				FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
				textifier.visitFieldInsn(opcode, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
			}
			case AbstractInsnNode.METHOD_INSN -> {
				MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
				textifier.visitMethodInsn(opcode, methodInsnNode.owner, methodInsnNode.name,
						methodInsnNode.desc, methodInsnNode.itf);
			}
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> {
				InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) instruction;
				textifier.visitInvokeDynamicInsn(invokeDynamicInsnNode.name, invokeDynamicInsnNode.desc,
						invokeDynamicInsnNode.bsm, invokeDynamicInsnNode.bsmArgs);
			}
			case AbstractInsnNode.JUMP_INSN -> {
				JumpInsnNode jumpInsnNode = (JumpInsnNode) instruction;
				textifier.visitJumpInsn(opcode, jumpInsnNode.label.getLabel());
			}
			case AbstractInsnNode.LABEL -> {
				LabelNode labelNode = (LabelNode) instruction;
				textifier.visitLabel(labelNode.getLabel());
			}
			case AbstractInsnNode.LDC_INSN -> {
				LdcInsnNode ldcInsnNode = (LdcInsnNode) instruction;
				textifier.visitLdcInsn(ldcInsnNode.cst);
			}
			case AbstractInsnNode.IINC_INSN -> {
				IincInsnNode iincInsnNode = (IincInsnNode) instruction;
				textifier.visitIincInsn(iincInsnNode.var, iincInsnNode.incr);
			}
			case AbstractInsnNode.TABLESWITCH_INSN -> {
				TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) instruction;
				textifier.visitTableSwitchInsn(tableSwitchInsnNode.min, tableSwitchInsnNode.max,
						tableSwitchInsnNode.dflt.getLabel(),
						(Label[]) tableSwitchInsnNode.labels.stream().map(LabelNode::getLabel).toArray());
			}
			case AbstractInsnNode.LOOKUPSWITCH_INSN -> {
				LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) instruction;
				textifier.visitLookupSwitchInsn(lookupSwitchInsnNode.dflt.getLabel(),
						lookupSwitchInsnNode.keys.stream().mapToInt(i -> i).toArray(),
						(Label[]) lookupSwitchInsnNode.labels.stream().map(LabelNode::getLabel).toArray());
			}
			case AbstractInsnNode.MULTIANEWARRAY_INSN -> {
				MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) instruction;
				textifier.visitMultiANewArrayInsn(multiANewArrayInsnNode.desc, multiANewArrayInsnNode.dims);
			}
			default -> throw new UnsupportedOperationException("Unsupported instruction: " + instruction);
		}
		StringWriter writer = new StringWriter();
		textifier.print(new PrintWriter(writer));
		return writer.toString().trim();
	}
}
