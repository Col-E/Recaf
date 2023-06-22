package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;

/**
 * Validates JFX is on the classpath.
 *
 * @author Matt Coley
 */
public class JFXValidation {
	public static final int SUCCESS = 0;
	public static final int ERR_CLASS_NOT_FOUND = 100;
	public static final int ERR_NO_SUCH_METHOD = 101;
	public static final int ERR_INVOKE_TARGET = 102;
	public static final int ERR_ACCESS_TARGET = 103;
	public static final int ERR_UNKNOWN = 200;
	private static final Logger logger = Logging.get(JFXValidation.class);

	/**
	 * Ensures that the JavaFX runtime is on the class path.
	 */
	public static int validateJFX() {
		try {
			String jfxVersionClass = "com.sun.javafx.runtime.VersionInfo";
			Class<?> versionClass = Class.forName(jfxVersionClass);
			ReflectUtil.getDeclaredMethod(versionClass, "setupSystemProperties").invoke(null);
			logger.info("JavaFX initialized: {}", System.getProperty("javafx.version"));
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