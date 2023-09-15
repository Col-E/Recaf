package software.coley.recaf.info;

/**
 * Outline of a JAR file container.
 *
 * @author Matt Coley
 */
public interface JarFileInfo extends ZipFileInfo {
	/**
	 * Multi-release JARs prefix class names with this, plus the target version.
	 * For example: Multiple versions of {@code foo/Bar.class}
	 * <ul>
	 *     <li>{@code foo/Bar.class}</li>
	 *     <li>{@code META-INF/versions/9/foo/Bar.class}</li>
	 *     <li>{@code META-INF/versions/11/foo/Bar.class}</li>
	 * </ul>
	 * The first item is used for Java 8.<br>
	 * The second item for Java 9 and 10.<br>
	 * The third item for Java 11+.
	 */
	String MULTI_RELEASE_PREFIX = "META-INF/versions/";
}
