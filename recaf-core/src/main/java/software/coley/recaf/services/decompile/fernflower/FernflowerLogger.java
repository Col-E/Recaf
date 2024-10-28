package software.coley.recaf.services.decompile.fernflower;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.Logging;

/**
 * Logger for Fernflower
 *
 * @author meiMingle
 */
public class FernflowerLogger extends IFernflowerLogger {
	private static final Logger logger = Logging.get(FernflowerLogger.class);
	private static final String VF_PREFIX = "VF: ";
	private final ObservableObject<Level> level;

	public FernflowerLogger(@Nonnull FernflowerConfig config) {
		this.level = config.getLoggingLevel();
	}

	@Override
	public void writeMessage(String message, Severity severity) {
		switch (severity) {
			case TRACE -> {
				if (level.getValue().compareTo(Level.TRACE) >= 0) logger.trace(VF_PREFIX + message);
			}
			case INFO -> {
				if (level.getValue().compareTo(Level.INFO) >= 0) logger.info(VF_PREFIX + message);
			}
			case WARN -> {
				if (level.getValue().compareTo(Level.WARN) >= 0) logger.warn(VF_PREFIX + message);
			}
			case ERROR -> logger.error(VF_PREFIX + message);
		}
	}

	@Override
	public void writeMessage(String message, Severity severity, Throwable throwable) {
		logger.error(VF_PREFIX + message, throwable);
	}
}
