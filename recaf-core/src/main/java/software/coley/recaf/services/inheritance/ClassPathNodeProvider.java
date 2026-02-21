package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

sealed interface ClassPathNodeProvider {

	@Nullable
	ClassPathNode getNode(@Nonnull String name);

	static ClassPathNodeProvider cache(Workspace workspace) {
		Stream<ClassPathNode> stream = workspace.classesStream();
		Map<String, ClassPathNode> nodes = new HashMap<>(4096);
		stream.forEach(classPathNode -> {
			nodes.putIfAbsent(classPathNode.getValue().getName(), classPathNode);
		});
		return new Cached(Map.copyOf(nodes));
	}

	record FromWorkspace(@Nonnull Workspace workspace) implements ClassPathNodeProvider {
		@Nullable
		@Override
		public ClassPathNode getNode(@Nonnull String name) {
			return workspace.findClass(name);
		}
	}

	record Cached(Map<String, ClassPathNode> nodes) implements ClassPathNodeProvider {
		@Nullable
		@Override
		public ClassPathNode getNode(@Nonnull String name) {
			return nodes.get(name);
		}
	}
}
