package me.coley.recaf.command.impl;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.workspace.InstrumentationResource;
import picocli.CommandLine;

import java.io.File;

/**
 * Command line initializer for Recaf, invoked from the main method.
 * Sets up the controller to use then starts it.
 *
 * @author Matt
 */
@CommandLine.Command(
		name = "Recaf",
		version = Recaf.VERSION,
		description = "Recaf: A modern java bytecode editor.",
		mixinStandardHelpOptions = true)
public class Initializer implements Runnable {
	@CommandLine.Option(names = {"--input" }, description = "The input file to load. " +
			"Supported types are: class, jar, json")
	public File input;
	@CommandLine.Option(names = {"--script" }, description = "Script file to load for cli usage")
	public File script;
	@CommandLine.Option(names = { "--cli" }, description = "Run Recaf via CLI")
	public boolean cli;
	@CommandLine.Option(names = { "--instrument" }, description = "Indicates Recaf has been invoked as an agent")
	public boolean instrument;
	//
	private Controller controller;

	@Override
	public void run() {
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
}