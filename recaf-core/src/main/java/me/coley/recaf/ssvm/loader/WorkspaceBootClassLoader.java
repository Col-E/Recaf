package me.coley.recaf.ssvm.loader;

import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.ClassParseResult;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Loader that pulls classes from {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceBootClassLoader implements BootClassLoader {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceBootClassLoader(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public ClassParseResult findBootClass(String name) {
		// Pull class from workspace
		ClassInfo info = workspace.getResources().getClass(name);
		if (info == null)
			return null;
		// Load into result
		ClassReader reader = info.getClassReader();
		ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.SKIP_FRAMES);
		return new ClassParseResult(reader, node);
	}
}
