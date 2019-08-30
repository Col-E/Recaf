package me.coley.recaf.command;

import me.coley.recaf.Recaf;
import me.coley.recaf.command.impl.*;
import me.coley.recaf.workspace.Workspace;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Base controller to work off of/invoke commands on.
 *
 * @author Matt
 */
public abstract class Controller implements Runnable {
	private final Map<Class<?>, Supplier<Callable<?>>> actions = new HashMap<>();
	protected File initialWorkspace;

	/**
	 * @param workspace
	 * 		Initial workspace file. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 */
	public Controller(File workspace) {
		this.initialWorkspace = workspace;
	}

	/**
	 * @param workspace Workspace to set.
	 */
	protected void setWorkspace(Workspace workspace) {
		Recaf.setCurrentWorkspace(workspace);
	}

	/**
	 * @return Current workspace.
	 */
	protected Workspace getWorkspace() {
		return Recaf.getCurrentWorkspace();
	}

	/**
	 * Try to load the passed initial workspace
	 *
	 * @throws Exception
	 * 		When the load action could not be completed.
	 */
	protected void loadInitialWorkspace() throws Exception {
		// Check if a workspace to load even exists
		if (initialWorkspace == null)
			return;
		// Attempt to load it
		LoadWorkspace load = get(LoadWorkspace.class);
		load.input = initialWorkspace;
		setWorkspace(load.call());
	}

	/**
	 * @param key
	 * 		The class of the callable to load.
	 * @param <R>
	 * 		Return type of callable.
	 * @param <T>
	 * 		Callable implementation.
	 *
	 * @return New callable instance.
	 */
	@SuppressWarnings("unchecked")
	public <R, T extends Callable<R>> T get(Class<?> key) {
		return (T) actions.get(key).get();
	}

	/**
	 * Setup commands.
	 */
	protected void setup() {
		register(LoadWorkspace.class);
		register(WorkspaceInfo.class);
		register(Help.class);
		register(Quit.class);
	}

	/**
	 * Register a command class so it can be invoked by the controller.
	 *
	 * @param clazz
	 * 		Command class.
	 * @param <R>
	 * 		Return type of callable.
	 * @param <T>
	 * 		Callable implementation.
	 */
	protected <R, T extends Callable<R>> void register(Class<T> clazz) {
		actions.put(clazz, () -> {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Failed to generate callable instance of: " +
						clazz.getName());
			}
		});
	}
}
