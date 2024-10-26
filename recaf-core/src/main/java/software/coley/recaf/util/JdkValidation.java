package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.ExitCodes;
import software.coley.recaf.ExitDebugLoggingHook;
import software.coley.recaf.analytics.logging.Logging;

/**
 * JDK vs JRE validation utils.
 *
 * @author Matt Coley
 */
public class JdkValidation {
	private static final Logger logger = Logging.get(JdkValidation.class);

	/**
	 * Applies a few assorted checks to ensure we are running on a JDK and not a JRE.
	 */
	public static void validateJdk() {
		try {
			Class.forName("com.sun.tools.attach.VirtualMachine");
		} catch (ClassNotFoundException ex) {
			logger.error("Recaf must be run with a JDK, but was run with a JRE: " + System.getProperty("java.home"));
			ExitDebugLoggingHook.exit(ExitCodes.ERR_NOT_A_JDK);
		}
	}
}
