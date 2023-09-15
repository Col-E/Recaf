package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.*;

/**
 * Builder for {@link WarFileInfo}.
 *
 * @author Matt Coley
 */
public class WarFileInfoBuilder extends ZipFileInfoBuilder {
	public WarFileInfoBuilder() {
		// empty
	}

	public WarFileInfoBuilder(@Nonnull WarFileInfo warInfo) {
		super(warInfo);
	}

	public WarFileInfoBuilder(@Nonnull FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicWarFileInfo build() {
		return new BasicWarFileInfo(this);
	}
}
