package software.coley.recaf.services.plugin.zip;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.recaf.plugin.PluginSource;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.LocalFileHeaderSource;

import java.io.IOException;

final class ZipSource implements PluginSource, AutoCloseable {
	private final ZipArchiveView archiveView;

	ZipSource(ZipArchiveView archiveView) {
		this.archiveView = archiveView;
	}

	@Override
	public ByteSource findResource(String name) {
		ZipArchiveView archiveView = this.archiveView;
		if (archiveView.closed)
			return null;
		synchronized (this) {
			if (archiveView.closed) return null;
			LocalFileHeader file = archiveView.names.get(name);
			if (file == null) return null;
			return new LocalFileHeaderSource(file);
		}
	}

	@Override
	public void close() throws IOException {
		archiveView.close();
	}
}
