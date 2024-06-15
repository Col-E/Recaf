package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;

import java.util.Set;

/**
 * Path node for {@link InnerClassInfo} types.
 *
 * @author Matt Coley
 */
public class InnerClassPathNode extends AbstractPathNode<ClassInfo, InnerClassInfo> {
	/**
	 * Type identifier for inner class nodes.
	 */
	public static final String TYPE_ID = "inner-class";

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
		super(TYPE_ID, parent, innerClass);
	}

	/**
	 * @param annotation
	 * 		Annotation to wrap into node.
	 *
	 * @return Path node of annotation, with the current inner class as parent.
	 */
	@Nonnull
	public AnnotationPathNode child(@Nonnull AnnotationInfo annotation) {
		return new AnnotationPathNode(this, annotation);
	}

	@Override
	public ClassPathNode getParent() {
		return (ClassPathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(ClassPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof InnerClassPathNode innerClassPathNode) {
			String name = getValue().getInnerClassName();
			String otherName = innerClassPathNode.getValue().getInnerClassName();
			return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(name, otherName);
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
