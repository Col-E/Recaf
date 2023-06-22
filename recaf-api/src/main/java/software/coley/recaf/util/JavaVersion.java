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
	private static final String JAVA_CLASS_VERSION = "java.class.version";
	private static final String JAVA_VM_SPEC_VERSION = "java.vm.specification.version";
	private static final int FALLBACK_VERSION = 11;
	private static final Logger logger = Logging.get(JavaVersion.class);
	private static int version = -1;

	/**
	 * Get the supported Java version of the current JVM.
	 *
	 * @return Version.
	 */
	public static int get() {
		if (version < 0) {
			// Check for class version
			String property = System.getProperty(JAVA_CLASS_VERSION, "");
			if (!property.isEmpty())
				return version = (int) (Float.parseFloat(property) - VERSION_OFFSET);

			// Odd, not found. Try the spec version
			logger.warn("Property '{}' not found, using '{}' as fallback",
					JAVA_CLASS_VERSION,
					JAVA_VM_SPEC_VERSION
			);
			property = System.getProperty(JAVA_VM_SPEC_VERSION, "");
			if (property.contains("."))
				return version = (int) Float.parseFloat(property.substring(property.indexOf('.') + 1));
			else if (!property.isEmpty())
				return version = Integer.parseInt(property);
			logger.warn("Property '{}' not found, using '{}' as fallback",
					JAVA_VM_SPEC_VERSION, FALLBACK_VERSION
			);

			// Very odd
			return FALLBACK_VERSION;
		}
		return version;
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
}
