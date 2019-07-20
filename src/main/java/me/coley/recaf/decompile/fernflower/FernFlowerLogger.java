package me.coley.recaf.decompile.fernflower;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.pmw.tinylog.Logger;

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
				Logger.trace(message);
				break;
			case INFO:
				Logger.info(message);
				break;
			case WARN:
				Logger.warn(message);
				break;
			case ERROR:
				Logger.error(message);
				break;
			default:
				break;
		}
	}

	@Override
	public void writeMessage(String message, Severity severity, Throwable throwable) {
		writeMessage(message, severity);
		Logger.error(throwable);
	}
}
