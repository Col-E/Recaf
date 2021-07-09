package me.coley.recaf.launch;

import me.coley.recaf.presentation.PresentationType;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Parameters for how Recaf should behave on initialization.
 *
 * @author Matt Coley
 */
public class InitializerParameters implements Callable<Void> {
	@CommandLine.Option(names = {"-t", "--type"}, description = "Presentation type")
	private final PresentationType presentationType;

	/**
	 * @param presentationType
	 * 		Presentation type, graphical or headless.
	 */
	public InitializerParameters(PresentationType presentationType) {
		this.presentationType = presentationType;
	}

	/**
	 * @return Default parameters for UI usage.
	 */
	public static InitializerParameters fromDefaultUI() {
		return new InitializerParameters(PresentationType.GUI);
	}

	/**
	 * @return Default parameters for headless usage.
	 */
	public static InitializerParameters fromDefaultHeadless() {
		return new InitializerParameters(PresentationType.HEADLESS);
	}

	/**
	 * @return Default parameters for no presentation layer usage.
	 */
	public static InitializerParameters fromDefaultNoDisplay() {
		return new InitializerParameters(PresentationType.NONE);
	}

	/**
	 * @param args
	 * 		Startup parameters as string array.
	 *
	 * @return Startup parameters.
	 */
	public static InitializerParameters fromArgs(String[] args) {
		InitializerParameters baseline = fromDefaultUI();
		new CommandLine(baseline).execute(args);
		return baseline;
	}

	/**
	 * @return Presentation UI type.
	 */
	public PresentationType getPresentationType() {
		return presentationType;
	}

	@Override
	public Void call() throws Exception {
		// No-op
		return null;
	}
}
