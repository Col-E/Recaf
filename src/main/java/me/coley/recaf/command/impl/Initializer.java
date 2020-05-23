package me.coley.recaf.command.impl;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.InstrumentationResource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

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
		boolean headless = cli || script != null;
		if (headless)
			controller = new HeadlessController(input, script);
		else
			controller = new GuiController(input);
		controller.setup();
		if(instrument)
			InstrumentationResource.setup(controller);
	}

	/**
	 * Start the controller.
	 */
	public void startController() {
		controller.run();
	}

	/**
	 * @return The controller to execute Recaf with.
	 */
	public Controller getController() {
		return controller;
	}
}