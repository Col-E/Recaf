package me.coley.edit.ui;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.BorderLayout;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JMenuItem;

public class Gui {
	private JFrame frame;

	/**
	 * Create the application.
	 */
	public Gui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setFrame(new JFrame());
		getFrame().setBounds(100, 100, 914, 512);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		getFrame().setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmOpenJar = new JMenuItem("Open Jar");
		mnFile.add(mntmOpenJar);
		
		JMenuItem mntmSaveJar = new JMenuItem("Save Jar");
		mnFile.add(mntmSaveJar);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		getFrame().getContentPane().setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(true);
		getFrame().getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JTree treeFiles = new JTree();
		splitPane.setLeftComponent(treeFiles);
		
		JPanel pnlContent = new JPanel();
		splitPane.setRightComponent(pnlContent);
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setFrame(JFrame frame) {
		this.frame = frame;
	}

}
