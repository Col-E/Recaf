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
	@Nonnull
	String getText();

	/**
	 * @return The {@link #getText() text content} split into lines.
	 */
	@Nonnull
	String[] getTextLines();

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
