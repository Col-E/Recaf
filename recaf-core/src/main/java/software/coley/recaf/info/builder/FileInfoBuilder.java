package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.*;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.info.properties.PropertyContainer;
import software.coley.recaf.util.StringDecodingResult;
import software.coley.recaf.util.StringUtil;

/**
 * Common builder info for {@link FileInfo}.
 *
 * @param <B>
 * 		Self type. Exists so implementations don't get stunted in their chaining.
 *
 * @author Matt Coley
 */
public class FileInfoBuilder<B extends FileInfoBuilder<?>> {
	private PropertyContainer properties = new BasicPropertyContainer();
	private String name;
	private byte[] rawContent;
	protected StringDecodingResult decodingResult;

	public FileInfoBuilder() {
		// default
	}

	protected FileInfoBuilder(@Nonnull FileInfo fileInfo) {
		// copy state
		withName(fileInfo.getName());
		withRawContent(fileInfo.getRawContent());
		withProperties(new BasicPropertyContainer(fileInfo.getProperties()));
	}

	protected FileInfoBuilder(@Nonnull FileInfoBuilder<?> other) {
		withName(other.getName());
		withRawContent(other.getRawContent());
		withProperties(other.getProperties());
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static <B extends FileInfoBuilder<?>> B forFile(FileInfo info) {
		FileInfoBuilder<?> builder;
		if (info.isZipFile()) {
			// Handle different container types
			if (info instanceof JarFileInfo jarInfo) {
				builder = new JarFileInfoBuilder(jarInfo);
			} else if (info instanceof JModFileInfo modInfo) {
				builder = new JModFileInfoBuilder(modInfo);
			} else if (info instanceof WarFileInfo warInfo) {
				builder = new WarFileInfoBuilder(warInfo);
			} else {
				builder = new ZipFileInfoBuilder(info.asZipFile());
			}
		} else if (info instanceof DexFileInfo dexInfo) {
			builder = new DexFileInfoBuilder(dexInfo);
		} else if (info instanceof ModulesFileInfo modInfo) {
			builder = new ModulesFileInfoBuilder(modInfo);
		} else if (info instanceof TextFileInfo textInfo) {
			builder = new TextFileInfoBuilder(textInfo);
		} else if (info instanceof BinaryXmlFileInfo xmlInfo) {
			builder = new BinaryXmlFileInfoBuilder(xmlInfo);
		} else if (info instanceof ArscFileInfo arscInfo) {
			builder = new ArscFileInfoBuilder(arscInfo);
		} else {
			builder = new FileInfoBuilder<>(info);
		}
		return (B) builder;
	}

	@SuppressWarnings("unchecked")
	public B withProperties(@Nonnull PropertyContainer properties) {
		this.properties = properties;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B withProperty(@Nonnull Property<?> property) {
		properties.setProperty(property);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B withName(@Nonnull String name) {
		this.name = name;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B withRawContent(@Nonnull byte[] rawContent) {
		this.rawContent = rawContent;
		decodingResult = null; // Clear decoding when content changes
		return (B) this;
	}

	public PropertyContainer getProperties() {
		return properties;
	}

	public String getName() {
		return name;
	}

	public byte[] getRawContent() {
		return rawContent;
	}

	/**
	 * @return Computed string decoding result.
	 */
	@Nonnull
	protected StringDecodingResult getDecodingResult() {
		if (decodingResult == null)
			decodingResult = StringUtil.decodeString(rawContent);
		return decodingResult;
	}

	@Nonnull
	public BasicFileInfo build() {
		if (name == null) throw new IllegalArgumentException("Name is required");
		if (rawContent == null) throw new IllegalArgumentException("Content is required");
		if (getDecodingResult().couldDecode())
			return new TextFileInfoBuilder(this, getDecodingResult()).build();
		else
			return new BasicFileInfo(this);
	}
}
