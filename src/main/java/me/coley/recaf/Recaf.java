package me.coley.recaf;

import me.coley.recaf.command.impl.Initializer;
import me.coley.recaf.workspace.Workspace;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static me.coley.recaf.util.Log.*;

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
		// Fix title bar not displaying in GTK systems
		System.setProperty("jdk.gtk.version", "2");
		System.setProperty("recaf.home", getStorageDirectory().normalize().toString());
		// Show version & start
		info("Recaf-{}", VERSION);
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
	 * Try not to use this too often. It would be best to be passed an instance of the workspace
	 * so things do not become statically dependent.
	 *
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
	 * @param path
	 * 		Subfolder name.
	 *
	 * @return Subfolder in Recaf's storage directory.
	 */
	public static Path getDirectory(String path) {
		return Paths.get(System.getProperty("user.home")).resolve("Recaf").resolve(path);
	}
}
