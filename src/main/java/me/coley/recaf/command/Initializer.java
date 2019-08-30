package me.coley.recaf.command;

import me.coley.recaf.Recaf;
import picocli.CommandLine;

import java.io.File;

/**
 * Command line initializer for Recaf, invoked from the main method.
 *
 * @author Matt
 */
@CommandLine.Command(
		name = "Recaf",
		version = Recaf.VERSION,
		description = "Recaf: A modern java bytecode editor.")
public class Initializer implements Runnable {
	/**
	 * Workspace file to import.
	 */
	@CommandLine.Option(names = {"--input" }, description = "The input file to load. Supported types are: class, jar, json")
	public File input;
	@CommandLine.Option(names = {"--script" }, description = "Script file to load for cli usage")
	public File script;
	@CommandLine.Option(names = { "--cli" }, description = "Run Recaf via CLI")
	public boolean cli;

	@Override
	public void run() {
		Controller controller;
		if (cli || script != null)
			controller = new HeadlessController(input, script);
		else
			controller = new GuiController(input);
		controller.setup();
		controller.run();
	}
}