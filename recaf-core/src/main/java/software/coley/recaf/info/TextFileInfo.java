package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.FileInfoBuilder;
import software.coley.recaf.info.builder.TextFileInfoBuilder;

import java.nio.charset.Charset;

/**
 * Outline of a text file.
 *
 * @author Matt Coley
 */
public interface TextFileInfo extends FileInfo {
	/**
	 * @return New builder wrapping this file information.
	 */
	@Nonnull
	default TextFileInfoBuilder toTextBuilder() {
		return new TextFileInfoBuilder(this);
	}

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

	/**
	 * @return The charset used to encode {@link #getText() the text content}.
	 */
	@Nonnull
	Charset getCharset();

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
