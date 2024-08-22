package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.util.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Stub implementation of {@link FileInfo}.
 *
 * @author Matt Coley
 */
public class StubFileInfo implements FileInfo {
	private final String name;

	/**
	 * @param name
	 * 		File name.
	 */
	public StubFileInfo(@Nonnull String name) {
		this.name = name;
	}

	/**
	 * @param text
	 * 		Text to assign.
	 *
	 * @return This file, with text content.
	 */
	@Nonnull
	public TextFileInfo withText(@Nonnull String text) {
		return withText(StandardCharsets.ISO_8859_1, text);
	}

	/**
	 * @param charset
	 * 		Charset of text.
	 * @param text
	 * 		Text to assign.
	 *
	 * @return This file, with text content.
	 */
	@Nonnull
	public TextFileInfo withText(@Nonnull Charset charset, @Nonnull String text) {
		return new StubTextFileInfo(name, charset, text);
	}

	@Nonnull
	@Override
	public byte[] getRawContent() {
		return new byte[0];
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Override
	public <V> void setProperty(Property<V> property) {
		// no-op
	}

	@Override
	public void removeProperty(String key) {
		// no-op
	}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		return Collections.emptyMap();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof FileInfo other) {
			return name.equals(other.getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	private static class StubTextFileInfo extends StubFileInfo implements TextFileInfo {
		private final String text;
		private final Charset charset;

		/**
		 * @param name
		 * 		File name.
		 * @param charset
		 * 		Charset of text.
		 * @param text
		 * 		Text content.
		 */
		public StubTextFileInfo(@Nonnull String name, @Nonnull Charset charset, @Nonnull String text) {
			super(name);

			this.text = text;
			this.charset = charset;
		}

		@Nonnull
		@Override
		public String getText() {
			return text;
		}

		@Nonnull
		@Override
		public String[] getTextLines() {
			return StringUtil.splitNewline(text);
		}

		@Nonnull
		@Override
		public Charset getCharset() {
			return charset;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null) return false;
			if (!super.equals(o)) return false;
			if (o instanceof TextFileInfo other) {
				return (text.equals(other.getText()));
			}
			return false;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + text.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return getName() + " : <" +  text + ">";
		}
	}
}
