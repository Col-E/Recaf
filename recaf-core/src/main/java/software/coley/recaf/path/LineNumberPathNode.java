package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;

import java.util.Set;

/**
 * Path node for line numbers within text {@link FileInfo} instances.
 *
 * @author Matt Coley
 */
public class LineNumberPathNode extends AbstractPathNode<FileInfo, Integer> {
	/**
	 * Type identifier for line number nodes.
	 */
	public static final String TYPE_ID = "line";

	/**
	 * Node without parent.
	 *
	 * @param line
	 * 		Line number value.
	 */
	public LineNumberPathNode(int line) {
		this(null, line);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param line
	 * 		Line number value.
	 *
	 * @see FilePathNode#child(int)
	 */
	public LineNumberPathNode(@Nullable FilePathNode parent, int line) {
		super(TYPE_ID, parent, line);
	}

	@Override
	public FilePathNode getParent() {
		return (FilePathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(FilePathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof LineNumberPathNode lineNode) {
			int i = getValue().compareTo(lineNode.getValue());
			if (i == 0) {
				// Fall back to parent file comparison if the local line numbers are the same.
				// Not ideal, but we can't validate anything else here.
				FilePathNode parent = getParent();
				FilePathNode otherParent = lineNode.getParent();
				if (parent != null && otherParent != null)
					i = parent.localCompare(otherParent);
			}
			return i;
		}

		return 0;
	}
}
