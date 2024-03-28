package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.Set;

/**
 * Path node for {@code catch(Exception)} within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class CatchPathNode extends AbstractPathNode<ClassMember, String> {
	/**
	 * Type identifier for catch nodes.
	 */
	public static final String TYPE_ID = "catch";

	/**
	 * Node without parent.
	 *
	 * @param type
	 * 		Exception type.
	 */
	public CatchPathNode(@Nonnull String type) {
		this(null, type);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param type
	 * 		Exception type.
	 *
	 * @see ClassMemberPathNode#childCatch(String)
	 */
	public CatchPathNode(@Nullable ClassMemberPathNode parent, @Nonnull String type) {
		super(TYPE_ID, parent, type);
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

		if (o instanceof CatchPathNode node)
			return getValue().compareTo(node.getValue());
		else if (o instanceof LocalVariablePathNode || o instanceof InstructionPathNode)
			return -1;
		else if (o instanceof ThrowsPathNode)
			return 1;

		return 0;
	}
}
