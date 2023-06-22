package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

/**
 * Path node for local variables within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class LocalVariablePathNode extends AbstractPathNode<ClassMember, LocalVariable> {
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
		super("variable", parent, LocalVariable.class, variable);
	}

	@Override
	public ClassMemberPathNode getParent() {
		return (ClassMemberPathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof LocalVariablePathNode node) {
			LocalVariable value = getValue();
			LocalVariable otherValue = node.getValue();
			int cmp = Integer.compare(value.getIndex(), otherValue.getIndex());
			if (cmp == 0)
				cmp = value.getName().compareTo(otherValue.getName());
			if (cmp == 0)
				cmp = value.getDescriptor().compareTo(otherValue.getDescriptor());
			return cmp;
		}
		return 0;
	}
}
