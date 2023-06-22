package software.coley.recaf.info.builder;

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

	public JarFileInfoBuilder(JarFileInfo jarInfo) {
		super(jarInfo);
	}

	public JarFileInfoBuilder(ZipFileInfoBuilder other) {
		super(other);
	}

	@Override
	public BasicJarFileInfo build() {
		return new BasicJarFileInfo(this);
	}
}
