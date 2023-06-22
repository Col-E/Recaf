package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of a text file.
 *
 * @author Matt Coley
 */
public interface TextFileInfo extends FileInfo {
	/**
	 * @return The {@link #getRawContent()} as text.
	 */
	String getText();

	@Nonnull
	@Override
	default TextFileInfo asTextFile() {
		return this;
	}

	@Override
	default boolean isTextFile() {
		return true;
	}
}
