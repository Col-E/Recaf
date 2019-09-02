package me.coley.recaf.command;

import picocli.CommandLine;

/**
 * Command that requires the CommandLine context.
 *
 * @author Matt
 */
public abstract class MetaCommand {
	protected CommandLine context;

	/**
	 * @param context
	 * 		CommandLine context
	 */
	public void setContext(CommandLine context) {
		this.context = context;
	}
}
