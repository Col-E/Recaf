package software.coley.recaf.info.builder;

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

	public ApkFileInfoBuilder(ApkFileInfo apkInfo) {
		super(apkInfo);
	}

	public ApkFileInfoBuilder(ZipFileInfoBuilder other) {
		super(other);
	}

	@Override
	public BasicApkFileInfo build() {
		return new BasicApkFileInfo(this);
	}
}
