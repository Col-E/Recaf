package software.coley.recaf.info.builder;

import software.coley.recaf.info.BasicTextFileInfo;
import software.coley.recaf.info.BasicZipFileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.info.ZipFileInfo;

/**
 * Builder for {@link TextFileInfo}.
 *
 * @author Matt Coley
 */
public class TextFileInfoBuilder extends FileInfoBuilder<TextFileInfoBuilder> {
	public TextFileInfoBuilder() {
		// empty
	}

	public TextFileInfoBuilder(TextFileInfo textInfo) {
		super(textInfo);
	}

	public TextFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Override
	public BasicTextFileInfo build() {
		return new BasicTextFileInfo(this);
	}
}
