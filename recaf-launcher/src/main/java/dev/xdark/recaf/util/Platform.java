package dev.xdark.recaf.util;

/**
 * Utility to determine user's operating system.
 *
 * @author xDark
 */
public final class Platform {
	static final int WIN = 0;
	static final int LINUX = 1;
	static final int MAC = 2;
	static final int PLATFORM;

	private Platform() {
	}

	static boolean isOnWindows() {
		return PLATFORM == WIN;
	}

	static {
		String os = System.getProperty("os.name").toLowerCase();
		int platform;
		if (os.contains("win")) {
			platform = WIN;
		} else if (os.contains("mac") || os.contains("osx")) {
			platform = MAC;
		} else {
			platform = LINUX;
		}
		PLATFORM = platform;
	}
}
