package me.coley.recaf.command.impl;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Command for delaying Recaf.
 * Useful for scripting when dealing with external tools.
 *
 * @author Matt
 */
@CommandLine.Command(name = "wait", description = "Delays Recaf")
public class Wait implements Callable<Void> {
	@CommandLine.Parameters(index = "0",  description = "Milliseconds to wait for.")
	public long millis;

	@Override
	public Void call() throws Exception {
		Thread.sleep(millis);
		return null;
	}
}
