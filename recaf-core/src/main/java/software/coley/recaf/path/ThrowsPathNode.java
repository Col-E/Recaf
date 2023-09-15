package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

/**
 * Path node for {@code throws} on {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class ThrowsPathNode extends AbstractPathNode<ClassMember, String> {
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
		super("throws", parent, String.class, type);
	}

	@Override
	public ClassMemberPathNode getParent() {
		return (ClassMemberPathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof ThrowsPathNode node) {
			return getValue().compareTo(node.getValue());
		}
		return 0;
	}
}
