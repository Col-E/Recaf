package me.coley.recaf.ui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.panel.AsmFlagsPanel;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.SearchPanel;
import me.coley.recaf.ui.component.panel.TabbedPanel;
import me.coley.recaf.ui.component.tree.JarFileTree;

import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class Gui {
	private final Recaf recaf = Recaf.getInstance();
	private JFrame frame;
	private JarFileTree treeFiles;
	private TabbedPanel tabbedContent;
	private JMenu mnSearch;

	public Gui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings("serial")
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
				JFileChooser chooser = recaf.fileChoosers.getFileChooser();
				int val = chooser.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					try {
						recaf.openFile(chooser.getSelectedFile());
					} catch (Exception e1) {
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
				JFileChooser chooser = recaf.fileChoosers.createFileSaver();
				int val = chooser.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					try {
						recaf.saveFile(chooser.getSelectedFile());
					} catch (Exception e1) {
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
		 * recaf.history.undoLast(); } }); mnEdit.add(mntmUndo);
		 * menuBar.add(mnEdit);
		 */

		JMenu mnOptions = new JMenu("Options");
		mnOptions.add(new ActionCheckBox("Show jump hints", recaf.options.opcodeShowJumpHelp,b -> recaf.options.opcodeShowJumpHelp = b));
		mnOptions.add(new ActionCheckBox("Simplify type descriptors", recaf.options.opcodeSimplifyDescriptors,b -> recaf.options.opcodeSimplifyDescriptors = b));
		mnOptions.add(new ActionCheckBox("Advanced Variable Table", recaf.options.showVariableSignatureInTable,b -> recaf.options.showVariableSignatureInTable = b));
		mnOptions.add(new ActionCheckBox("Confirm deletions", recaf.options.confirmDeletions,b -> recaf.options.confirmDeletions = b));
		mnOptions.add(new ActionMenuItem("ASM flags", () -> {
			openTab("ASM Flags", new AsmFlagsPanel());
		}));
		menuBar.add(mnOptions);

		mnSearch = new JMenu("Search");
		mnSearch.setEnabled(false);
		JMenuItem mntmSearch1 = new ActionMenuItem("Strings", () -> openTab("Search: Strings", new SearchPanel(SearchPanel.S_STRINGS)));
		JMenuItem mntmSearch2 = new ActionMenuItem("Fields", () -> openTab("Search: Fields", new SearchPanel(SearchPanel.S_FIELD)));
		JMenuItem mntmSearch3 = new ActionMenuItem("Methods", () -> openTab("Search: Methods", new SearchPanel(SearchPanel.S_METHOD)));
		JMenuItem mntmSearch4 = new ActionMenuItem("Class Name", () -> openTab("Search: Class", new SearchPanel(SearchPanel.S_CLASS_NAME)));
		JMenuItem mntmSearch5 = new ActionMenuItem("Class References", () -> openTab("Search: Class References", new SearchPanel(SearchPanel.S_CLASS_REF)));
		mnSearch.add(mntmSearch1);
		mnSearch.add(mntmSearch2);
		mnSearch.add(mntmSearch3);
		mnSearch.add(mntmSearch4);
		mnSearch.add(mntmSearch5);
		menuBar.add(mnSearch);

		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(true);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		treeFiles = new JarFileTree();
		splitPane.setLeftComponent(treeFiles);
		treeFiles.setDropTarget(new DropTarget() {
			@Override
			public final void drop(final DropTargetDropEvent event) {
				try {
					event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					if (transferData == null) {
						return;
					}
					@SuppressWarnings("unchecked")
					List<File> ls = (List<File>) transferData;
					File file = ls.get(0);
					if (ls.size() > 1) {
						JOptionPane.showMessageDialog(null, "Only one file can be accepted. Going with: " + file);
					}
					if (file.getName().toLowerCase().endsWith(".jar")) {
						recaf.openFile(file);
					} else {
						JOptionPane.showMessageDialog(null, "Input was not a java archive (jar).");
					}
				} catch (UnsupportedFlavorException ex) {
					JOptionPane.showMessageDialog(null, "Input was not a java archive (jar).");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		tabbedContent = new TabbedPanel();
		splitPane.setRightComponent(tabbedContent);

	}

	/**
	 * Creates a new tab with the text of the exception.
	 *
	 * @param e The exception.
	 */
	public void displayError(Throwable e) {
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
	 * @param node The node.
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
		mnSearch.setEnabled(true);
		treeFiles.refresh();
	}

	public JFrame getFrame() {
		return frame;
	}

}
