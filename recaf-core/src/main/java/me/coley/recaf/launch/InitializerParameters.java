package me.coley.recaf.launch;

import me.coley.recaf.presentation.PresentationType;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Parameters for how Recaf should behave on initialization.
 *
 * @author Matt Coley
 */
public class InitializerParameters implements Callable<Void> {
	@CommandLine.Option(names = {"-t", "--type"}, description = "Presentation type")
	private PresentationType presentationType = PresentationType.GUI;
	@CommandLine.Option(names = {"-s", "--script"}, description = "Path to Recaf script file")
	private File scriptPath;

	/**
	 * @param args
	 * 		Startup parameters as string array.
	 *
	 * @return Startup parameters.
	 */
	public static InitializerParameters fromArgs(String[] args) {
		InitializerParameters params = new InitializerParameters();
		new CommandLine(params).execute(args);
		return params;
	}

	/**
	 * @return Presentation UI type.
	 */
	public PresentationType getPresentationType() {
		return presentationType;
	}

	/**
	 * @return The script file to run on startup.
	 */
	public File getScriptPath() {
		return scriptPath;
	}

	@Override
	public Void call() throws Exception {
		// No-op
		return null;
	}
}
