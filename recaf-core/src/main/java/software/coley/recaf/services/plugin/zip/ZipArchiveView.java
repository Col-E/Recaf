package software.coley.recaf.services.plugin.zip;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes a {@link ZipArchive}'s contents as a {@code Map<String, LocalFileHeader>}.
 *
 * @author xDark
 */
final class ZipArchiveView implements AutoCloseable {
	private final ZipArchive archive; // keep alive
	private final Map<String, LocalFileHeader> names;
	private volatile boolean closed;

	ZipArchiveView(@Nonnull ZipArchive archive) {
		this.archive = archive;
		Map<String, LocalFileHeader> names;

		// Populate view from authoritative central directory entries if possible.
		List<CentralDirectoryFileHeader> centralDirectories = archive.getCentralDirectories();
		if (!centralDirectories.isEmpty()) {
			names = HashMap.newHashMap(centralDirectories.size());
			for (CentralDirectoryFileHeader cdf : centralDirectories) {
				String name = cdf.getFileNameAsString();
				names.putIfAbsent(name, cdf.getLinkedFileHeader());
			}
		} else {
			// Fall back to local file entries id central directory entries do not exist.
			List<LocalFileHeader> localFiles = archive.getLocalFiles();
			names = HashMap.newHashMap(localFiles.size());
			for (LocalFileHeader localFile : localFiles) {
				names.putIfAbsent(localFile.getFileNameAsString(), localFile);
			}
		}
		this.names = names;
	}

	@Override
	public void close() throws IOException {
		if (closed) return;
		synchronized (this) {
			if (closed) return;
			closed = true;
		}

		// Try-with to auto-close the archive when complete.
		try (archive) {
			names.clear();
		}
	}

	/**
	 * @return {@code true} when the backing archive is released.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @return Backing archive.
	 */
	@Nonnull
	public ZipArchive getArchive() {
		return archive;
	}

	/**
	 * @return Archive entries as a map of internal paths.
	 */
	@Nonnull
	public Map<String, LocalFileHeader> getEntries() {
		return names;
	}
}
