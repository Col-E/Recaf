package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.FileBundle;

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
		super(TYPE_ID, parent, info);
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

	@Nonnull
	@Override
	public FilePathNode withCurrentWorkspaceContent() {
		DirectoryPathNode parent = getParent();
		if (parent == null) return this;
		FileBundle bundle = getValueOfType(FileBundle.class);
		if (bundle == null) return this;
		FileInfo fileInfo = bundle.get(getValue().getName());
		if (fileInfo == null || fileInfo == getValue()) return this;
		return parent.child(fileInfo);
	}

	@Override
	public boolean hasEqualOrChildValue(@Nonnull PathNode<?> other) {
		if (other instanceof FilePathNode otherClassPathNode) {
			FileInfo cls = getValue();
			FileInfo otherCls = otherClassPathNode.getValue();

			// We'll determine equality just by the name of the contained file.
			// Path equality should match by location, so comparing just by name
			// allows this path and the other path to have different versions of
			// the same file.
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

		if (o instanceof FilePathNode fileNode) {
			String name = getValue().getName();
			String otherName = fileNode.getValue().getName();
			return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(name, otherName);
		}
		return 0;
	}
}
