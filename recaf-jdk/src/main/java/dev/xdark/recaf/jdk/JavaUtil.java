package dev.xdark.recaf.jdk;

import java.nio.file.Path;
import java.nio.file.Paths;

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
		return jdkHome.resolve("bin").resolve("java.exe");
	}

	/**
	 * @return path to 'java' executable
	 * for this process.
	 */
	public static Path getJavaExecutable() {
		return getJavaExecutable(Paths.get(System.getProperty("java.home")));
	}
}
