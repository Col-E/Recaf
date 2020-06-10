package me.coley.recaf.command.impl;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Command for quitting Recaf.
 *
 * @author Matt
 */
@CommandLine.Command(name = "quit", description = "Closes Recaf")
public class Quit implements Callable<Void> {
	@CommandLine.Option(names = { "--force" },  description = "Immediately/forcefully exit.")
	public boolean force;

	@Override
	public Void call() throws Exception {
		// Hard exit
		if (force)
			System.exit(0);
		// Handled externally
		return null;
	}
}
