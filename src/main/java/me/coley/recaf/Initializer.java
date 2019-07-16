package me.coley.recaf;

import picocli.CommandLine;

import java.io.File;

/**
 * Command line initializer for Recaf.
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
	@CommandLine.Option(names = {"--workspace", "-w"}, description = "The workspace to load.")
	public File workspace;

	@Override
	public void run() {

	}
}
