package me.coley.edit.ui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JSplitPane;
import me.coley.edit.Program;
import me.coley.edit.ui.component.FileTree;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

public class Gui {
	private final Program callback;
	private JFrame frame;
	private FileTree treeFiles;
	private JTabbedPane tabbedContent;

	/**
	 * Create the application.
	 * 
	 * @param instance
	 */
	public Gui(Program instance) {
		this.callback = instance;
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

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		getFrame().getContentPane().setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.15);
		splitPane.setOneTouchExpandable(true);
		getFrame().getContentPane().add(splitPane, BorderLayout.CENTER);

		treeFiles = new FileTree(callback);
		splitPane.setLeftComponent(treeFiles);

		tabbedContent = new JTabbedPane(JTabbedPane.TOP);
		tabbedContent.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		splitPane.setRightComponent(tabbedContent);

	}

	private void displayError(Exception e) {
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

		tabbedContent.add("Error: " + e.getClass().getSimpleName(), new JScrollPane(text));
		tabbedContent.setSelectedIndex(tabbedContent.getTabCount() - 1);
	}

	public void updateTree() {
		treeFiles.refresh();
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setFrame(JFrame frame) {
		this.frame = frame;
	}

}
