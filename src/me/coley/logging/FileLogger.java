package me.coley.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileLogger extends Logger<FileOutputStream> {
	/**
	 * Construct a FileLogger to the given file.
	 * 
	 * @param file
	 *            Name of file to write to.
	 * @param level
	 *            Level of detail to log.
	 * @throws IOException
	 *             Thrown if the file could not be created or written to.
	 */
	public FileLogger(String file, Level level) throws IOException {
		this(new File(file), level);
	}

	/**
	 * Construct a FileLogger to the given file.
	 * 
	 * @param file
	 *            File to write to.
	 * @param level
	 *            Level of detail to log.
	 * @throws IOException
	 *             Thrown if the file could not be created or written to.
	 */
	public FileLogger(File file, Level level) throws IOException {
		super(createOutStream(file), level);
	}

	/**
	 * Construct a FileOutputStream and ensure the given file exists.
	 * 
	 * @param file
	 *            File to write to.
	 * @return Out-Stream to file.
	 * @throws IOException
	 *             Thrown if the file could not be created or written to.
	 */
	private static FileOutputStream createOutStream(File file) throws IOException {
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		if (!file.exists()) {
			file.createNewFile();
		}
		return new FileOutputStream(file, false);
	}

}
