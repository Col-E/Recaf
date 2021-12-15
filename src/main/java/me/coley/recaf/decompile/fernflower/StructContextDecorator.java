package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.IDecompiledData;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;

import java.io.IOException;
import java.util.Map;

import static me.coley.recaf.util.CollectionUtil.copySet;

/**
 * Decorator for StructContext to support Recaf workspaces.
 *
 * @author Matt
 */
public class StructContextDecorator extends StructContext {
	/**
	 * Constructs a StructContext.
	 *
	 * @param saver
	 * 		Result saver <i>(Unused/noop)</i>
	 * @param data
	 * 		Data instance, should be an instance of
	 *        {@link me.coley.recaf.decompile.fernflower.FernFlowerAccessor}.
	 * @param loader
	 * 		LazyLoader to hold links to class resources.
	 */
	public StructContextDecorator(IResultSaver saver, IDecompiledData data, LazyLoader loader) {
		super(saver, data, loader);
	}

	/**
	 * @param workspace
	 * 		Recaf workspace to pull classes from.
	 *
	 * @throws IOException
	 * 		Thrown if a class cannot be read.
	 * @throws IndexOutOfBoundsException
	 * 		Thrown if FernFlower can't read the class.
	 * 		<i>(IE: It fails on newer Java class files)</i>
	 */
	public void addWorkspace(Workspace workspace) throws IOException {
		// Add primary resource classes
		addResource(workspace.getPrimary());
		for (JavaResource resource : workspace.getLibraries())
			addResource(resource);
	}

	private void addResource(JavaResource resource) throws IOException {
		// Iterate resource class entries
		for (Map.Entry<String, byte[]> entry : copySet(resource.getClasses().entrySet())) {
			String name = entry.getKey();
			String simpleName = name.substring(name.lastIndexOf('/') + 1);
			byte[] code = entry.getValue();
			addData(name, simpleName, code, true);
		}
	}
}
