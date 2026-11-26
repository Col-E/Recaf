package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;

/**
 * Supported Java version of the current JVM.
 *
 * @author Matt Coley
 */
public class JavaVersion {
	/**
	 * The offset from which a version and the version constant value is. For example, Java 8 is 52 <i>(44 + 8)</i>.
	 */
	public static final int VERSION_OFFSET = 44;
	private static final Logger logger = Logging.get(JavaVersion.class);

	/**
	 * Get the supported Java version of the current JVM.
	 *
	 * @return Version.
	 */
	public static int get() {
		return Runtime.version().feature();
	}

	/**
	 * Adapts the class file spec version to the familiar release versions.
	 * For example, 52 becomes Java 8.
	 *
	 * @param version
	 * 		Class file version, such as from {@link JvmClassInfo#getVersion()}.
	 *
	 * @return Version.
	 */
	public static int adaptFromClassFileVersion(int version) {
		return version - VERSION_OFFSET;
	}

	/**
	 * Adapts the Java language version to the class file spec version.
	 * For example, Java 8 becomes 52.
	 *
	 * @param version
	 * 		Java language version, such as from {@link Runtime.Version#feature()}.
	 *
	 * @return Class file version, such as from {@link JvmClassInfo#getVersion()}.
	 */
	public static int adaptFromLanguageVersion(int version) {
		return version + VERSION_OFFSET;
	}
}
