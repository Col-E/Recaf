package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.util.function.Function;

/**
 * Lookup for convenience.
 *
 * @author Matt Coley
 */
public class ClassLookup implements Function<String, JvmClassInfo> {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull from.
	 */
	public ClassLookup(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public JvmClassInfo apply(String name) {
		if (name == null) return null;
		ClassPathNode classPath = workspace.findJvmClass(name);
		if (classPath == null) classPath = workspace.findLatestVersionedJvmClass(name);
		if (classPath == null) return null;
		return classPath.getValue().asJvmClass();
	}
}
