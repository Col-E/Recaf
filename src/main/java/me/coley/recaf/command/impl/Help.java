package me.coley.recaf.command.impl;

import me.coley.recaf.command.MetaCommand;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;

import static me.coley.recaf.util.Log.*;

/**
 * Command for quitting Recaf.
 *
 * @author Matt
 */
@CommandLine.Command(name = "help", description = "Prints usage for the specified command", helpCommand = true)
public class Help extends MetaCommand implements Callable<Void> {
	@CommandLine.Parameters(index = "0",  description = "The command to show usage for.", arity = "0..1")
	public String command;
	@CommandLine.Parameters(index = "1",  description = "The sub command name.", arity = "0..1", hidden = true)
	public String subcommand;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, help command missing cli context</li></ul>
	 */
	@Override
	public Void call() throws Exception {
		// Get root command
		String key = command;
		CommandLine cmd = context.getSubcommands().get(key);
		// Check if subcommand is specified
		if (cmd != null && subcommand != null) {
			key += " " + subcommand;
			cmd = cmd.getSubcommands().get(subcommand);
		}
		// Check if fetched command exists, if so print usage
		// Otherwise list all commands
		if (cmd != null)
			cmd.usage(context.getOut());
		else {
			StringBuilder sb = new StringBuilder();
			if (command != null)
				sb.append("No such command: '").append(key).append("'\n");
			else
				sb.append("Specify a command to see it's usage.\n");
			sb.append("The existing commands are:");
			// Sort commands alphabetically
			List<CommandLine> list = new ArrayList<>(context.getSubcommands().values());
			list.sort(Comparator.comparing(CommandLine::getCommandName));
			for (CommandLine com : list)
				sb.append("\n - ").append(com.getCommandName());
			info(sb.toString());
		}
		return null;
	}
}
