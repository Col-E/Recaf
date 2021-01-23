package me.coley.recaf.util;

/**
 * Operating system enumeration.
 *
 * @author Matt Coley
 */
public enum OperatingSystem {
	WINDOWS("win"),
	MAC("mac"),
	LINUX("linux");

	private final String mvnName;

	OperatingSystem(String mvnName) {
		this.mvnName = mvnName;
	}

	/**
	 * In some cases, maven artifacts with natives will be published with os-specific suffixes.
	 *
	 * @return Maven artifact name suffix.
	 */
	public String getMvnName() {
		return mvnName;
	}

	/**
	 * @return Operating system short-hand name.
	 */
	public static OperatingSystem get() {
		String s = System.getProperty("os.name").toLowerCase();
		if (s.contains("win")) {
			return WINDOWS;
		}
		if (s.contains("mac") || s.contains("osx")) {
			return MAC;
		}
		return LINUX;
	}
}