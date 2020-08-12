package me.coley.recaf;

import io.github.soc.directories.BaseDirectories;
import me.coley.recaf.command.impl.Initializer;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.EntryLoaderProviderPlugin;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.Natives;
import me.coley.recaf.util.self.SelfDependencyPatcher;
import me.coley.recaf.util.self.SelfReferenceUtil;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.InstrumentationResource;
import me.coley.recaf.workspace.Workspace;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import static me.coley.recaf.util.Log.*;

/**
 * Entry point &amp; version constant.
 *
 * @author Matt
 */
public class Recaf {
	public static final String VERSION = "2.4.1";
	public static final String DOC_URL = "https://col-e.github.io/Recaf/documentation.html";
	private static Controller currentController;
	private static Workspace currentWorkspace;
	private static boolean initialized;
	private static boolean headless;
	/**
	 * Listeners to call when the {@link #currentWorkspace current workspace} is changed.
	 */
	private static final Set<Consumer<Workspace>> workspaceSetListeners = new HashSet<>();

	/**
	 * Start Recaf.
	 *
	 * @param args
	 * 		Optional args.
	 */
	public static void main(String[] args) {
		Natives.loadAttach().ifPresent(t -> error(t, "Failed to load attach library."));
		init();
		launch(args);
	}

	/**
	 * Start Recaf as a launch-argument Java agent.
	 *
	 * @param agentArgs
	 * 		Agent arguments to pass to Recaf.
	 * @param inst
	 * 		Instrumentation instance.
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		agent(agentArgs, inst);
	}

	/**
	 * Start Recaf as a dynamically attached Java agent.
	 *
	 * @param agentArgs
	 * 		Agent arguments to pass to Recaf.
	 * @param inst
	 * 		Instrumentation instance.
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		agent(agentArgs, inst);
	}

	private static void agent(String args, Instrumentation inst) {
		if (InstrumentationResource.isActive()) {
			String message = "Recaf was previously attached to current VM.\n" +
					"Reattaching currently not supported.\n" +
					"Watch GitHub for further releases that might solve this issue.";
			throw new UnsupportedOperationException(message);
		}
		try {
			inst.appendToSystemClassLoaderSearch(new JarFile(SelfReferenceUtil.get().getFile()));
		} catch (IOException ex) {
			Log.error(ex, "Failed to self-append to system classloader search.");
		}
		Thread t = Thread.currentThread();
		if (t.getContextClassLoader() == null) {
			ClassLoader cl = Recaf.class.getClassLoader();
			if (cl == null) cl = ClassLoader.getSystemClassLoader();
			t.setContextClassLoader(cl);
		}
		init();
		// Log that we are an agent
		info("Starting as agent...");
		// Add instrument launch arg
		if(args == null || args.trim().isEmpty())
			args = "--instrument";
		else if(args.contains("--instrument"))
			args = args + ",--instrument";
		// Set instance
		InstrumentationResource.instrumentation = inst;
		// Start Recaf
		launch(args.split("[=,]"));
	}

	/**
	 * Run pre-launch initialization tasks.
	 */
	private static void init() {
		if (!initialized) {
			// Patch in dependencies
			SelfDependencyPatcher.patch();
			// Fix title bar not displaying in GTK systems
			System.setProperty("jdk.gtk.version", "2");
			// Show version & start
			info("Recaf-{}", VERSION);
			initialized = true;
		}
	}

	/**
	 * Launch Recaf
	 */
	private static void launch(String[] args) {
		// Setup initializer, this loads command line arguments
		Initializer initializer = new Initializer();
		new CommandLine(initializer).execute(args);
		headless = initializer.cli;
		loadPlugins();
		// Do version check
		SelfUpdater.setController(initializer.getController());
		SelfUpdater.setArgs(args);
		SelfUpdater.checkForUpdates();
		// Start the initializer's controller, starting Recaf
		initializer.startController();
	}

	/**
	 * Load plugins.
	 */
	private static void loadPlugins() {
		try {
			PluginsManager manager = PluginsManager.getInstance();
			manager.load();
			// Check for loaders, set the current loader to the first one found
			Collection<EntryLoaderProviderPlugin> loaders = manager.ofType(EntryLoaderProviderPlugin.class);
			if (!loaders.isEmpty())
				manager.setEntryLoader(loaders.iterator().next().create());
		} catch(NoClassDefFoundError noDef) {
			Log.error("An error occurred loading the plugins, failed class lookup: " +
					noDef.getMessage() + "\n - Is the plugin outdated?");
		} catch(Throwable t) {
			Log.error(t, "An error occurred loading the plugins");
		}
	}

	/**
	 * @param currentWorkspace
	 * 		New workspace.
	 */
	public static void setCurrentWorkspace(Workspace currentWorkspace) {
		try {
			workspaceSetListeners.forEach(listener -> listener.accept(currentWorkspace));
		} catch(Throwable t) {
			Log.error(t, "Workspace listener threw an error: {}", t.getMessage());
		}
		Recaf.currentWorkspace = currentWorkspace;
	}

	/**
	 * Try not to use this too often. It would be best to be passed an instance of the workspace
	 * so things do not become statically dependent.
	 *
	 * @return Current workspace.
	 */
	public static Workspace getCurrentWorkspace() {
		return currentWorkspace;
	}

	/**
	 * @param controller
	 * 		New controller.
	 */
	public static void setController(Controller controller) {
		if (currentController != null)
			throw new IllegalStateException("Controller already set!");
		headless = controller instanceof HeadlessController;
		currentController = controller;
	}

	/**
	 * @return Recaf controller.
	 */
	public static Controller getController() {
		return currentController;
	}

	/**
	 * @return Set of listeners to call when the {@link #currentWorkspace current workspace} is changed.
	 */
	public static Set<Consumer<Workspace>> getWorkspaceSetListeners() {
		return workspaceSetListeners;
	}

	/**
	 * @return {@code true} when Recaf is running in headless mode.
	 */
	public static boolean isHeadless() {
		return headless;
	}

	/**
	 * @return Recaf's storage directory.
	 */
	public static Path getDirectory() {
		return Paths.get(BaseDirectories.get().configDir).resolve("Recaf");
	}

	/**
	 * @param subfolder
	 * 		Subfolder name.
	 *
	 * @return Subfolder in Recaf's storage directory.
	 */
	public static Path getDirectory(String subfolder) {
		return getDirectory().resolve(subfolder);
	}
}
