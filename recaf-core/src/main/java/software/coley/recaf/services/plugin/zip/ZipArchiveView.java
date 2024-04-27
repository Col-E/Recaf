package software.coley.recaf.services.plugin.zip;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ZipArchiveView implements AutoCloseable {
	final ZipArchive archive; // keep alive
	final Map<String, LocalFileHeader> names;
	volatile boolean closed;

	ZipArchiveView(ZipArchive archive) {
		this.archive = archive;
		Map<String, LocalFileHeader> names;
		List<CentralDirectoryFileHeader> centralDirectories = archive.getCentralDirectories();
		if (!centralDirectories.isEmpty()) {
			names = HashMap.newHashMap(centralDirectories.size());
			for (CentralDirectoryFileHeader cdf : centralDirectories) {
				String name = cdf.getFileNameAsString();
				names.putIfAbsent(name, cdf.getLinkedFileHeader());
			}
		} else {
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
		try (archive) {
			names.clear();
		}
	}
}
