package me.coley.recaf;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.asm.AsmUtil;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.config.AsmConfig;
import me.coley.recaf.config.ThemeConfig;
import me.coley.recaf.config.UiConfig;
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
	 * The options.
	 */
	public final UiConfig confUI;
	/**
	 * The color configuration.
	 */
	public final ThemeConfig confTheme;
	/**
	 * ASM configuration.
	 */
	public final AsmConfig confASM;
	/**
	 * The utility instance handling a variety of ASM duties <i>(Bytecode
	 * loading, parsing, exporting)</i>.
	 */
	public final AsmUtil asm;

	private Recaf() {
		filePrompts = new FilePrompt();
		gui = new Gui(this);
		confTheme = new ThemeConfig();
		confTheme.load();
		confUI = new UiConfig();
		confUI.load();
		confASM = new AsmConfig();
		confASM.load();
		asm = new AsmUtil(this);
	}

	public void openFile(File file) throws IOException {
		this.currentJar = file;
		this.jarData = new JarData(file);
		this.gui.updateTree();
		this.gui.getFrame().setTitle("Recaf: " + file.getName());
	}

	public void saveFile(File file) throws IOException {
		this.jarData.save(file);
	}

	public void selectClass(ClassNode node) {
		this.gui.addClassView(node);
	}

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
				} catch (Exception e) {
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
		if (args.length > 0) {
			File f = new File(args[0]);
			if (f.exists()) {
				INSTANCE.currentJar = f;
			}
		}
	}
}
