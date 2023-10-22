package software.coley.recaf.util;

import org.slf4j.Logger;
import regexodus.Matcher;
import software.coley.recaf.analytics.logging.Logging;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;

/**
 * Validates JFX is on the classpath.
 *
 * @author Matt Coley
 */
public class JFXValidation {
	public static final int MIN_JFX_VERSION = 19;
	public static final int SUCCESS = 0;
	public static final int ERR_UNKNOWN = 100;
	public static final int ERR_CLASS_NOT_FOUND = 101;
	public static final int ERR_NO_SUCH_METHOD = 102;
	public static final int ERR_INVOKE_TARGET = 103;
	public static final int ERR_ACCESS_TARGET = 104;
	public static final int ERR_OLD_JFX_VERSION = 105;
	public static final int ERR_UNKNOWN_JFX_VERSION = 106;
	private static final Logger logger = Logging.get(JFXValidation.class);

	/**
	 * Ensures that the JavaFX runtime is on the class path.
	 */
	public static int validateJFX() {
		try {
			String jfxVersionClass = "com.sun.javafx.runtime.VersionInfo";
			Class<?> versionClass = Class.forName(jfxVersionClass);
			ReflectUtil.getDeclaredMethod(versionClass, "setupSystemProperties").invoke(null);

			String versionProperty = System.getProperty("javafx.version");
			Matcher versionMatcher = RegexUtil.getMatcher("\\d+", versionProperty);

			if (versionMatcher.find()) {
				int majorVersion = Integer.parseInt(versionMatcher.group());
				if (majorVersion < MIN_JFX_VERSION) {
					logger.error("JavaFX version {} is present, but Recaf requires {}+", majorVersion, MIN_JFX_VERSION);
					return ERR_OLD_JFX_VERSION;
				}
			} else {
				logger.error("JavaFX version {} does not declare a major release version, cannot validate compatibility", versionProperty);
				return ERR_UNKNOWN_JFX_VERSION;
			}

			logger.info("JavaFX successfully initialized: {}", versionProperty);
			return SUCCESS;
		} catch (ClassNotFoundException ex) {
			logger.error("JFX validation failed, could not find 'VersionInfo' class", ex);
			return ERR_CLASS_NOT_FOUND;
		} catch (NoSuchMethodException ex) {
			logger.error("JFX validation failed, could not find 'setupSystemProperties' in 'VersionInfo'", ex);
			return ERR_NO_SUCH_METHOD;
		} catch (InvocationTargetException ex) {
			logger.error("JFX validation failed, failed to invoke 'setupSystemProperties'", ex);
			return ERR_INVOKE_TARGET;
		} catch (IllegalAccessException | InaccessibleObjectException ex) {
			logger.error("JFX validation failed, failed to invoke 'setupSystemProperties'", ex);
			return ERR_ACCESS_TARGET;
		} catch (Exception ex) {
			logger.error("JFX validation failed due to unhandled exception", ex);
			return ERR_UNKNOWN;
		}
	}
}