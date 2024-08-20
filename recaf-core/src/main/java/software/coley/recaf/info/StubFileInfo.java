package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.properties.Property;

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
	 * @param name File name.
	 */
	public StubFileInfo(@Nonnull String name) {
		this.name = name;
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
		if (o == null || getClass() != o.getClass()) return false;

		StubFileInfo that = (StubFileInfo) o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
