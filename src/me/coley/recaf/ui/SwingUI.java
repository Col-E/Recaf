package me.coley.recaf.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.LaunchParams;
import me.coley.recaf.Recaf;
import me.coley.recaf.agent.Agent;
import me.coley.recaf.agent.Attach;
import me.coley.recaf.agent.Marker;
import me.coley.recaf.plugin.Plugin;
import me.coley.recaf.ui.component.JVMMenu;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.panel.AsmFlagsPanel;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.ui.component.panel.SearchPanel;
import me.coley.recaf.ui.component.panel.SearchPanel.Results;
import me.coley.recaf.ui.component.panel.SearchPanel.SearchType;
import me.coley.recaf.ui.component.panel.TabbedPanel;
import me.coley.recaf.ui.component.panel.UiOptionsPanel;
import me.coley.recaf.ui.component.tree.JarFileTree;
import me.coley.recaf.util.FilePrompt;

public class SwingUI {
	private final JMenuBar menuBar = new JMenuBar();
	private final JMenu mnSearch;
	private final JMenu mnPlugins;
	public final JFrame frame;
	public final TabbedPanel tabs = new TabbedPanel();
	public JarFileTree classTree = new JarFileTree();

	public SwingUI() {
		frame = new JFrame(Lang.get("title.default"));
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (Recaf.INSTANCE.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, Lang.get("misc.warn.exit"), "Warning",
							JOptionPane.YES_NO_OPTION);
					if (dialogResult == JOptionPane.YES_OPTION) {
						frame.dispose();
					}
				} else {
					frame.dispose();
				}
			}
		});
		mnSearch = new JMenu(Lang.get("navbar.search"));
		mnPlugins = new JMenu(Lang.get("navbar.plugin"));
	}

	public void init(LaunchParams params) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					frame.setSize(params.getSize());
					frame.setJMenuBar(menuBar);
					frame.getContentPane().setLayout(new BorderLayout(0, 0));
					setupMenu();
					setupPane();
				} catch (Exception exception) {
					Recaf.INSTANCE.logging.error(exception);
				}
			}
		});
	}

	/**
	 * Adds menu items to the menu bar.
	 */
	private void setupMenu() {
		// Disable certain menu's by default
		mnSearch.setEnabled(false);
		mnPlugins.setEnabled(false);

		// Setup file sub-menu
		JMenu mnFile = new JMenu(Lang.get("navbar.file"));
		if (!Agent.active()) {
			mnFile.add(new ActionMenuItem(Lang.get("navbar.file.open"), () -> {
				JFileChooser chooser = FilePrompt.getLoader();
				int val = chooser.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					try {
						Recaf.INSTANCE.selectInput(chooser.getSelectedFile());
					} catch (Exception e) {
						Recaf.INSTANCE.logging.error(e);
					}
				}
			}));
		}
		mnFile.add(new ActionMenuItem(Lang.get("navbar.file.save"), () -> {
			JFileChooser chooser = FilePrompt.getSaver();
			int val = chooser.showOpenDialog(null);
			if (val == JFileChooser.APPROVE_OPTION) {
				try {
					Recaf.INSTANCE.save(chooser.getSelectedFile());
				} catch (Exception e) {
					Recaf.INSTANCE.logging.error(e);
				}
			}
		}));
		menuBar.add(mnFile);
		// Setup options sub-menu
		JMenu mnOptions = new JMenu(Lang.get("navbar.options"));
		mnOptions.add(new ActionMenuItem(Lang.get("navbar.options.ui"), () -> {
			openTab(Lang.get("option.ui.title"), new UiOptionsPanel());
		}));
		mnOptions.add(new ActionMenuItem(Lang.get("navbar.options.asm"), () -> {
			openTab(Lang.get("option.asm.title"), new AsmFlagsPanel());
		}));
		menuBar.add(mnOptions);
		// Setup search sub-menu
		// Instead of manual, iterate through an enum to do this
		for (SearchType type : SearchType.values()) {
			mnSearch.add(new ActionMenuItem(Lang.get(type.getKey()), () -> openSearch(type)));
		}
		menuBar.add(mnSearch);
		// Setup agent sub-menu
		if (!Attach.fail) {
			JMenu mnAgent = new JMenu(Lang.get("navbar.agent"));
			if (Agent.active()) {
				mnAgent.add(new ActionMenuItem(Lang.get("navbar.agent.refresh"), () -> {
					refreshTree();
				}));
				mnAgent.add(new ActionMenuItem(Lang.get("navbar.agent.mark"), () -> {
					if (tabs.getTabCount() > 0) {
						String title = tabs.getTitleAt(tabs.getSelectedTab());
						if (Recaf.INSTANCE.jarData.classes.containsKey(title)) {
							Marker.mark(title);
						}
					}
				}));
				mnAgent.add(new ActionMenuItem(Lang.get("navbar.agent.apply"), () -> {
					Agent.apply();
				}));
			} else {
				mnAgent.add(new JVMMenu());
			}
			menuBar.add(mnAgent);
		}
		// Setup plugins sub-menu
		for (Entry<String, Plugin> entry : Recaf.INSTANCE.plugins.getPlugins().entrySet()) {
			mnPlugins.add(new ActionMenuItem(entry.getKey(), () -> entry.getValue().onMenuClick()));
		}
		menuBar.add(mnPlugins);
	}

	/**
	 * Adds content to the primary splitpane.
	 */
	private void setupPane() {
		// Setup splitpane, will host main content such as the file tree and the
		// tabbed panel which will hold classes and other menus.
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.1);
		splitPane.setOneTouchExpandable(true);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		splitPane.setLeftComponent(classTree);
		splitPane.setRightComponent(tabs);
	}

	/**
	 * Makes the ui visible.
	 */
	public void setVisible() {
		frame.setVisible(true);
	}

	/**
	 * Set the window title for the given duration.
	 * 
	 * @param title
	 *            Text to set.
	 * @param duration
	 *            Time for text to remain.
	 */
	public void setTempTile(String title, int duration) {
		String old = frame.getTitle();
		frame.setTitle(title);
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {}
				frame.setTitle(old);
			}
		}.start();
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
	public JComponent openTab(String title, JComponent component) {
		if (tabs.hasCached(title)) {
			tabs.setSelectedTab(tabs.getCachedIndex(title));
			return tabs.getChild(title);
		} else {
			tabs.addTab(title, component);
			int i = tabs.getCachedIndex(title);
			if (i == -1) {
				i = tabs.getTabCount() - 1;
			}
			tabs.setSelectedTab(i);
			return component;
		}
	}

	/**
	 * Opens up a class tab for the given class-node, or opens an existing page
	 * if one is found.
	 *
	 * @param node
	 *            The node.
	 */
	public ClassDisplayPanel openClass(ClassNode node) {
		return (ClassDisplayPanel) openTab(node.name, new ClassDisplayPanel(node));
	}

	/**
	 * Opens a search of the given mode in a new tab.
	 * 
	 * @param type
	 *            Search type.
	 * @param args
	 *            Default values of the search inputs.
	 */
	public Results openSearch(SearchType type, String... args) {
		return openSearch(type, true, args);
	}

	/**
	 * Opens a search of the given mode in a new tab.
	 * 
	 * @param type
	 *            Search type.
	 * @param tab
	 *            If a new tab should be opened.
	 * @param args
	 *            Default values of the search inputs.
	 */
	public Results openSearch(SearchType type, boolean tab, String... args) {
		SearchPanel search = new SearchPanel(type, args);
		if (tab) openTab(Lang.get("search.title") + Lang.get(type.getKey()), search);
		return search.getResults();
	}

	/**
	 * Creates a new tab with the text of the exception.
	 *
	 * @param exception
	 *            The exception.
	 */
	public void openException(Exception exception) {
		JTextArea text = new JTextArea();
		text.setEditable(false);
		text.append(exception.getClass().getSimpleName() + ":\n");
		text.append("Message: " + exception.getMessage() + "\n");
		text.append("Trace: \n");
		for (StackTraceElement element : exception.getStackTrace()) {
			text.append(element.toString() + "\n");
		}
		Throwable cause = exception.getCause();
		if (cause != null) {
			text.append("Cause: " + cause + "\n");
			for (StackTraceElement element : cause.getStackTrace()) {
				text.append(element.toString() + "\n");
			}
		}
		openTab("Error: " + exception.getClass().getSimpleName(), new JScrollPane(text));
	}

	/**
	 * Creates a new tab with the given text.
	 * 
	 * @param title
	 *            Title of tab.
	 * @param content
	 *            Content of tab.
	 */
	public void openMessage(String title, String content) {
		JTextArea text = new JTextArea();
		text.setEditable(false);
		text.append(content);
		openTab(title, new JScrollPane(text));
	}

	/**
	 * Refresh the UI after setting the given look and feel.
	 * 
	 * @param lookAndFeel
	 *            Look and feel to set.
	 */
	public void refreshLAF(String lookAndFeel) {
		// Run the LAF update on the AWT event-dispatch thread.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					// Set the look and feel, refresh components to apply
					// changes
					UIManager.setLookAndFeel(lookAndFeel);
					refresh();
				} catch (Exception e) {}
			}
		});
	}

	/**
	 * Refreshes the tree to display the current jar file.
	 */
	public void refreshTree() {
		// Enable menus since this implies a jar has been loaded.
		mnSearch.setEnabled(true);
		mnPlugins.setEnabled(true);
		// Refresh tree UI.
		classTree.refresh();
	}

	/**
	 * Refresh the UI.
	 */
	public void refresh() {
		// Refresh the UI.
		if (frame != null) {
			SwingUtilities.updateComponentTreeUI(frame);
		}
	}
}
