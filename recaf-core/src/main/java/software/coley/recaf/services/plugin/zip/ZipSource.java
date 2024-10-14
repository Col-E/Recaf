package software.coley.recaf.services.plugin.zip;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.recaf.services.plugin.PluginSource;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.LocalFileHeaderSource;

import java.io.IOException;

/**
 * ZIP backed plugin source.
 *
 * @author xDark
 */
final class ZipSource implements PluginSource, AutoCloseable {
	private final ZipArchiveView archiveView;

	ZipSource(@Nonnull ZipArchiveView archiveView) {
		this.archiveView = archiveView;
	}

	@Override
	public ByteSource findResource(String name) {
		ZipArchiveView archiveView = this.archiveView;
		if (archiveView.isClosed())
			return null;
		synchronized (this) {
			if (archiveView.isClosed()) return null;
			LocalFileHeader file = archiveView.getEntries().get(name);
			if (file == null) return null;
			return new LocalFileHeaderSource(file);
		}
	}

	@Override
	public void close() throws IOException {
		archiveView.close();
	}
}
