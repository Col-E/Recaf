package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.Set;

/**
 * Path node for Dalvik instructions within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class AndroidInstructionPathNode extends AbstractPathNode<ClassMember, Instruction> {
	/**
	 * Type identifier for instruction nodes.
	 */
	public static final String TYPE_ID = "dex-instruction";
	private final int index;

	/**
	 * Node without parent.
	 *
	 * @param insn
	 * 		Instruction value.
	 * @param index
	 * 		Index of the instruction within the method code.
	 */
	public AndroidInstructionPathNode(@Nonnull Instruction insn, int index) {
		this(null, insn, index);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param insn
	 * 		Instruction value.
	 * @param index
	 * 		Index of the instruction within the method code.
	 *
	 * @see ClassMemberPathNode#childInsn(Instruction, int)
	 */
	public AndroidInstructionPathNode(@Nullable ClassMemberPathNode parent, @Nonnull Instruction insn, int index) {
		super(TYPE_ID, parent, insn);
		this.index = index;
	}

	/**
	 * @return Index of the instruction within the method code <i>(As determined by ASM)</i>.
	 */
	public int getInstructionIndex() {
		return index;
	}

	@Override
	public ClassMemberPathNode getParent() {
		return (ClassMemberPathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(ClassMemberPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof AndroidInstructionPathNode node)
			return Integer.compare(index, node.index);
		else if (o instanceof ThrowsPathNode || o instanceof CatchPathNode || o instanceof LocalVariablePathNode)
			return 1;

		return 0;
	}
}
