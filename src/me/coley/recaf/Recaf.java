package me.coley.recaf;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.asm.AsmUtil;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.config.AsmConfig;
import me.coley.recaf.config.BlocksConfig;
import me.coley.recaf.config.ThemeConfig;
import me.coley.recaf.config.UiConfig;
import me.coley.recaf.event.Bus;
import me.coley.recaf.event.impl.EInit;
import me.coley.recaf.plugin.Plugins;
import me.coley.recaf.ui.FilePrompt;
import me.coley.recaf.ui.Gui;

public enum Recaf {
	/**
	 * Singleton instance.
	 */
	INSTANCE;
	/**
	 * The gui.
	 */
	public Gui gui;
	/**
	 * The current jar file being modified.
	 */
	public File currentJar;
	/**
	 * Data about the {@link #currentJar current jar file}.
	 */
	public JarData jarData;
	/**
	 * The file selection manager.
	 */
	public final FilePrompt filePrompts;
	/**
	 * The ui configuration.
	 */
	public final UiConfig confUI;
	/**
	 * The color configuration.
	 */
	public final ThemeConfig confTheme;
	/**
	 * The ASM configuration.
	 */
	public final AsmConfig confASM;
	/**
	 * The opcode blocks configuration.
	 */
	public final BlocksConfig confblocks;
	/**
	 * The utility instance handling a variety of ASM duties <i>(Bytecode
	 * loading, parsing, exporting)</i>.
	 */
	public final AsmUtil asm;
	/**
	 * Event bus.
	 */
	public final Bus bus = new Bus();
	/**
	 * Plugin system.
	 */
	public final Plugins plugins;
	
	private Recaf() {
		filePrompts = new FilePrompt();
		gui = new Gui(this);
		confTheme = new ThemeConfig();
		confTheme.load();
		confUI = new UiConfig();
		confUI.load();
		confASM = new AsmConfig();
		confASM.load();
		confblocks = new BlocksConfig();
		confblocks.load();
		asm = new AsmUtil(this);
		plugins = new Plugins(this);
	}

	/**
	 * Sets the {@link #currentJar current jar}, loads the {@link #jarData data
	 * within}, and refreshes the UI.
	 * 
	 * @param file
	 *            File to read classes from.
	 * @throws IOException
	 *             Thrown if file could not be read.
	 */
	public void openFile(File file) throws IOException {
		this.currentJar = file;
		this.jarData = new JarData(file);
		this.gui.updateTree();
		this.gui.getFrame().setTitle("Recaf: " + file.getName());
	}

	/**
	 * Saves the current edits to the given file.
	 * 
	 * @param file
	 *            File to save modified jar to.
	 * @throws IOException
	 *             Thrown if file could not be saved.
	 */
	public void saveFile(File file) throws IOException {
		this.jarData.save(file);
	}

	/**
	 * Opens a class in the gui.
	 * 
	 * @param node
	 *            The class to open.
	 */
	public void selectClass(ClassNode node) {
		this.gui.addClassView(node);
	}

	/**
	 * Displays the GUI.
	 */
	public void showGui() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					gui.initialize();
					gui.getFrame().setVisible(true);
					// If a file has been set, open it.
					if (currentJar != null) {
						openFile(currentJar);
					}
					// post init event
					bus.post(new EInit());
				} catch (Exception e) {
					// TODO: Propper logging
					e.printStackTrace();
				}
			}
		});
	}

	public static void main(String[] args) {
		INSTANCE.confUI.setLookAndFeel(INSTANCE.confUI.getLookAndFeel());
		INSTANCE.showGui();
		// TODO: Proper command line system
		//
		// Read args, check if input file given.
		if (args.length >= 1) {
			File f = new File(args[0]);
			if (f.exists()) {
				INSTANCE.currentJar = f;
			}
		}
	}
}
