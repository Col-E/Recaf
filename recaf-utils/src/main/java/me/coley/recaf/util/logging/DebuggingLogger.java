package me.coley.recaf.util.logging;

import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * Used for verbose logging that we normally would not want to capture due to excessiveness.
 * But in the case where we want to enable it for local testing, its available.
 *
 * @author Matt Coley
 */
public interface DebuggingLogger extends Logger {
	boolean MANUAL_DEBUG = false;

	/**
	 * Only do the given action when manual debugging is enabled.
	 *
	 * @param action
	 * 		Call onto self.
	 */
	default void debugging(Consumer<DebuggingLogger> action) {
		if (MANUAL_DEBUG)
			action.accept(this);
	}
}
