package me.coley.recaf;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.asm.AsmUtil;
import me.coley.recaf.asm.JarData;
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
	public final Options options;
	/**
	 * The utility instance handling a variety of ASM duties <i>(Bytecode loading, parsing, exporting)</i>.
	 */
	public final AsmUtil asm;

	private Recaf() {
		filePrompts = new FilePrompt();
		options = new Options();
		options.load();
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
					gui = new Gui();
					gui.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		INSTANCE.showGui();
	}
}
