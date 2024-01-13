package software.coley.recaf.services.decompile.vineflower;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

/**
 * Logger for Vineflower
 *
 * @author therathatter
 */
public class VineflowerLogger extends IFernflowerLogger {
    private static final Logger logger = Logging.get(VineflowerLogger.class);

    @Override
    public void writeMessage(String s, Severity severity) {
        switch (severity) {
            case TRACE -> logger.trace(s);
            case INFO -> logger.info(s);
            case WARN -> logger.warn(s);
            case ERROR -> logger.error(s);
        }
    }

    @Override
    public void writeMessage(String s, Severity severity, Throwable throwable) {
        writeMessage(s, severity);
    }
}
