package me.coley.recaf.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

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

}