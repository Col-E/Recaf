package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.*;

/**
 * Builder for {@link JModFileInfo}.
 *
 * @author Matt Coley
 */
public class JModFileInfoBuilder extends ZipFileInfoBuilder {
	public JModFileInfoBuilder() {
		// empty
	}

	public JModFileInfoBuilder(@Nonnull JModFileInfo jmodInfo) {
		super(jmodInfo);
	}

	public JModFileInfoBuilder(@Nonnull FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicJModFileInfo build() {
		return new BasicJModFileInfo(this);
	}
}
