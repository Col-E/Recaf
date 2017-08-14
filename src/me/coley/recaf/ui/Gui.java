package me.coley.recaf.ui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Program;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.panel.AsmFlagsPanel;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.TabbedPanel;
import me.coley.recaf.ui.component.tree.JarFileTree;

import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Gui {
	private final Program callback = Program.getInstance();
	private JFrame frame;
	private JarFileTree treeFiles;
	private TabbedPanel tabbedContent;

	public Gui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame("Recaf: Java Bytecode Editor");
		frame.setBounds(100, 100, 1200, 730);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpenJar = new JMenuItem("Open Jar");
		mntmOpenJar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = callback.fileChoosers.getFileChooser();
				int val = chooser.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					try {
						callback.openFile(chooser.getSelectedFile());
					} catch (IOException e1) {
						displayError(e1);
					}
				}
			}

		});
		mnFile.add(mntmOpenJar);

		JMenuItem mntmSaveJar = new JMenuItem("Save Jar");
		mntmSaveJar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = callback.fileChoosers.createFileSaver();
				int val = chooser.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					try {
						callback.saveFile(chooser.getSelectedFile());
					} catch (IOException e1) {
						displayError(e1);
					}
				}
			}

		});
		mnFile.add(mntmSaveJar);

		/*
		 * JMenu mnEdit = new JMenu("Edit"); JMenuItem mntmUndo = new
		 * JMenuItem("Undo"); mntmUndo.addActionListener(new ActionListener() {
		 * 
		 * @Override public void actionPerformed(ActionEvent e) {
		 * callback.history.undoLast(); } }); mnEdit.add(mntmUndo);
		 * menuBar.add(mnEdit);
		 */

		JMenu mnOptions = new JMenu("Options");
		mnOptions.add(new ActionCheckBox("Show jump hints", callback.options.opcodeShowJumpHelp,
				b -> callback.options.opcodeShowJumpHelp = b));
		mnOptions.add(new ActionCheckBox("Simplify type descriptors", callback.options.opcodeSimplifyDescriptors,
				b -> callback.options.opcodeSimplifyDescriptors = b));
		mnOptions.add(new ActionMenuItem("ASM Flags", () -> {
			openTab("ASM Flags", new AsmFlagsPanel());
		}));
		menuBar.add(mnOptions);

		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(true);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		treeFiles = new JarFileTree();
		splitPane.setLeftComponent(treeFiles);

		tabbedContent = new TabbedPanel();
		splitPane.setRightComponent(tabbedContent);

	}

	/**
	 * Creates a new tab with the text of the exception.
	 * 
	 * @param e
	 */
	public void displayError(Exception e) {
		JTextArea text = new JTextArea();
		text.setEditable(false);
		text.append(e.getClass().getSimpleName() + ":\n");
		text.append("Message: " + e.getMessage() + "\n");
		text.append("Trace: \n");
		for (StackTraceElement element : e.getStackTrace()) {
			text.append(element.toString() + "\n");
		}

		// TODO: Logging of cause
		// text.append("Cause: " + e.getCause() + "\n");

		tabbedContent.addTab("Error: " + e.getClass().getSimpleName(), new JScrollPane(text));
		tabbedContent.setSelectedTab(tabbedContent.getTabCount() - 1);
	}

	/**
	 * Opens up a class tab for the given class-node, or opens an existing page
	 * if one is found.
	 * 
	 * @param node
	 */
	public void addClassView(ClassNode node) {
		if (tabbedContent.hasCached(node.name)) {
			tabbedContent.setSelectedTab(tabbedContent.getCachedIndex(node.name));
		} else {
			tabbedContent.addTab(node.name, new JScrollPane(new ClassDisplayPanel(node)));
			tabbedContent.setSelectedTab(tabbedContent.getTabCount() - 1);
			int i = tabbedContent.getCachedIndex(node.name);
			if (i == -1) {
				i = tabbedContent.getTabCount() - 1;
			}
			tabbedContent.setSelectedTab(i);
		}
	}

	/**
	 * Opens up a tab for the given component, or opens an existing page if one
	 * is found.
	 * 
	 * @param title
	 *            Title of tab.
	 * @param component
	 *            Content of tab.
	 */
	public void openTab(String title, Component component) {
		if (tabbedContent.hasCached(title)) {
			tabbedContent.setSelectedTab(tabbedContent.getCachedIndex(title));
		} else {
			tabbedContent.addTab(title, component);
			int i = tabbedContent.getCachedIndex(title);
			if (i == -1) {
				i = tabbedContent.getTabCount() - 1;
			}
			tabbedContent.setSelectedTab(i);
		}
	}

	/**
	 * Refreshes the tree to display the current jar file.
	 */
	public void updateTree() {
		treeFiles.refresh();
	}

	public JFrame getFrame() {
		return frame;
	}

}
