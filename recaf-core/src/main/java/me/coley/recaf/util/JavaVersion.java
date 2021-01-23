package me.coley.recaf.util;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

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
	private static final int FALLBACK_VERSION = 8;
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
}
