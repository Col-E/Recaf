package me.coley.recaf.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.CodeSource;

import me.coley.recaf.Recaf;

/**
 * File IO utilities.
 * 
 * @author Matt
 */
public class Files {
	/**
	 * Reads all text from the file at the given path.
	 * 
	 * @param path
	 *            Path to text file.
	 * @return Text contents of file.
	 * @throws IOException
	 *             Thrown if a stream to the file could not be opened or read
	 *             from.
	 */
	public static String readFile(String path) throws IOException {
		return new String(java.nio.file.Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
	}

	/**
	 * Writes the contents to the given path. File is written to in utf-8
	 * encoding.
	 * 
	 * @param path
	 *            Path to file to write to.
	 * @param content
	 *            Text contents to write.
	 * @throws IOException
	 *             Thrown if the file could not be written.
	 */
	public static void writeFile(String path, String content) throws IOException {
		java.nio.file.Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Writes the contents to the given path.
	 * 
	 * @param path
	 *            Path to file to write to.
	 * @param content
	 *            Raw content to write.
	 * @throws IOException
	 *             Thrown if the file could not be written.
	 */
	public static void writeFile(String path, byte[] content) throws IOException {
		java.nio.file.Files.write(Paths.get(path), content);
	}

	/**
	 * @return Recaf executable context.
	 * @throws URISyntaxException
	 *             Thrown if the file reference could not be resolved.
	 */
	public static SelfReference getSelf() throws URISyntaxException {
		CodeSource codeSource = Recaf.class.getProtectionDomain().getCodeSource();
		File selfFile = new File(codeSource.getLocation().toURI().getPath());
		return new SelfReference(selfFile);
	}

	/**
	 * Wrapper for executable context of Recaf.
	 * 
	 * @author Matt
	 */
	public static class SelfReference {
		private final File file;
		private final boolean isJar;

		public SelfReference(File file) {
			this.file = file;
			this.isJar = file.getName().toLowerCase().endsWith(".jar");
		}

		/**
		 * @return File reference to self.
		 */
		public File getFile() {
			return file;
		}

		/**
		 * @return File path to self.
		 */
		public String getPath() {
			return file.getAbsolutePath();
		}

		/**
		 * @return Is the current executable context a jar file.
		 */
		public boolean isJar() {
			return isJar;
		}

	}

}