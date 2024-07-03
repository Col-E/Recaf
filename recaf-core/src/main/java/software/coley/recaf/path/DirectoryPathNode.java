package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.util.Set;

/**
 * Path node for packages of {@link ClassInfo} and directories of {@link FileInfo} types.
 *
 * @author Matt Coley
 */
@SuppressWarnings("rawtypes")
public class DirectoryPathNode extends AbstractPathNode<Bundle, String> {
	/**
	 * Type identifier for directory nodes.
	 */
	public static final String TYPE_ID = "directory";

	/**
	 * Node without parent.
	 *
	 * @param directory
	 * 		Directory name.
	 */
	public DirectoryPathNode(@Nonnull String directory) {
		this(null, directory);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param directory
	 * 		Directory name.
	 *
	 * @see BundlePathNode#child(String)
	 */
	public DirectoryPathNode(@Nullable BundlePathNode parent, @Nonnull String directory) {
		super(TYPE_ID, parent, directory);
	}

	/**
	 * @param directory
	 * 		New directory name.
	 *
	 * @return New node with same parent, but different directory name value.
	 */
	@Nonnull
	public DirectoryPathNode withDirectory(@Nonnull String directory) {
		return new DirectoryPathNode(getParent(), directory);
	}

	/**
	 * @param classInfo
	 * 		Class to wrap into node.
	 *
	 * @return Path node of class, with the current package as parent.
	 */
	@Nonnull
	public ClassPathNode child(@Nonnull ClassInfo classInfo) {
		return new ClassPathNode(this, classInfo);
	}

	/**
	 * @param fileInfo
	 * 		File to wrap into node.
	 *
	 * @return Path node of file, with the current directory as parent.
	 */
	@Nonnull
	public FilePathNode child(@Nonnull FileInfo fileInfo) {
		return new FilePathNode(this, fileInfo);
	}

	@Override
	@SuppressWarnings("all")
	public BundlePathNode getParent() {
		return (BundlePathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(BundlePathNode.TYPE_ID, DirectoryPathNode.TYPE_ID);
	}

	@Override
	public boolean hasEqualOrChildValue(@Nonnull PathNode<?> other) {
		if (other instanceof DirectoryPathNode otherDirectory) {
			String dir = getValue();
			String maybeParentDir = otherDirectory.getValue();

			// We cannot do just a basic 'startsWith' check on the path values since they do not
			// end with a trailing slash. This could lead to cases where:
			//  'co' is a parent value of 'com/foo'
			//
			// By doing an equals check, we allow for 'co' vs 'com' to fail but 'co' vs 'co' to pass,
			// and the following startsWith check with a slash allows us to not fall to the suffix issue described above.
			return dir.equals(maybeParentDir) || dir.startsWith(maybeParentDir + "/");
		}

		return super.hasEqualOrChildValue(other);
	}

	@Override
	public boolean isDescendantOf(@Nonnull PathNode<?> other) {
		// Descendant check comparing between directories will check for containment within the local value's path.
		// This way 'a/b/c' is seen as a descendant of 'a/b'.
		if (typeId().equals(other.typeId()))
			return hasEqualOrChildValue(other) && allParentsMatch(other);

		return super.isDescendantOf(other);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof DirectoryPathNode pathNode) {
			String name = getValue();
			String otherName = pathNode.getValue();
			return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(name, otherName);
		}
		return 0;
	}
}
