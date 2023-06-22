package software.coley.recaf.info.builder;

import software.coley.recaf.info.BasicZipFileInfo;
import software.coley.recaf.info.ZipFileInfo;

/**
 * Builder for {@link ZipFileInfo}.
 *
 * @author Matt Coley
 * @see JarFileInfoBuilder
 * @see JModFileInfoBuilder
 * @see WarFileInfoBuilder
 * @see ApkFileInfoBuilder
 */
public class ZipFileInfoBuilder extends FileInfoBuilder<ZipFileInfoBuilder> {
	public ZipFileInfoBuilder() {
		// empty
	}

	public ZipFileInfoBuilder(ZipFileInfo zipInfo) {
		super(zipInfo);
	}

	public ZipFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	public JarFileInfoBuilder asJar() {
		return new JarFileInfoBuilder(this);
	}

	public ApkFileInfoBuilder asApk() {
		return new ApkFileInfoBuilder(this);
	}

	public JModFileInfoBuilder asJMod() {
		return new JModFileInfoBuilder(this);
	}

	public WarFileInfoBuilder asWar() {
		return new WarFileInfoBuilder(this);
	}

	@Override
	public BasicZipFileInfo build() {
		return new BasicZipFileInfo(this);
	}
}
