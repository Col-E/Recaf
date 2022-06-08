package me.coley.recaf.config;

import me.coley.recaf.config.container.*;

import java.util.Arrays;
import java.util.Collection;

/**
 * Instance manager of {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
public class Configs {
	private static final DisplayConfig display = new DisplayConfig();
	private static final EditorConfig editor = new EditorConfig();
	private static final KeybindConfig keybinds = new KeybindConfig();
	private static final DialogConfig dialogs = new DialogConfig();
	private static final AssemblerConfig assembler = new AssemblerConfig();
	private static final CompilerConfig compiler = new CompilerConfig();
	private static final DecompilerConfig decompiler = new DecompilerConfig();
	private static final ExportConfig export = new ExportConfig();
	private static final RecentWorkspacesConfig recentWorkspaces = new RecentWorkspacesConfig();
	private static final SsvmConfig ssvm = new SsvmConfig();
	private static final PluginConfig plugin = new PluginConfig();

	/**
	 * @return Collection of all config container instances.
	 */
	public static Collection<ConfigContainer> containers() {
		return Arrays.asList(
				display,
				editor,
				keybinds,
				dialogs,
				assembler,
				compiler,
				decompiler,
				export,
				recentWorkspaces,
				ssvm,
				plugin
		);
	}

	/**
	 * @return Assembler config instance.
	 */
	public static AssemblerConfig assembler() {
		return assembler;
	}

	/**
	 * @return Display config instance.
	 */
	public static DisplayConfig display() {
		return display;
	}

	/**
	 * @return Editor config instance.
	 */
	public static EditorConfig editor() {
		return editor;
	}

	/**
	 * @return Keybind config instance.
	 */
	public static KeybindConfig keybinds() {
		return keybinds;
	}

	/**
	 * @return Dialog config instance.
	 */
	public static DialogConfig dialogs() {
		return dialogs;
	}

	/**
	 * @return Compiler config instance.
	 */
	public static CompilerConfig compiler() {
		return compiler;
	}

	/**
	 * @return Decompiler config instance.
	 */
	public static DecompilerConfig decompiler() {
		return decompiler;
	}

	/**
	 * @return Export config instance.
	 */
	public static ExportConfig export() {
		return export;
	}

	/**
	 * @return Recent workspaces config instance.
	 */
	public static RecentWorkspacesConfig recentWorkspaces() {
		return recentWorkspaces;
	}

	/**
	 * @return SSVM config instance.
	 */
	public static SsvmConfig ssvm() {
		return ssvm;
	}

	/**
	 * @return Plugin config instance.
	 */
	public static PluginConfig plugin() {
		return plugin;
	}
}
