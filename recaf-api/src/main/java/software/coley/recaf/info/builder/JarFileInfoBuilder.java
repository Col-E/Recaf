package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.BasicJarFileInfo;
import software.coley.recaf.info.JarFileInfo;

/**
 * Builder for {@link JarFileInfo}.
 *
 * @author Matt Coley
 */
public class JarFileInfoBuilder extends ZipFileInfoBuilder {
	public JarFileInfoBuilder() {
		// empty
	}

	public JarFileInfoBuilder(@Nonnull JarFileInfo jarInfo) {
		super(jarInfo);
	}

	public JarFileInfoBuilder(@Nonnull ZipFileInfoBuilder other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicJarFileInfo build() {
		return new BasicJarFileInfo(this);
	}
}
