package software.coley.recaf.path;

import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;

/**
 * Path node for line numbers within text {@link FileInfo} instances.
 *
 * @author Matt Coley
 */
public class LineNumberPathNode extends AbstractPathNode<FileInfo, Integer> {
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
		super("line", parent, Integer.class, line);
	}

	@Override
	public FilePathNode getParent() {
		return (FilePathNode) super.getParent();
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof LineNumberPathNode lineNode) {
			return getValue().compareTo(lineNode.getValue());
		}
		return 0;
	}
}
