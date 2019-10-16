package me.coley.recaf.command.impl;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Command for executing external commands from Recaf.
 * Useful for scripting when dealing with external tools.
 *
 * @author Matt
 */
@CommandLine.Command(name = "run", description = "Run an external command.")
public class Run implements Callable<Process> {
	@CommandLine.Parameters(index = "0",  description = "Command to run.")
	public String command;
	@CommandLine.Option(names = { "--waitComplete" },  description = "Wait until the command process finishes.")
	public boolean waitUntilCompletion;

	/**
	 * @return Externally run process.
	 *
	 * @throws Exception
	 * 		<ul><li>IOException, misc process i/o error</li></ul>
	 * 		<ul><li>SecurityException, not allowed to run 'exec'</li></ul>
	 */
	@Override
	public Process call() throws Exception {
		if (command == null || command.isEmpty())
			throw new IllegalArgumentException("No command has been given!");
		Process process = Runtime.getRuntime().exec(command);
		while(waitUntilCompletion && process.isAlive())
			Thread.sleep(100);
		return process;
	}
}
