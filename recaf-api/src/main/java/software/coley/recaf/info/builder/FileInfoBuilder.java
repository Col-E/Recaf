package software.coley.recaf.info.builder;

import software.coley.recaf.info.*;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.info.properties.PropertyContainer;
import software.coley.recaf.util.StringUtil;

/**
 * Common builder info for {@link FileInfo}.
 *
 * @param <B>
 * 		Self type. Exists so implementations don't get stunted in their chaining.
 *
 * @author Matt Coley
 * @see ZipFileInfoBuilder
 * @see DexFileInfoBuilder
 * @see ModulesFileInfoBuilder
 */
public class FileInfoBuilder<B extends FileInfoBuilder<?>> {
	private PropertyContainer properties = new BasicPropertyContainer();
	private String name;
	private byte[] rawContent;

	public FileInfoBuilder() {
		// default
	}

	protected FileInfoBuilder(FileInfo fileInfo) {
		// copy state
		withName(fileInfo.getName());
		withRawContent(fileInfo.getRawContent());
		withProperties(new BasicPropertyContainer(fileInfo.getProperties()));
	}

	protected FileInfoBuilder(FileInfoBuilder<?> other) {
		withName(other.getName());
		withRawContent(other.getRawContent());
		withProperties(other.getProperties());
	}

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
	public B withProperties(PropertyContainer properties) {
		this.properties = properties;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B withName(String name) {
		this.name = name;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B withRawContent(byte[] rawContent) {
		this.rawContent = rawContent;
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

	public BasicFileInfo build() {
		if (name == null) throw new IllegalArgumentException("Name is required");
		if (rawContent == null) throw new IllegalArgumentException("Content is required");
		if (StringUtil.isText(rawContent))
			return new BasicTextFileInfo(this);
		else
			return new BasicFileInfo(this);
	}
}
