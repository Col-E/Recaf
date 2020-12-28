package me.coley.recaf.control;

import me.coley.recaf.Recaf;
import me.coley.recaf.command.impl.*;
import me.coley.recaf.config.ConfigManager;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.CommandPlugin;
import me.coley.recaf.plugin.api.ExitPlugin;
import me.coley.recaf.plugin.api.StartupPlugin;
import me.coley.recaf.plugin.api.WorkspacePlugin;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.workspace.InstrumentationResource;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static me.coley.recaf.util.Log.*;

/**
 * Base controller to work off of/invoke commands on.
 *
 * @author Matt
 */
public abstract class Controller implements Runnable {
	private final Map<Class<?>, Supplier<Callable<?>>> actions = new HashMap<>();
	private final ConfigManager configs = new ConfigManager(Recaf.getDirectory("config"));
	private Workspace workspace;
	protected Path initialWorkspace;


	/**
	 * @param workspace
	 * 		Initial workspace path. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 */
	public Controller(Path workspace) {
		this.initialWorkspace = workspace;
	}

	/**
	 * @param workspace Workspace to set.
	 */
	public void setWorkspace(Workspace workspace) {
		Collection<WorkspacePlugin> plugins = PluginsManager.getInstance().ofType(WorkspacePlugin.class);
		Workspace old = this.workspace;
		if (old != null) {
			plugins.forEach(plugin -> plugin.onClosed(old));
		}
		this.workspace = workspace;
		Recaf.setCurrentWorkspace(workspace);
		plugins.forEach(plugin -> plugin.onOpened(workspace));
	}

	/**
	 * @return Current workspace.
	 */
	public final Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * @return Config manager.
	 */
	public ConfigManager config() {
		return configs;
	}

	@Override
	public void run() {
		// Load workspace
		try {
			loadInitialWorkspace();
		} catch(Exception ex) {
			error(ex, "Error loading workspace from file: " + initialWorkspace);
		}
		// Call startup plugins
		PluginsManager.getInstance().ofType(StartupPlugin.class).forEach(plugin -> plugin.onStart(this));
	}

	/**
	 * Try to load the passed initial workspace
	 *
	 * @throws Exception
	 * 		When the load action could not be completed.
	 */
	protected final void loadInitialWorkspace() throws Exception {
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
	 * Setup config and commands.
	 *
	 * @return {@code true} if successful.
	 */
	public boolean setup() {
		// Load config
		boolean success = true;
		try {
			config().initialize();
		} catch (IOException ex) {
			error(ex, "Error initializing ConfigManager");
			success = false;
		}
		// Register commands
		register(LoadWorkspace.class);
		register(WorkspaceInfo.class);
		register(Disassemble.class);
		register(Decompile.class);
		register(Assemble.class);
		register(Export.class);
		register(Search.class);
		register(Remap.class);
		register(Help.class);
		register(Quit.class);
		register(Wait.class);
		register(Run.class);
		// Load command plugins
		PluginsManager.getInstance().ofType(CommandPlugin.class)
				.forEach(commandPlugin -> register(commandPlugin.getClass()));
		return success;
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
	@SuppressWarnings("unchecked")
	protected <R, T extends Callable<R>> void register(Class<T> clazz) {
		actions.put(clazz, () -> {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Failed to generate callable instance of: " +
						clazz.getName());
			}
		});
		for (Class<?> subclass : clazz.getDeclaredClasses()) {
			try {
				Class<T> cubcast = (Class<T>) subclass;
				actions.put(cubcast, () -> {
					try {
						return cubcast.newInstance();
					} catch(Exception e) {
						throw new IllegalStateException("Failed to generate callable instance of: " +
								cubcast.getName());
					}
				});
			} catch(ClassCastException ex) {
				error("Failed to setup subcommand: " + subclass.getName());
				throw new IllegalStateException("Failed to setup subcommand: " + subclass.getName(), ex);
			}
		}
	}

	/**
	 * Close Recaf.
	 */
	public void exit() {
		info("Shutting down");
		PluginsManager.getInstance()
				.ofType(ExitPlugin.class)
				.forEach(plugin -> plugin.onExit(this));
		config().save();
		ThreadUtil.shutdown();
		if (!InstrumentationResource.isActive()) {
			System.exit(0);
		}
	}
}
