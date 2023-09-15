package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
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

	public ZipFileInfoBuilder(@Nonnull ZipFileInfo zipInfo) {
		super(zipInfo);
	}

	public ZipFileInfoBuilder(@Nonnull FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	public JarFileInfoBuilder asJar() {
		return new JarFileInfoBuilder(this);
	}

	@Nonnull
	public ApkFileInfoBuilder asApk() {
		return new ApkFileInfoBuilder(this);
	}

	@Nonnull
	public JModFileInfoBuilder asJMod() {
		return new JModFileInfoBuilder(this);
	}

	@Nonnull
	public WarFileInfoBuilder asWar() {
		return new WarFileInfoBuilder(this);
	}

	@Nonnull
	@Override
	public BasicZipFileInfo build() {
		return new BasicZipFileInfo(this);
	}
}
