package me.coley.recaf.workspace;

import me.coley.recaf.control.Controller;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.*;
import java.lang.module.Module;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Importable instrumentation resource.
 *
 * @author Matt
 */
public class InstrumentationResource extends JavaResource {
	public static Instrumentation instrumentation;
	private static InstrumentationResource instance;

	/**
	 * Constructs an instrumentation resource.
	 *
	 * @throws IllegalStateException
	 * 		When the {@link #instrumentation} instance has not been set.
	 * @throws IOException
	 * 		When querying for runtime classes fails.
	 */
	private InstrumentationResource() throws IllegalStateException, IOException {
		super(ResourceKind.INSTRUMENTATION);
		if(instrumentation == null)
			throw new IllegalStateException("Instrumentation has not been initialized!");
		if (instance != null)
			throw new IllegalStateException("There already is an instrumentation resource!");
		instance = this;
		setSkippedPrefixes(Arrays.asList("java/", "javax/", "javafx/", "sun/",
				"com/sun/", "com/oracle/", "jdk/", "me/coley/"));
	}

	/**
	 * Setup an instrumentation based workspace.
	 *
	 * @param controller Controller to act on.
	 *
	 * @return Created workspace.
	 */
	public static Workspace setup(Controller controller) {
		try {
			// Add transformer to add new classes to the map
			instrumentation.addTransformer(new InstrumentationResourceTransformer());
			// Set workspace
			controller.setWorkspace(new Workspace(getInstance()));
			Log.info("Loaded instrumentation workspace");
		} catch(Exception ex) {
			Log.error(ex, "Failed to initialize instrumentation");
		}
		return controller.getWorkspace();
	}

	/**
	 * Saves changed by retransforming classes.
	 *
	 * @throws ClassNotFoundException
	 * 		When the modified class couldn't be found.
	 * @throws UnmodifiableClassException
	 * 		When the modified class is not allowed to be modified.
	 * @throws ClassFormatError
	 * 		When the modified class is not valid.
	 */
	public void save() throws ClassNotFoundException, UnmodifiableClassException, ClassFormatError {
		// Classes to update
		Set<String> dirty = new HashSet<>(getDirtyClasses());
		Log.info("Preparing to redefine {} classes", dirty.size());
		ClassDefinition[] definitions = new ClassDefinition[dirty.size()];
		int i = 0;
		for (String name : dirty) {
			String clsName = name.replace('/', '.');
			Class<?> cls = Class.forName(clsName, false, ClasspathUtil.scl);
			byte[] value = getClasses().get(name);
			if (value == null)
				throw new IllegalStateException("Failed to fetch code for class: " + name);
			definitions[i] = new ClassDefinition(cls, value);
			i++;
		}
		// Apply new definitions
		instrumentation.redefineClasses(definitions);
		// We don't want to continually re-apply changes that don't need to be updated
		getDirtyClasses().clear();
		Log.info("Successfully redefined {} classes", definitions.length);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		Map<String, byte[]> classes = new HashMap<>();
		return classes;
	}

	@Override
	protected Map<String, byte[]> loadFiles() {
		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return "Instrumentation";
	}

	private void loadRuntimeClasses(Map<String, byte[]> map) throws IOException {
		// iterate over loaded classes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for(Class<?> c : instrumentation.getAllLoadedClasses()) {
			String name = Type.getInternalName(c);
			// skip specified prefixes
			if(shouldSkip(name))
				continue;
			// Skip array types
			if (name.contains("["))
				continue;
			String path = name.concat(".class");
			ClassLoader loader = c.getClassLoader();
			try(InputStream in = (loader != null) ?
					loader.getResourceAsStream(path) :
					ClassLoader.getSystemResourceAsStream(path)) {
				if(in != null) {
					out.reset();
					getClasses().put(name, IOUtil.toByteArray(in, out, buffer));
					getDirtyClasses().remove(name);
				}
			}
		}
	}
  
	/**
	 *
	 * @return Instrumentation resource instance.
	 * @throws IOException If the resource cannot be instantiated.
	 */
	public static InstrumentationResource getInstance() throws IOException {
		if (instance == null)
			instance = new InstrumentationResource();
		return instance;
	}

	/**
	 * @return {@code true} if Recaf is running as a Java agent.
	 */
	public static boolean isActive() {
		return instrumentation != null;
	}

	/**
	 * Transformer to load classes from instrumentation.
	 */
	private static class InstrumentationResourceTransformer implements ClassFileTransformer {
		private static boolean firstTransformerLoad;

		public byte[] transform(Module module, ClassLoader loader, String className,
								Class<?> cls, ProtectionDomain domain, byte[] buffer) {
			return transform(loader, className, cls, domain, buffer);
		}

		@Override
		public byte[] transform(ClassLoader loader, String className,
								Class<?> cls, ProtectionDomain domain, byte[] buffer) {
			// This super odd way of getting the resource IS INTENTIONAL.
			// If you choose to optimize this in the future verify it behaves the same.
			InstrumentationResource res = null;
			try {
				res = getInstance();
				if(firstTransformerLoad) {
					firstTransformerLoad = false;
					// There is a time gap between when we first called 'loadClasses' and this gets called.
					// We need to fetch those classes here so we have everything available.
					res.loadRuntimeClasses(getInstance().getClasses());
				}
			} catch(IOException ex) { return buffer; }
			// Check to skip class
			String internal = className.replace('.', '/');
			if(res.shouldSkip(internal))
				return buffer;
			// Add to classes map
			res.getClasses().put(internal, buffer);
			// Make sure the class is NOT marked as dirty after initially registering it
			res.getDirtyClasses().remove(internal);
			return buffer;
		}
	}
}
