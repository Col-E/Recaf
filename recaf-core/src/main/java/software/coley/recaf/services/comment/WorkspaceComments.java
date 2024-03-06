package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

/**
 * Outline of a container for commented elements in a workspace.
 *
 * @author Matt Coley
 */
public interface WorkspaceComments extends Iterable<ClassComments> {
	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return Comments container for the class, creating a new container if none exist.
	 */
	@Nonnull
	ClassComments getOrCreateClassComments(@Nonnull ClassPathNode classPath);

	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return Comments container for the class, if comments exist for the class. Otherwise {@code null}.
	 */
	@Nullable
	ClassComments getClassComments(@Nonnull ClassPathNode classPath);

	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return The removed comments container for the class, or {@code null} if no comments previously existed.
	 */
	@Nullable
	ClassComments deleteClassComments(@Nonnull ClassPathNode classPath);

	/**
	 * @param path
	 * 		Class or member path within a workspace.
	 *
	 * @return Class or member comment, if any is associated with the path.
	 */
	@Nullable
	default String getComment(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath)
			return getClassComment(classPath);
		else if (path instanceof ClassMemberPathNode memberPath)
			return getMemberComment(memberPath);
		return null;
	}

	/**
	 * @param classPath
	 * 		Class path within a workspace.
	 *
	 * @return Class comment, if any is associated with the path.
	 */
	@Nullable
	default String getClassComment(@Nonnull ClassPathNode classPath) {
		ClassComments classComments = getClassComments(classPath);
		if (classComments == null)
			return null;
		return classComments.getClassComment();
	}

	/**
	 * @param memberPath
	 * 		Member path within a workspace.
	 *
	 * @return Member comment, if any is associated with the path.
	 */
	@Nullable
	default String getMemberComment(@Nonnull ClassMemberPathNode memberPath) {
		ClassPathNode classPath = memberPath.getParent();
		if (classPath == null)
			return null;

		ClassComments classComments = getClassComments(classPath);
		if (classComments == null)
			return null;

		ClassMember member = memberPath.getValue();
		if (member.isField())
			return classComments.getFieldComment(member.getName(), member.getDescriptor());
		else
			return classComments.getMethodComment(member.getName(), member.getDescriptor());
	}
}
