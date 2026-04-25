package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider of class path nodes.
 *
 * @author xDark
 */
sealed interface ClassPathNodeProvider {
	/**
	 * @param name
	 * 		Class name to look up.
	 *
	 * @return Path node for the class with the given name, or {@code null} if no such class exists in the provider.
	 */
	@Nullable
	ClassPathNode getNode(@Nonnull String name);

	/**
	 * Create a cached provider that contains all nodes from the workspace at the time of creation.
	 *
	 * @param workspace
	 * 		Workspace to cache nodes from.
	 *
	 * @return Provider that caches all nodes from the workspace at the time of creation.
	 */
	@Nonnull
	static ClassPathNodeProvider.Cached cache(@Nonnull Workspace workspace) {
		Map<String, ClassPathNode> nodes = new HashMap<>((int) workspace.classesStream().count());
		workspace.classesStream().forEach(path -> nodes.putIfAbsent(path.getValue().getName(), path));
		return new Cached(nodes);
	}

	/**
	 * Provider that looks up nodes directly from the workspace.
	 * This is not recommended for repeated lookups, but it is useful for one-off lookups or when the workspace is expected to be changing frequently.
	 *
	 * @param workspace
	 * 		Workspace to look up nodes from.
	 */
	record Live(@Nonnull Workspace workspace) implements ClassPathNodeProvider {
		@Nullable
		@Override
		public ClassPathNode getNode(@Nonnull String name) {
			return workspace.findClass(name);
		}
	}

	/**
	 * Provider that caches all nodes from the workspace at the time of creation.
	 * This is recommended for repeated lookups, but it is not suitable for workspaces that are expected to be changing frequently.
	 *
	 * @param nodes
	 * 		Map of class names to their corresponding path nodes. This map is expected to be immutable.
	 *
	 * @see #cache(Workspace)
	 */
	record Cached(@Nonnull Map<String, ClassPathNode> nodes) implements ClassPathNodeProvider {
		int size() {
			return nodes.size();
		}

		@Nullable
		@Override
		public ClassPathNode getNode(@Nonnull String name) {
			return nodes.get(name);
		}
	}
}
