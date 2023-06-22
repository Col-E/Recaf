package software.coley.recaf.services.callgraph;

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
	public ClassLookup(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public JvmClassInfo apply(String name) {
		ClassPathNode classPath = workspace.findJvmClass(name);
		if (classPath == null) return null;
		return classPath.getValue().asJvmClass();
	}
}
