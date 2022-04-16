package me.coley.recaf.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Pth byte source.
 *
 * @author xDark
 */
final class PathByteSource implements ByteSource {
	private final Path path;

	/**
	 * @param path
	 * 		Path to read bytes from.
	 */
	PathByteSource(Path path) {
		this.path = path;
	}

	@Override
	public byte[] readAll() throws IOException {
		return Files.readAllBytes(path);
	}

	@Override
	public byte[] peek(int count) throws IOException {
		try (InputStream in = Files.newInputStream(path)) {
			byte[] buf = new byte[count];
			int offset = 0;
			int r;
			while ((r = in.read(buf, offset, count)) != -1) {
				offset += r;
				count -= r;
			}
			return count == 0 ? buf : Arrays.copyOf(buf, offset);
		}
	}
}
