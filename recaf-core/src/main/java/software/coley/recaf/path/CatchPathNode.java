package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;

/**
 * Path node for {@code catch(Exception)} within {@link MethodMember} instances.
 *
 * @author Matt Coley
 */
public class CatchPathNode extends AbstractPathNode<ClassMember, String> {
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
		super("catch", parent, String.class, type);
	}

	@Override
	public ClassMemberPathNode getParent() {
		return (ClassMemberPathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof CatchPathNode node) {
			return getValue().compareTo(node.getValue());
		}
		return 0;
	}
}
