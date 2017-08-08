package me.coley.recaf;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.asm.AsmUtil;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.ui.FileChoosers;
import me.coley.recaf.ui.Gui;

public class Program {
	private static Program instance;
	public Gui window;
	public File currentJar;
	public JarData jarData;
	public FileChoosers fileChoosers;
	public Options options;
	public AsmUtil asm;

	public Program() {
		instance = this;
		fileChoosers = new FileChoosers();
		options = new Options();
		asm = new AsmUtil();
	}

	public void openFile(File file) throws IOException {
		this.currentJar = file;
		this.jarData = new JarData(file);
		this.window.updateTree();
		this.window.getFrame().setTitle("Recaf: " + file.getName());
	}

	public void saveFile(File file) throws IOException {
		this.jarData.save(file);
	}

	public void selectClass(ClassNode node) {
		this.window.addClassView(node);
	}

	public void showGui() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new Gui();
					window.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static Program getInstance() {
		return instance;
	}
}
