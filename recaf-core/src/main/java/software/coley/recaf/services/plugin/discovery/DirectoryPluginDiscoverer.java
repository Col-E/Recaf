package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.PluginException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class DirectoryPluginDiscoverer extends PathPluginDiscoverer {
	private final Path directory;

	public DirectoryPluginDiscoverer(Path directory) {
		this.directory = directory;
	}

	@Nonnull
	@Override
	protected Stream<Path> stream() throws PluginException {
		try {
			return Files.find(directory, 1, (path, attributes) -> attributes.isRegularFile());
		} catch (IOException ex) {
			throw new PluginException(ex);
		}
	}
}
