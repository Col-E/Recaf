package software.coley.recaf.services.callgraph.resolver;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Lookup for workspace classes.
 *
 * @author Matt Coley
 */
public class ClassLookup {
	private final Workspace workspace;

	public ClassLookup(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	@Nullable
	public ClassInfo get(@Nullable String name) {
		if (name == null)
			return null;
		ClassPathNode classPath = workspace.findClass(name);
		if (classPath == null)
			return null;
		return classPath.getValue();
	}
}
