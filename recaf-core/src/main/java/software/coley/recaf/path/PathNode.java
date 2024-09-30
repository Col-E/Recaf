package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A <i>"modular"</i> value type for representing <i>"paths"</i> to content in a {@link Workspace}.
 * The path must contain all data in a <i>"chain"</i> such that it can have access from most specific portion
 * all the way up to the {@link Workspace} portion.
 * <p/>
 * <b>NOTE: Regarding contents in embedded resources,</b> the path result of the methods like
 * {@link Workspace#findClass(String)} will contain the root {@link WorkspaceResource} but the exact {@link Bundle}.
 * To find the exact embedded resource of a result use {@link WorkspaceResource#resolveBundleContainer(Bundle)}.
 *
 * @param <V>
 * 		Path value type.
 *
 * @author Matt Coley
 */
public interface PathNode<V> extends Comparable<PathNode<?>> {
	/**
	 * The parent node of this node. This value does not have to be present in the actual UI model.
	 * The parent linkage is so that child types like {@link ClassPathNode} can access their full scope,
	 * including their containing {@link DirectoryPathNode package}, {@link BundlePathNode bundle},
	 * {@link ResourcePathNode resource}, and {@link WorkspacePathNode workspace}.
	 * <br>
	 * This allows child-types such as {@link ClassPathNode} to be passed around to consuming APIs and retain access
	 * to the mentioned scoped values.
	 *
	 * @return Parent node.
	 *
	 * @see #getValueOfType(Class) Used by child-types to look up values in themselves, and their parents.
	 */
	@Nullable
	@SuppressWarnings("rawtypes")
	PathNode getParent();

	/**
	 * Creates a copy of the path node with this child-most node's value being looked up for a newer
	 * value in the associated workspace.
	 * <p/>
	 * <b>Note:</b> A {@link WorkspacePathNode} must be present.
	 *
	 * @return A new path node pointing to the same location,
	 * but with the {@link #getValue() value} being updated to the current in the associated {@link Workspace}.
	 * If a lookup could not be done then the current instance is returned.
	 */
	@Nonnull
	@SuppressWarnings("rawtypes")
	default PathNode withCurrentWorkspaceContent() {
		return this;
	}

	/**
	 * @return Wrapped value.
	 */
	@Nonnull
	V getValue();

	/**
	 * @param other
	 * 		Some other path node.
	 *
	 * @return {@code true} when the other path has the same {@link #getValue() local value}.
	 */
	default boolean hasEqualOrChildValue(@Nonnull PathNode<?> other) {
		return this == other || getValue().equals(other.getValue());
	}

	/**
	 * Used to differentiate path nodes in a chain that have the same {@link #getValueType()}.
	 *
	 * @return String unique ID per path-node type.
	 */
	@Nonnull
	String typeId();

	/**
	 * @param node
	 * 		Other node to check.
	 *
	 * @return {@code true} when the current {@link #typeId()} is the same as the other's ID.
	 */
	default boolean typeIdMatch(@Nonnull PathNode<?> node) {
		return typeId().equals(node.typeId());
	}

	/**
	 * @return Set of expected {@link #typeId()} values for {@link #getParent() parent nodes}.
	 */
	@Nonnull
	Set<String> directParentTypeIds();

	/**
	 * @return The type of this path node's {@link #getValue() wrapped value}.
	 */
	@Nonnull
	Class<V> getValueType();

	/**
	 * @param type
	 * 		Some type contained in the full path.
	 * 		This includes the current {@link PathNode} and any {@link #getParent() parent}.
	 * @param <T>
	 * 		Implied value type.
	 * @param <I>
	 * 		Implied path node implementation type.
	 *
	 * @return Node in the path holding a value of the given type.
	 *
	 * @see #getValueOfType(Class) Get the direct value of the parent node.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	default <T, I extends PathNode<? extends T>> I getPathOfType(@Nonnull Class<T> type) {
		if (type.isAssignableFrom(getValueType()))
			return (I) this;
		PathNode<?> parent = getParent();
		if (parent == null) return null;
		return parent.getPathOfType(type);
	}

	/**
	 * @param type
	 * 		Some type contained in the full path.
	 * 		This includes the current {@link PathNode} and any {@link #getParent() parent}.
	 * @param <T>
	 * 		Implied value type.
	 *
	 * @return Instance of value from the path, or {@code null} if not found in this path.
	 *
	 * @see #getPathOfType(Class) Get the containing {@link PathNode} instead of the direct value.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	default <T> T getValueOfType(@Nonnull Class<T> type) {
		if (type.isAssignableFrom(getValueType()))
			return (T) getValue();
		PathNode<?> parent = getParent();
		if (parent == null) return null;
		return parent.getValueOfType(type);
	}

	/**
	 * @param type
	 * 		Some type contained in the full path.
	 * 		This includes the current {@link PathNode} and any {@link #getParent() parent}.
	 * @param action
	 * 		Action to run on the discovered value in the path.
	 * 		If no value is found, the action is not run.
	 * @param <T>
	 * 		Implied value type.
	 *
	 * @return {@code true} when a matching value was found and the action was run.
	 */
	default <T> boolean onValue(@Nonnull Class<T> type, @Nonnull Consumer<T> action) {
		T value = getValueOfType(type);
		if (value != null) {
			action.accept(value);
			return true;
		}
		return false;
	}

	/**
	 * @param type
	 * 		Some {@link PathNode} type.
	 * @param action
	 * 		Action to run on the discovered path node.
	 * 		If no path node is found, the action is not run.
	 * @param <T>
	 * 		Implied path type.
	 *
	 * @return {@code true} when a matching path node was found and the action was run.
	 */
	@SuppressWarnings("unchecked")
	default <T extends PathNode<?>> boolean onPath(@Nonnull Class<T> type, @Nonnull Consumer<T> action) {
		if (type == getClass()) {
			action.accept((T) this);
			return true;
		} else {
			PathNode<?> parent = getParent();
			if (parent != null)
				return parent.onPath(type, action);
			return false;
		}
	}

	/**
	 * Checks for tree alignment. Consider this simple example:
	 * <pre>
	 *   Path1   Path2   Path3
	 *     A       A       A
	 *     |       |       |
	 *     B       B       B
	 *     |       |       |
	 *     C       C       X
	 * </pre>
	 * With this setup:
	 * <ul>
	 *     <li>{@code path1C.allParentsMatch(path1C) == true} Self checks are equal</li>
	 *     <li>{@code path1C.allParentsMatch(path2C) == true} Two identical paths <i>(by value of each node)</i> are equal</li>
	 *     <li>{@code path1C.allParentsMatch(path2B) == false} Comparing between non-parallel levels are not equal</li>
	 *     <li>{@code path1C.allParentsMatch(path3X) == false} Paths to different items are not equal</li>
	 * </ul>
	 *
	 * @param other
	 * 		Some other path node.
	 *
	 * @return {@code true} when from this level all parents going up the path match values.
	 */
	default boolean allParentsMatch(@Nonnull PathNode<?> other) {
		// Type identifiers should match for all levels.
		if (!typeId().equals(other.typeId()))
			return false;

		// Should both have the same level of tree heights (number of parents).
		PathNode<?> parent = getParent();
		PathNode<?> otherParent = other.getParent();
		if (parent == null && otherParent == null)
			// Root node edge case
			return hasEqualOrChildValue(other);
		else if (parent == null || otherParent == null)
			// Mismatch in tree structure height
			return false;

		// Go up the chain if the matching values continue.
		if (hasEqualOrChildValue(other))
			return parent.allParentsMatch(otherParent);
		return false;
	}

	/**
	 * @param other
	 * 		Some other path node.
	 *
	 * @return {@code true} when our path represents a more generic path than the given one.
	 * {@code false} when our path does not belong to parent path of the given item.
	 */
	default boolean isParentOf(@Nonnull PathNode<?> other) {
		return other.isDescendantOf(this);
	}

	/**
	 * @param other
	 * 		Some other path node.
	 *
	 * @return {@code true} when our path represents a more specific path than the given one.
	 * {@code false} when our path does not belong to a potential sub-path of the given item.
	 */
	default boolean isDescendantOf(@Nonnull PathNode<?> other) {
		// If our type identifiers are the same everything going up the path should match.
		String otherTypeId = other.typeId();
		if (otherTypeId.equals(typeId()))
			return hasEqualOrChildValue(other) && allParentsMatch(other);

		// Check if the other is an allowed parent.
		PathNode<?> parent = getParent();
		if (directParentTypeIds().contains(otherTypeId) && parent != null) {
			// The parent is an allowed type, check if the parent says it is a descendant of the other path.
			if (parent == other || (parent.hasEqualOrChildValue(other)))
				return parent.isDescendantOf(other);
		}

		// Check in parent.
		if (parent != null)
			return parent.isDescendantOf(other);

		// Not a descendant.
		return false;
	}

	/**
	 * @param o
	 * 		Some other path node.
	 *
	 * @return Comparison for visual sorting purposes.
	 */
	int localCompare(PathNode<?> o);
}
