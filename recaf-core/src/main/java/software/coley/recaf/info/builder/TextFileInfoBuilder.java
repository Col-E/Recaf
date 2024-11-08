package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.BasicTextFileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.util.StringDecodingResult;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Builder for {@link TextFileInfo}.
 *
 * @author Matt Coley
 */
public class TextFileInfoBuilder extends FileInfoBuilder<TextFileInfoBuilder> {
	public TextFileInfoBuilder() {
		// empty
	}

	public TextFileInfoBuilder(@Nonnull TextFileInfo textInfo) {
		super(textInfo);
		this.decodingResult = new StringDecodingResult(textInfo.getRawContent(), textInfo.getCharset(), textInfo.getText());
	}

	public TextFileInfoBuilder(@Nonnull FileInfoBuilder<?> other, @Nonnull StringDecodingResult decodingResult) {
		super(other);
		this.decodingResult = decodingResult;
	}

	@Nonnull
	public TextFileInfoBuilder withText(@Nonnull String text) {
		return withRawContent(text.getBytes(getCharset()));
	}

	@Nonnull
	public String getText() {
		return Objects.requireNonNull(getDecodingResult().text(), "File '" + getName() + "' could not be decoded");
	}

	@Nonnull
	public Charset getCharset() {
		return Objects.requireNonNull(getDecodingResult().charset(), "File '" + getName() + "' could not be decoded");
	}

	@Nonnull
	@Override
	public BasicTextFileInfo build() {
		return new BasicTextFileInfo(this);
	}
}
