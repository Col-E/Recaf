package me.coley.recaf.search.result;

import me.coley.recaf.code.FileInfo;

import java.util.Objects;

/**
 * Location implementation inside a {@link me.coley.recaf.code.FileInfo}.
 *
 * @author Matt Coley
 */
public class FileLocation implements Location {
	private final FileInfo containingFile;
	private final int line;

	/**
	 * @param builder Builder containing information about the parent result.
	 */
	public FileLocation(ResultBuilder builder) {
		containingFile = builder.getContainingFile();
		line = builder.getFileLine();
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

	public int getLine() {
		return line;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		FileLocation that = (FileLocation) o;

		if (line != that.line) return false;
		return Objects.equals(containingFile, that.containingFile);
	}

	@Override
	public int hashCode() {
		int result = containingFile != null ? containingFile.getName().hashCode() : 0;
		result = 31 * result + line;
		return result;
	}

	@Override
	public String toString() {
		return "FileLocation{containingFile=" + containingFile.getName() + ", line=" + line + '}';
	}
}
