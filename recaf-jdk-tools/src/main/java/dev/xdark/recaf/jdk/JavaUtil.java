package dev.xdark.recaf.jdk;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Java detection utils.
 *
 * @author xDark
 */
public final class JavaUtil {

	/**
	 * @param jdkHome
	 * 		JDK installation home.
	 *
	 * @return path to 'java' executable.
	 */
	public static Path getJavaExecutable(Path jdkHome) {
		jdkHome = jdkHome.resolve("bin");
		String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (osName.contains("win")) {
			return jdkHome.resolve("java.exe");
		} else {
			return jdkHome.resolve("java");
		}
	}

	/**
	 * @return path to 'java' executable
	 * for this process.
	 */
	public static Path getJavaExecutable() {
		return getJavaExecutable(Paths.get(System.getProperty("java.home")));
	}
}
