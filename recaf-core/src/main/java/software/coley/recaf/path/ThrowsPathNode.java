package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.Set;

/**
 * Path node for {@code throws} on {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class ThrowsPathNode extends AbstractPathNode<ClassMember, String> {
	/**
	 * Type identifier for throws nodes.
	 */
	public static final String TYPE_ID = "throws";

	/**
	 * Node without parent.
	 *
	 * @param type
	 * 		Thrown type.
	 */
	public ThrowsPathNode(@Nonnull String type) {
		this(null, type);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param type
	 * 		Thrown type.
	 *
	 * @see ClassMemberPathNode#childThrows(String)
	 */
	public ThrowsPathNode(@Nullable ClassMemberPathNode parent, @Nonnull String type) {
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

		if (o instanceof ThrowsPathNode node)
			return getValue().compareTo(node.getValue());
		else if (o instanceof LocalVariablePathNode || o instanceof InstructionPathNode || o instanceof CatchPathNode)
			return -1;

		return 0;
	}
}
