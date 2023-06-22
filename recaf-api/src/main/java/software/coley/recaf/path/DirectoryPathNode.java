package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.Bundle;

/**
 * Path node for packages of {@link ClassInfo} and directories of {@link FileInfo} types.
 *
 * @author Matt Coley
 */
@SuppressWarnings("rawtypes")
public class DirectoryPathNode extends AbstractPathNode<Bundle, String> {
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
		super("directory", parent, String.class, directory);
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
	 * @return Path node of class, with current package as parent.
	 */
	@Nonnull
	public ClassPathNode child(@Nonnull ClassInfo classInfo) {
		return new ClassPathNode(this, classInfo);
	}

	/**
	 * @param fileInfo
	 * 		File to wrap into node.
	 *
	 * @return Path node of file, with current directory as parent.
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

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof DirectoryPathNode classNode) {
			String name = getValue();
			String otherName = classNode.getValue();
			return String.CASE_INSENSITIVE_ORDER.compare(name, otherName);
		}
		return 0;
	}
}
