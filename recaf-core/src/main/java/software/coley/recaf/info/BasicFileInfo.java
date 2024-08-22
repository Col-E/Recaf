package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.FileInfoBuilder;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.info.properties.PropertyContainer;

import java.util.Arrays;
import java.util.Map;

/**
 * Basic implementation of file info.
 *
 * @author Matt Coley
 */
public class BasicFileInfo implements FileInfo {
	private final PropertyContainer properties;
	private final String name;
	private final byte[] rawContent;

	public BasicFileInfo(@Nonnull FileInfoBuilder<?> builder) {
		this(builder.getName(),
				builder.getRawContent(),
				builder.getProperties());
	}

	/**
	 * @param name
	 * 		File name/path.
	 * @param rawContent
	 * 		Raw contents of file.
	 * @param properties
	 * 		Assorted properties.
	 */
	public BasicFileInfo(@Nonnull String name, @Nonnull byte[] rawContent, @Nonnull PropertyContainer properties) {
		this.name = name;
		this.rawContent = rawContent;
		this.properties = properties;
	}

	@Nonnull
	@Override
	public byte[] getRawContent() {
		return rawContent;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof FileInfo other) {
			if (!name.equals(other.getName())) return false;
			return Arrays.equals(rawContent, other.getRawContent());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Arrays.hashCode(rawContent);
		return result;
	}

	@Override
	public <V> void setProperty(Property<V> property) {
		properties.setProperty(property);
	}

	@Override
	public void removeProperty(String key) {
		properties.removeProperty(key);
	}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		return properties.getProperties();
	}

	@Override
	public String toString() {
		return name;
	}
}
