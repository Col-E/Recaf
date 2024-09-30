package software.coley.recaf.workspace.model.bundle;

import software.coley.recaf.info.JarFileInfo;

/**
 * Bundle of versioned JVM classes in a JAR under the {@link JarFileInfo#MULTI_RELEASE_PREFIX} .
 *
 * @author Matt Coley
 */
public interface VersionedJvmClassBundle extends JvmClassBundle {
	/**
	 * @return The associated version of classes in this bundle. Uses standard version, so Java 8 is 8.
	 */
	int version();
}
