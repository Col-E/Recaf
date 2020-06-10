package me.coley.recaf.util;

/**
 * Operating system utility.
 */
public enum OSUtil {
	WINDOWS("win"),
	MAC("mac"),
	LINUX("linux");

	private final String mvnName;

	OSUtil(String mvnName) {
		this.mvnName = mvnName;
	}

	/**
	 * @return Maven artifact name suffix.
	 */
	public String getMvnName() {
		return mvnName;
	}

	/**
	 * @return Operating system short-hand name.
	 */
	public static OSUtil getOSType() {
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
