package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;

import java.util.Set;

/**
 * Path node for {@link FileInfo} types.
 *
 * @author Matt Coley
 */
public class FilePathNode extends AbstractPathNode<String, FileInfo> {
	/**
	 * Type identifier for file nodes.
	 */
	public static final String TYPE_ID = "file";

	/**
	 * Node without parent.
	 *
	 * @param info
	 * 		File value.
	 */
	public FilePathNode(@Nonnull FileInfo info) {
		this(null, info);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param info
	 * 		File value.
	 *
	 * @see DirectoryPathNode#child(FileInfo)
	 */
	public FilePathNode(@Nullable DirectoryPathNode parent, @Nonnull FileInfo info) {
		super(TYPE_ID, parent, FileInfo.class, info);
	}

	/**
	 * @param lineNo
	 * 		Line number to wrap into node.
	 *
	 * @return Path node of line number, with the current file as parent.
	 */
	@Nonnull
	public LineNumberPathNode child(int lineNo) {
		return new LineNumberPathNode(this, lineNo);
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
		if (o instanceof FilePathNode fileNode) {
			String name = getValue().getName();
			String otherName = fileNode.getValue().getName();
			return String.CASE_INSENSITIVE_ORDER.compare(name, otherName);
		}
		return 0;
	}
}
