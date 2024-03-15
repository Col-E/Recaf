package software.coley.recaf.path;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Set;

/**
 * Path node for an {@link AssemblerPathData} associated with an assembler in the UI.
 *
 * @author Matt Coley
 */
public class AssemblerPathNode extends AbstractPathNode<Object, AssemblerPathData> {
	/**
	 * Type identifier for assembler nodes.
	 */
	public static final String TYPE_ID = "assembler";

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param data
	 * 		Assembler data.
	 */
	@SuppressWarnings("unchecked")
	public AssemblerPathNode(@Nonnull PathNode<?> parent, @Nonnull AssemblerPathData data) {
		super(TYPE_ID, (PathNode<Object>) parent, data);
	}

	@Nonnull
	@Override
	public PathNode<Object> getParent() {
		return Objects.requireNonNull(super.getParent());
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(ClassPathNode.TYPE_ID, ClassMemberPathNode.TYPE_ID, AssemblerPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		PathNode<?> otherParent = o.getParent();
		if (otherParent == null)
			return 1;
		return getParent().compareTo(otherParent);
	}
}
