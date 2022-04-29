package me.coley.recaf.search.result;

import me.coley.recaf.code.FileInfo;

/**
 * Location implementation inside a {@link me.coley.recaf.code.FileInfo}.
 *
 * @author Matt Coley
 */
public class FileLocation implements Location {
	private final FileInfo containingFile;

	/**
	 * @param builder
	 * 		Builder containing information about the parent result.
	 */
	public FileLocation(ResultBuilder builder) {
		containingFile = builder.getContainingFile();
	}

	/**
	 * @return The file the result was found in.
	 */
	public FileInfo getContainingFile() {
		return containingFile;
	}

	@Override
	public String comparableString() {
		return containingFile.getName();
	}
}
