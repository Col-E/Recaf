package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

import java.util.Set;

/**
 * Path node for local variables within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class LocalVariablePathNode extends AbstractPathNode<ClassMember, LocalVariable> {
	/**
	 * Type identifier for local variable nodes.
	 */
	public static final String TYPE_ID = "variable";

	/**
	 * Node without parent.
	 *
	 * @param variable
	 * 		Variable value.
	 */
	public LocalVariablePathNode(@Nonnull LocalVariable variable) {
		this(null, variable);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param variable
	 * 		Variable value.
	 *
	 * @see ClassMemberPathNode#childVariable(LocalVariable)
	 */
	public LocalVariablePathNode(@Nullable ClassMemberPathNode parent, @Nonnull LocalVariable variable) {
		super(TYPE_ID, parent, variable);
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

		if (o instanceof LocalVariablePathNode node) {
			LocalVariable value = getValue();
			LocalVariable otherValue = node.getValue();
			int cmp = Integer.compare(value.getIndex(), otherValue.getIndex());
			if (cmp == 0)
				cmp = value.getName().compareTo(otherValue.getName());
			if (cmp == 0)
				cmp = value.getDescriptor().compareTo(otherValue.getDescriptor());
			return cmp;
		} else if (o instanceof ThrowsPathNode || o instanceof CatchPathNode)
			return 1;
		else if (o instanceof InstructionPathNode)
			return -1;

		return 0;
	}
}
