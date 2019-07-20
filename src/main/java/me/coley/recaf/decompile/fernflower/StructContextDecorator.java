package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.*;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

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
	 * 		{@link me.coley.recaf.decompile.fernflower.FernFlowerAccessor}.
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
	 * @throws ReflectiveOperationException
	 * 		Thrown if the parent loader could not be fetched.
	 */
	public void addWorkspace(Workspace workspace) throws IOException,
			ReflectiveOperationException {
		LazyLoader loader = getLoader();
		// Add primary resource classes
		addResource(workspace.getPrimary(), loader, true);
		for (JavaResource resource : workspace.getLibraries())
			addResource(resource, loader, false);
	}

	private void addResource(JavaResource resource, LazyLoader loader, boolean isPrimary) throws IOException {
		// Iterate resource class entries
		for(Map.Entry<String, byte[]> entry: resource.getClasses().entrySet()) {
			String name = entry.getKey();
			byte[] code = entry.getValue();
			// register class in the map and lazy-loader.
			getClasses().put(name, new StructClass(code, isPrimary, loader));
			loader.addClassLink(name, new LazyLoader.Link(null, name + ".class"));
		}
	}

	private LazyLoader getLoader() throws ReflectiveOperationException {
		// Hack to access private parent loader
		Field floader = StructContext.class.getDeclaredField("loader");
		floader.setAccessible(true);
		return (LazyLoader) floader.get(this);
	}
}
