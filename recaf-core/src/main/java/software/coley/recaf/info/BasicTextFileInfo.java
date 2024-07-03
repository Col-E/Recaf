package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.FileInfoBuilder;
import software.coley.recaf.info.builder.TextFileInfoBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Basic implementation of text file info.
 *
 * @author Matt Coley
 */
public class BasicTextFileInfo extends BasicFileInfo implements TextFileInfo {
	private final Charset charset;
	private final String text;
	private String[] lines;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicTextFileInfo(@Nonnull TextFileInfoBuilder builder) {
		super(builder);
		text = builder.getText();
		charset = builder.getCharset();
	}

	@Nonnull
	@Override
	public String getText() {
		return text;
	}

	@Nonnull
	@Override
	public String[] getTextLines() {
		if (lines == null)
			lines = getText().lines().toArray(String[]::new);
		return lines;
	}

	@Nonnull
	@Override
	public Charset getCharset() {
		return charset;
	}
}
