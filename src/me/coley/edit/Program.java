package me.coley.edit;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import me.coley.edit.asm.JarData;
import me.coley.edit.ui.Gui;

public class Program {
	public Gui window;
	public File currentJar;
	public JarData jarData;
	public FileChoosers fileChoosers = new FileChoosers();

	public void openFile(File file) throws IOException {
		this.currentJar = file;
		this.jarData = new JarData(file);
		this.window.updateTree();
	}

	public void saveFile(File file) throws IOException {
		this.jarData.save(file);
	}

	public void selectClass(ClassNode node) {
		// TODO Auto-generated method stub

	}

	public void showGui() {
		Program instance = this;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new Gui(instance);
					window.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
