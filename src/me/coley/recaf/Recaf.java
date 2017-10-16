package me.coley.recaf;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
		String laf = System.getenv("COL_RECAF_LAF");
		try {
			setLookAndFeel(laf);
		} catch (ClassNotFoundException|ClassCastException|UnsupportedLookAndFeelException ex) {
			System.out.println("The specified Look and Feel '" + laf + "' could not be loaded.");
			try {
				setLookAndFeel(null);
			} catch(Exception ex2) {
				//If this exception is thrown we are truly insane…
				throw new RuntimeException(ex2);
			}
		}
		INSTANCE.showGui();
	}

	public static void setLookAndFeel(String laf) throws ClassNotFoundException, ClassCastException, UnsupportedLookAndFeelException {
		if (laf == null) {
			//Default Look & Feel
			laf = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
		}
		try {
			UIManager.setLookAndFeel(laf);
		} catch (InstantiationException|IllegalAccessException ex) {
			//As far as I know, these errors are not recoverable
			throw new RuntimeException(ex);
		}
	}
}
