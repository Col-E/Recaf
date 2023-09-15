package software.coley.recaf.info;

/**
 * Outline of a WAR file container.
 *
 * @author Matt Coley
 */
public interface WarFileInfo extends ZipFileInfo {
	/**
	 * WAR files prefix their class names with this.
	 */
	String WAR_CLASS_PREFIX = "WEB-INF/classes/";
}
