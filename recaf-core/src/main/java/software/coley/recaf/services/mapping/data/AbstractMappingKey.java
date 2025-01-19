package software.coley.recaf.services.mapping.data;

import jakarta.annotation.Nonnull;

/**
 * Base mapping key.
 *
 * @author xDark
 */
public abstract class AbstractMappingKey implements MappingKey {
	private String text;

	@Nonnull
	@Override
	public String getAsText() {
		String text = this.text;
		if (text == null) {
			return this.text = toText();
		}
		return text;
	}

	@Override
	public int compareTo(MappingKey o) {
		return getAsText().compareTo(o.getAsText());
	}

	@Override
	public String toString() {
		return getAsText();
	}

	@Nonnull
	protected abstract String toText();
}
