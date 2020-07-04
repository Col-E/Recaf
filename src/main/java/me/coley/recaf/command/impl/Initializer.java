package me.coley.recaf.command.impl;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.InstrumentationResource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Command line initializer for Recaf, invoked from the main method.
 * Sets up the controller to use then starts it.
 *
 * @author Matt
 */
@Command(
		name = "Recaf",
		version = Recaf.VERSION,
		description = "Recaf: A modern java bytecode editor.",
		mixinStandardHelpOptions = true)
public class Initializer implements Runnable {
	@Option(names = {"--input" }, description = "The input file to load. " +
			"Supported types are: class, jar, json")
	public Path input;
	@Option(names = {"--script" }, description = "Script file to load for cli usage")
	public Path script;
	@Option(names = { "--cli" }, description = "Run Recaf via CLI")
	public boolean cli;
	@Option(names = { "--instrument" }, description = "Indicates Recaf has been invoked as an agent")
	public boolean instrument;
	@Option(names = { "--noupdate" }, description = "Disable update checking entirely")
	public boolean noUpdates;
	//
	private Controller controller;

	@Override
	public void run() {
		// Disable update check
		if (noUpdates)
			SelfUpdater.disable();
		// Setup controller
		boolean headless = isHeadless();
		if (headless)
			controller = new HeadlessController(input, script);
		else
			controller = new GuiController(input);
		controller.setup();
		if(instrument)
			InstrumentationResource.setup(controller);
		else if (controller.config().backend().firstTime)
			promptFirstTime();
	}

	/**
	 * Start the controller.
	 */
	public void startController() {
		controller.run();
	}

	/**
	 * Show documentation nag.
	 */
	private void promptFirstTime() {
		// Unmark value
		controller.config().backend().firstTime = false;
		// Determine how to show prompt
		boolean doShowDocs = false;
		if (isHeadless()) {
			Log.info(LangUtil.translate("misc.firsttime.cli"));
			String line = new Scanner(System.in).nextLine();
			if (line != null) {
				line = line.trim().toLowerCase();
				doShowDocs = line.contains("ok") | line.contains("y");
			}
		} else {
			try {
				UIManager.setLookAndFeel(
						UIManager.getSystemLookAndFeelClassName());
			} catch (Throwable t) { /* Whatever... So it looks ugly once. Who cares */ }
			int value = JOptionPane.showConfirmDialog(null,
					LangUtil.translate("misc.firsttime.gui"),
					LangUtil.translate("misc.firsttime.gui.title"),
					JOptionPane.OK_CANCEL_OPTION);
			if (value == JOptionPane.OK_OPTION) {
				doShowDocs = true;
			}
		}
		// Show 'em
		if (doShowDocs) {
			try {
				Desktop.getDesktop().browse(new URL(Recaf.DOC_URL).toURI());
			} catch(Exception ex) {
				Log.error(ex, "Failed to open documentation url");
			}
		}
	}

	/**
	 * @return {@code true} when Recaf should not allocate a UI.
	 */
	private boolean isHeadless() {
		return cli || script != null;
	}

	/**
	 * @return The controller to execute Recaf with.
	 */
	public Controller getController() {
		return controller;
	}
}