package me.coley.recaf;

import me.coley.recaf.workspace.Workspace;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Entry point &amp; version constant.
 *
 * @author Matt
 */
public class Recaf {
	/**
	 * Recaf version.
	 */
	public static final String VERSION = "2.0.0";
	/**
	 * Current workspace.
	 */
	private static Workspace currentWorkspace;
	/**
	 * Listeners to call when the {@link #currentWorkspace current workspace} is changed.
	 */
	private static final Set<Consumer<Workspace>> workspaceSetListeners = new HashSet<>();

	/**
	 * Start recaf.
	 *
	 * @param args
	 * 		Optional args.
	 */
	public static void main(String[] args) {
		setupLogging();
		Logger.info("Starting Recaf-{}...", VERSION);
		// Invoke
		new CommandLine(new Initializer()).execute(args);
	}

	/**
	 * @param currentWorkspace
	 * 		Sets the current workspace.
	 */
	public static void setCurrentWorkspace(Workspace currentWorkspace) {
		workspaceSetListeners.forEach(listener -> listener.accept(currentWorkspace));
		Recaf.currentWorkspace = currentWorkspace;
	}

	/**
	 * @return Current workspace.
	 */
	public static Workspace getCurrentWorkspace() {
		return currentWorkspace;
	}

	/**
	 * @return Set of listeners to call when the {@link #currentWorkspace current workspace} is changed.
	 */
	public static Set<Consumer<Workspace>> getWorkspaceSetListeners() {
		return workspaceSetListeners;
	}

	/**
	 * Setup tinylog logging.
	 */
	public static void setupLogging() {
		// Setup tinylog instance
		Configurator.defaultConfig()
				.formatPattern("{level}-{date}: {message|indent=4}")
				.writingThread(true)
				.addWriter(new FileWriter("rclog.txt"))
				.activate();
	}
}
