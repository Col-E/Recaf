package me.coley.recaf;

import java.io.File;
import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import me.coley.logging.Level;
import me.coley.recaf.agent.Attach;
import me.coley.recaf.asm.JarData;
import me.coley.recaf.config.Configs;
import me.coley.recaf.event.Bus;
import me.coley.recaf.event.impl.EClassSelect;
import me.coley.recaf.event.impl.EInit;
import me.coley.recaf.plugin.PluginManager;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.SwingUI;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.util.Swing;
import picocli.CommandLine;

public class Recaf {
	/**
	 * Singleton instance of recaf.
	 */
	public final static Recaf INSTANCE = new Recaf();
	/**
	 * Logging.
	 */
	public final Logging logging = new Logging();
	/**
	 * Event bus.
	 */
	public final Bus bus = new Bus();
	/**
	 * Plugin system.
	 */
	public final PluginManager plugins = new PluginManager();
	/**
	 * Wrapper for multiple configurations.
	 */
	public final Configs configs = new Configs();
	/**
	 * User interface.
	 */
	public SwingUI ui;
	/**
	 * Content of the current jar file.
	 */
	public JarData jarData;

	/**
	 * Setup things.
	 * 
	 * @param params
	 *            Optional command line arguements.
	 */
	private void setup(LaunchParams params) {
		logging.info("Setting up Recaf");
		Swing.init();
		logging.info("Loading config", 1);
		configs.init();
		Lang.load(configs.ui.language);
		if (!params.isAgent) {
			// skip attach setup if already an agent.
			try {
				logging.info("Loading attach api", 1);
				Attach.load();
				Attach.setProviders();
			} catch (Throwable e) {
				Attach.fail = true;
				// ensure you are running recaf via the JDK and not the JRE
				logging.info("Failed to load attach api. Potential solutions:", 1);
				logging.info("Ensure your JDK version matches the JRE version used to execute recaf", 2);
				logging.info("Run recaf with the JDK instead of the JRE", 2);
				logging.info("Attach recaf with '-javaagent' instead of using the UI", 2);
			}
		} else {
			// as an agent, ensure future ASM version not being used in case VM
			// attached to already has an outdated ASM version. If there is none
			// it'll read what recaf uses so non-issue.
			configs.asm.checkVersion();
		}
		logging.info("Creating UI", 1);
		ui = new SwingUI();
		configs.ui.setLookAndFeel(configs.ui.getLookAndFeel());
		ui.init(params);
		logging.info("Loading plugins", 1);
		logging.setLevelConsole(Level.values()[params.logConsole]);
		logging.setLevelFile(Level.values()[params.logFile]);
		plugins.init();
		logging.info("Displaying UI", 1);
		ui.setVisible();
		bus.post(new EInit());
		logging.info("Finished setup");
		if (params.initialFile != null) {
			logging.info("Opening initial file: " + params.initialFile.getName());
			selectInput(params.initialFile);
			if (params.initialClass != null) {
				logging.info("Opening initial class: " + params.initialClass);
				selectClass(params.initialClass);
			}
		}
		if (params.isAgent) {
			loadJarFromVM();
		}
		Swing.fixLaunchLAF();
	}

	/**
	 * Sets the {@link #jarData current loaded jar}.
	 * 
	 * @param in
	 *            File to read class(es) from.
	 */
	public void selectInput(File in) {
		try {
			jarData = new JarData(in);
			ui.refreshTree();
			ui.frame.setTitle(Lang.get("title.prefix") + in.getName());
		} catch (IOException e) {
			logging.error(e);
		}
	}

	/**
	 * Sets the {@link #jarData current loaded jar}, but loades classes from the
	 * current VM.
	 */
	public void loadJarFromVM() {
		try {
			jarData = new JarData();
			ui.refreshTree();
			ui.frame.setTitle(Lang.get("title.agent"));
		} catch (IOException e) {
			logging.error(e);
		}
	}

	/**
	 * Saves the current edits to the given file.
	 * 
	 * @param out
	 *            File to save modifications to.
	 */
	public void save(File out) {
		try {
			if (out.getName().toLowerCase().endsWith(".class")) {
				jarData.saveClass(out);
			} else {
				jarData.saveAsJar(out);
			}
		} catch (IOException e) {
			logging.error(e);
		}
	}

	/**
	 * Opens a class in the gui.
	 * 
	 * @param nodeName
	 *            Name of the class to open. Format is
	 *            <i>com/example/MyClass</i>.
	 */
	public ClassDisplayPanel selectClass(String nodeName) {
		return selectClass(jarData.classes.get(nodeName));
	}

	/**
	 * Opens a class in the gui.
	 * 
	 * @param cn
	 *            Node to open.
	 */
	public ClassDisplayPanel selectClass(ClassNode cn) {
		try {
			if (cn == null) {
				throw new RuntimeException("Node cannot be null!");
			}

			ClassDisplayPanel cdp = ui.openClass(cn);
			bus.post(new EClassSelect(cdp));
			return cdp;
		} catch (Exception e) {
			logging.error(e);
		}
		return null;
	}

	/**
	 * Launch with args parsed by Picocli.
	 * 
	 * @param args
	 *            Command line arguments containing optional arguments.
	 */
	public static void main(String[] args) {
		LaunchParams params = new LaunchParams();
		start(args, params);
	}

	/**
	 * Launch with args to populate the given params object.
	 * 
	 * @param args
	 *            Command line arguments containing optional arguments.
	 * @param params
	 *            Wrapper for argument values.
	 */
	public static void start(String[] args, LaunchParams params) {
		CommandLine.call(params, System.out, args);
		INSTANCE.setup(params);
	}
}
