package software.coley.recaf.info.builder;

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

	public WarFileInfoBuilder(WarFileInfo warInfo) {
		super(warInfo);
	}

	public WarFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Override
	public BasicWarFileInfo build() {
		return new BasicWarFileInfo(this);
	}
}
