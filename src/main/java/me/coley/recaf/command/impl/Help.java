package me.coley.recaf.command.impl;

import org.tinylog.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command for quitting Recaf.
 *
 * @author Matt
 */
@CommandLine.Command(name = "help", description = "Prints usage for the specified command", helpCommand = true)
public class Help implements Callable<Void> {
	public CommandLine context;
	@CommandLine.Parameters(index = "0",  description = "The command to show usage for.", arity = "0..1")
	public String command;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, help command missing cli context</li></ul>
	 */
	@Override
	public Void call() throws Exception {
		if (context == null)
			throw new IllegalStateException("Help command was not provided with command-line context!");
		CommandLine subcommand = context.getSubcommands().get(command);
		if (subcommand != null)
			subcommand.usage(context.getOut());
		else {
			StringBuilder sb = new StringBuilder();
			if (command != null)
				sb.append("No such command: '" + command + "'\n");
			else
				sb.append("Specify a command to see it's usage.\n");
			sb.append("The existing commands are:");
			for (CommandLine com : context.getSubcommands().values())
				sb.append("\n - " + com.getCommandName());
			Logger.error(sb.toString());
		}
		return null;
	}
}
