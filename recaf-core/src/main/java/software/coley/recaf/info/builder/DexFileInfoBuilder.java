package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.BasicDexFileInfo;
import software.coley.recaf.info.DexFileInfo;

/**
 * Builder for {@link DexFileInfo}.
 *
 * @author Matt Coley
 */
public class DexFileInfoBuilder extends FileInfoBuilder<DexFileInfoBuilder> {
	public DexFileInfoBuilder() {
		// empty
	}

	public DexFileInfoBuilder(DexFileInfo dexFileInfo) {
		super(dexFileInfo);
	}

	public DexFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicDexFileInfo build() {
		return new BasicDexFileInfo(this);
	}
}
