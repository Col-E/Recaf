package software.coley.recaf.services.plugin.zip;

import software.coley.lljzip.format.model.LocalFileHeader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

final class LocalFileHeaderStreamHandler extends URLStreamHandler {
	private final ZipArchiveView archiveView; // keep alive
	final LocalFileHeader header;

	LocalFileHeaderStreamHandler(ZipArchiveView archiveView, LocalFileHeader header) {
		this.archiveView = archiveView;
		this.header = header;
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new URLConnection(u) {
			InputStream in;

			@Override
			public void connect() throws IOException {

			}

			@Override
			public InputStream getInputStream() throws IOException {
				InputStream in = this.in;
				if (in == null) {
					if (archiveView.isClosed())
						throw new IOException("Archive is closed");
					this.in = in = blackbox(header); // TODO Blocked on ll-java-zip.
				}
				return in;
			}
		};
	}

	private static native <T> T blackbox(Object value);
}
