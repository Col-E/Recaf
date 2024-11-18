package software.coley.recaf.analytics.logging;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.util.function.Consumer;

/**
 * Used for verbose logging that we normally would not want to capture due to excessiveness.
 * But in the case where we want to enable it for local testing, its available.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Logging not relevant for test coverage")
public interface DebuggingLogger extends Logger {
	boolean DEBUG = System.getenv("RECAF_DEBUG") != null;

	/**
	 * Only do the given action when manual debugging is enabled.
	 *
	 * @param action
	 * 		Call onto self.
	 */
	default void debugging(@Nonnull Consumer<DebuggingLogger> action) {
		if (DEBUG)
			action.accept(this);
	}
}
