package software.coley.recaf.services.plugin.zip;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.recaf.services.plugin.PluginSource;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.LocalFileHeaderSource;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

	@Nonnull
	@Override
	public Collection<String> classNames() {
		ZipArchiveView archiveView = this.archiveView;
		if (archiveView.isClosed())
			return List.of();
		synchronized (this) {
			if (archiveView.isClosed()) return List.of();
			return archiveView.getEntries().keySet().stream()
					.filter(name -> name.endsWith(".class"))
					.filter(name -> !name.startsWith("META-INF/versions/"))
					.filter(name -> !name.endsWith("module-info.class"))
					.filter(name -> !name.endsWith("package-info.class"))
					.map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
					.sorted(Comparator.naturalOrder())
					.toList();
		}
	}

	@Override
	public void close() throws IOException {
		archiveView.close();
	}
}
