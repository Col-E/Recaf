package software.coley.recaf.info;

/**
 * Outline of a JVM JMod file.
 * These files are found at {@code %JAVA%/jmods/}.
 *
 * @author Matt Coley
 */
public interface JModFileInfo extends ZipFileInfo {
	/**
	 * Classes in the JMod archive are prefixed with this path.
	 */
	String CLASSES_PREFIX = "classes/";
}
