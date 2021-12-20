package me.coley.recaf.workspace;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ExitPlugin;
import me.coley.recaf.plugin.api.InternalPlugin;
import me.coley.recaf.plugin.api.StartupPlugin;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.Type;
import org.plugface.core.annotations.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Importable instrumentation resource.
 *
 * @author Matt
 */
public class InstrumentationResource extends JavaResource {
	private static final ResourceLocation LOCATION = LiteralResourceLocation.ofKind(
			ResourceKind.INSTRUMENTATION,
			"Instrumentation");
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
		// Instrumentation is ALWAYS primary
		setPrimary(true);
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
			ClassFileTransformer transformer = new InstrumentationResourceTransformer();
			instrumentation.addTransformer(transformer);
			// Setup hook for workspace.
			PluginsManager.getInstance()
					.addPlugin(new InstrumentationPlugin(instance, transformer));
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
		if(dirty.isEmpty()) {
			Log.info("There are no classes to redefine.", dirty.size());
			return;
		}
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
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> loadFiles() {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> copyMap(Map<String, byte[]> map) {
		return new ConcurrentHashMap<>(map);
	}

	@Override
	public String toString() {
		return "Instrumentation";
	}

	private void loadRuntimeClasses(Map<String, byte[]> map) throws IOException {
		// iterate over loaded classes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		Class<?>[] klass = {null};
		int failedTransformations = 0;
		// Let's skipp all Recaf's classes.
		for(Class<?> c : instrumentation.getAllLoadedClasses()) {
			if (ClasspathUtil.isRecafClass(c)) {
				continue;
			}
			String name = Type.getInternalName(c);
			// skip specified prefixes
			if(shouldSkip(name))
				continue;
			// Skip array types
			if (name.contains("["))
				continue;
			try {
				klass[0] = c;
				instrumentation.retransformClasses(klass);
			} catch (UnmodifiableClassException ex) {
				if (++failedTransformations < 5) {
					Log.error("Could not get live version of a class {}:", name, ex);
				}
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
		if (failedTransformations != 0) {
			Log.error("Could not get live version for {} classes", failedTransformations);
		}
	}

	/**
	 * @return Instrumentation resource instance.
	 *
	 * @throws IOException
	 * 		When the resource cannot be instantiated.
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
		private boolean firstTransformerLoad = true;

		public byte[] transform(Module module, ClassLoader loader, String className,
								Class<?> cls, ProtectionDomain domain, byte[] buffer) {
			return transform(loader, className, cls, domain, buffer);
		}

		@Override
		public byte[] transform(ClassLoader loader, String className,
								Class<?> cls, ProtectionDomain domain, byte[] buffer) {
			// This super odd way of getting the resource IS INTENTIONAL.
			// If you choose to optimize this in the future verify it behaves the same.
			InstrumentationResource res;
			try {
				res = getInstance();
				synchronized (this) {
					if (firstTransformerLoad) {
						firstTransformerLoad = false;
						// There is a time gap between when we first called 'loadClasses' and this gets called.
						// We need to fetch those classes here so we have everything available.
						res.loadRuntimeClasses(getInstance().getClasses());
					}
				}
			} catch(IOException ex) { return buffer; }
			// Checks to skip class
			if (ClasspathUtil.isRecafLoader(loader)) {
				return buffer;
			}
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

	@Override
	public ResourceLocation getShortName() {
		return LOCATION;
	}

	@Override
	public ResourceLocation getName() {
		return LOCATION;
	}

	@Plugin(name = "Instrumentation")
	private static final class InstrumentationPlugin implements InternalPlugin,
			StartupPlugin, ExitPlugin {
		private final InstrumentationResource resource;
		private final ClassFileTransformer transformer;

		/**
		 * @param resource
		 * 		Instrumentation resource.
		 * @param transformer
		 * 		Instrumentation transformer.
		 */
		InstrumentationPlugin(InstrumentationResource resource,
							  ClassFileTransformer transformer) {
			this.resource = resource;
			this.transformer = transformer;
		}

		@Override
		public void onStart(Controller controller) {
			controller.setWorkspace(new Workspace(resource));
		}

		@Override
		public void onExit(Controller controller) {
			instrumentation.removeTransformer(transformer);
		}

		@Override
		public String getVersion() {
			return Recaf.VERSION;
		}

		@Override
		public String getDescription() {
			return "Instrumentation hook";
		}
	}
}
