package dev.xdark.recaf.util;

import dev.dirs.BaseDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * File path and directory utilities.
 *
 * @author xDark
 */
public class PathUtils {
	private static final Logger logger = LoggerFactory.getLogger("Launcher");
	private static Path recafDirectory;

	/**
	 * Cuts off leading directories, leaving only the file name.
	 * For example {@code foo/bar.txt} becomes {@code bar.txt}.
	 *
	 * @param fileName
	 * 		Some file path that may contain separators.
	 *
	 * @return Just the file name.
	 */
	public static String getCompactFileName(String fileName) {
		int index = indexOfLastSeparator(fileName);
		return fileName.substring(index + 1);
	}

	/**
	 * @return Installation directory for JRE/JDK.
	 */
	public static Path getJavaHome() {
		return Paths.get(System.getProperty("java.home"));
	}

	/**
	 * @return Path to {@code java} executable associated with the current JVM.
	 */
	public static Path getJavaExecutable() {
		Path javaHome = getJavaHome();
		Path bin = javaHome.resolve("bin");
		if (Platform.isOnWindows()) {
			return bin.resolve("java.exe");
		}
		return bin.resolve("java");
	}

	/**
	 * @return Recaf directory.
	 */
	public static Path getRecafDirectory() {
		Path directory = recafDirectory;
		if (directory == null) {
			try {
				directory = Paths.get(BaseDirectories.get().configDir).resolve("Recaf");
			} catch (Throwable t) {
				// Only fail if we're on a system where we can't easily provide a fallback option
				if (!Platform.isOnWindows()) {
					throw new IllegalStateException("Failed to initialize Recaf directory", t);
				}
				logger.error("Could not get base directory: ", t);
				logger.error("Using %APPDATA%/Recaf as base directory");
			}
			if (directory == null) {
				// On Windows, BaseDirectories invokes a PowerShell script to get directories.
				// If it fails, it may return null.
				if (Platform.isOnWindows()) {
					directory = Paths.get(System.getenv("APPDATA"), "Recaf");
				} else {
					// Have not encountered failures on other systems, should not occur as far as I'm aware
					throw new IllegalStateException("Unable to get base directory");
				}
			}
			recafDirectory = directory;
		}
		return directory;
	}

	/**
	 * @param filePath
	 * 		Some file path that may contain separators.
	 *
	 * @return Last index of a path separator character, or {@code -1} if not present.
	 */
	public static int indexOfLastSeparator(String filePath) {
		int lastUnixPos = filePath.lastIndexOf('/');
		int lastWindowsPos = filePath.lastIndexOf('\\');
		return Math.max(lastUnixPos, lastWindowsPos);
	}

	/**
	 * @param path
	 * 		Path to some file.
	 *
	 * @return {@code true} if the file at the path is writable.
	 */
	public static boolean isWritable(Path path) {
		if (Files.isDirectory(path)) {
			return false;
		}
		try (OutputStream ignored = Files.newOutputStream(path, StandardOpenOption.APPEND)) {
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}
}
