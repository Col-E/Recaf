package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;

import java.util.Set;

/**
 * Path node for {@link ClassInfo} types.
 *
 * @author Matt Coley
 */
public class ClassPathNode extends AbstractPathNode<String, ClassInfo> {
	/**
	 * Type identifier for class nodes.
	 */
	public static final String TYPE_ID = "class";

	/**
	 * Node without parent.
	 *
	 * @param info
	 * 		Class value.
	 */
	public ClassPathNode(@Nonnull ClassInfo info) {
		this(null, info);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param info
	 * 		Class value.
	 *
	 * @see DirectoryPathNode#child(ClassInfo)
	 */
	public ClassPathNode(@Nullable DirectoryPathNode parent, @Nonnull ClassInfo info) {
		super(TYPE_ID, parent, info);
	}

	/**
	 * @param name
	 * 		Field or method name in the current class.
	 * @param desc
	 * 		Field or method descriptor in the current class.
	 *
	 * @return Path node of member, with the current class as parent.
	 * {@code null} if a field or method with the given name/descriptor could not be found.
	 */
	@Nullable
	public ClassMemberPathNode child(@Nonnull String name, @Nonnull String desc) {
		ClassInfo classInfo = getValue();
		ClassMember member;
		if (!desc.isEmpty() && desc.charAt(0) == '(')
			member = classInfo.getDeclaredMethod(name, desc);
		else
			member = classInfo.getDeclaredField(name, desc);

		if (member != null) return child(member);

		return null;
	}

	/**
	 * @param member
	 * 		Member to wrap into node.
	 *
	 * @return Path node of member, with the current class as parent.
	 */
	@Nonnull
	public ClassMemberPathNode child(@Nonnull ClassMember member) {
		return new ClassMemberPathNode(this, member);
	}

	/**
	 * @param innerClass
	 * 		Inner class to wrap into node.
	 *
	 * @return Path node of inner class, with the current class as parent.
	 */
	@Nonnull
	public InnerClassPathNode child(@Nonnull InnerClassInfo innerClass) {
		return new InnerClassPathNode(this, innerClass);
	}

	/**
	 * @param annotation
	 * 		Annotation to wrap into node.
	 *
	 * @return Path node of annotation, with the current member as parent.
	 */
	@Nonnull
	public AnnotationPathNode child(@Nonnull AnnotationInfo annotation) {
		return new AnnotationPathNode(this, annotation);
	}

	@Nonnull
	@Override
	public ClassPathNode withCurrentWorkspaceContent() {
		DirectoryPathNode parent = getParent();
		if (parent == null) return this;
		ClassBundle<?> bundle = getValueOfType(ClassBundle.class);
		if (bundle == null) return this;
		ClassInfo classInfo = bundle.get(getValue().getName());
		if (classInfo == null || classInfo == getValue()) return this;
		return parent.child(classInfo);
	}

	@Override
	public boolean hasEqualOrChildValue(@Nonnull PathNode<?> other) {
		if (other instanceof ClassPathNode otherClassPath) {
			ClassInfo cls = getValue();
			ClassInfo otherCls = otherClassPath.getValue();

			// We'll determine equality just by the name of the contained class.
			// Path equality should match by location, so comparing just by name
			// allows this path and the other path to have different versions of
			// the same class.
			return cls.getName().equals(otherCls.getName());
		}

		return false;
	}

	@Override
	public DirectoryPathNode getParent() {
		return (DirectoryPathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(DirectoryPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof ClassPathNode classPathNode) {
			String name = getValue().getName();
			String otherName = classPathNode.getValue().getName();
			return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(name, otherName);
		}
		return 0;
	}
}
