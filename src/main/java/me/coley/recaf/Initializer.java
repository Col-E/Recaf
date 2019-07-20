package me.coley.recaf;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	@CommandLine.Option(names = { "-s", "--skip" }, description = "Blacklisted packages to ignore.", arity = "0..*")
	public List<String> skipped = new ArrayList<>();

	@Override
	public void run() {

	}
}
// TODO: mappings file (used by JavaResource implmentations)