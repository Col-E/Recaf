package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.*;

/**
 * Builder for {@link ApkFileInfo}.
 *
 * @author Matt Coley
 */
public class ApkFileInfoBuilder extends ZipFileInfoBuilder {
	public ApkFileInfoBuilder() {
		// empty
	}

	public ApkFileInfoBuilder(@Nonnull ApkFileInfo apkInfo) {
		super(apkInfo);
	}

	public ApkFileInfoBuilder(@Nonnull ZipFileInfoBuilder other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicApkFileInfo build() {
		return new BasicApkFileInfo(this);
	}
}
