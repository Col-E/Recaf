package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;

/**
 * Path node for {@link InnerClassInfo} types.
 *
 * @author Matt Coley
 */
public class InnerClassPathNode extends AbstractPathNode<ClassInfo, InnerClassInfo> {
	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Optional parent node.
	 * @param innerClass
	 * 		Inner class instance.
	 *
	 * @see ClassPathNode#child(InnerClassInfo)
	 */
	public InnerClassPathNode(@Nullable ClassPathNode parent,
							  @Nonnull InnerClassInfo innerClass) {
		super("inner-class", parent, InnerClassInfo.class, innerClass);
	}

	@Override
	public ClassPathNode getParent() {
		return (ClassPathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof InnerClassPathNode innerClassPathNode) {
			String name = getValue().getInnerClassName();
			String otherName = innerClassPathNode.getValue().getInnerClassName();
			return String.CASE_INSENSITIVE_ORDER.compare(name, otherName);
		}

		// Show before members
		if (o instanceof ClassMemberPathNode)
			return -1;

		// Show after annos
		if (o instanceof AnnotationPathNode)
			return 1;

		return 0;
	}
}
