package me.coley.recaf.decompile.fernflower;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;

import static me.coley.recaf.util.Log.*;

/**
 * FernFlower logger implementation.
 *
 * @author Matt
 */
public class FernFlowerLogger extends IFernflowerLogger {
	@Override
	public void writeMessage(String message, Severity severity) {
		if(!accepts(severity))
			return;
		switch(severity) {
			case TRACE:
				trace(message);
				break;
			case INFO:
				info(message);
				break;
			case WARN:
				warn(message);
				break;
			case ERROR:
				error(message);
				break;
			default:
				break;
		}
	}

	@Override
	public void writeMessage(String message, Severity severity, Throwable throwable) {
		writeMessage(message, severity);
		error(throwable, message);
	}
}
