package software.coley.recaf.util;

import software.coley.recaf.analytics.SystemInformation;

/**
 * Operating system enumeration.
 *
 * @author Matt Coley
 */
public enum PlatformType {
	WINDOWS,
	MAC,
	LINUX;

	/**
	 * @return Operating system type.
	 */
	public static PlatformType get() {
		String osName = SystemInformation.OS_NAME.toLowerCase();
		if (osName.contains("win"))
			return WINDOWS;
		if (osName.contains("mac") || osName.contains("osx"))
			return MAC;
		return LINUX;
	}
}