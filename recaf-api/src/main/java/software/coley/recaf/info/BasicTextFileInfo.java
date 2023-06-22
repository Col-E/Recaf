package software.coley.recaf.info;

import software.coley.recaf.info.builder.FileInfoBuilder;

import java.nio.charset.StandardCharsets;

/**
 * Basic implementation of text file info.
 *
 * @author Matt Coley
 */
public class BasicTextFileInfo extends BasicFileInfo implements TextFileInfo {
	private String text;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicTextFileInfo(FileInfoBuilder<?> builder) {
		super(builder);
	}

	@Override
	public String getText() {
		if (text == null)
			text = new String(getRawContent(), StandardCharsets.UTF_8);
		return text;
	}
}
