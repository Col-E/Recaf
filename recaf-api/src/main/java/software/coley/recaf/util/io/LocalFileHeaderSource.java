package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Byte source from {@link LocalFileHeader}.
 *
 * @author xDark
 */
public final class LocalFileHeaderSource implements ByteSource {
	private final LocalFileHeader fileHeader;
	private ByteData decompressed;

	public LocalFileHeaderSource(LocalFileHeader fileHeader) {
		this.fileHeader = fileHeader;
	}

	@Nonnull
	@Override
	public byte[] readAll() throws IOException {
		return ByteDataUtil.toByteArray(decompress());
	}

	@Nonnull
	@Override
	public byte[] peek(int count) throws IOException {
		ByteData data = decompress();
		long length = data.length();
		if (length < count)
			count = (int) length;
		byte[] bytes = new byte[count];
		data.get(0L, bytes, 0, count);
		return bytes;
	}

	@Nonnull
	@Override
	public InputStream openStream() throws IOException {
		// Delegate to byte source
		return ByteSources.forZip(decompress()).openStream();
	}

	/**
	 * @return {@code true} when the data length of the content is 0.
	 *
	 * @throws IOException
	 * 		When data cannot be decompressed to determine true content length.
	 */
	public boolean isEmpty() throws IOException {
		if (fileHeader.getCompressionMethod() == ZipCompressions.STORED)
			return fileHeader.getFileData().length() == 0;
		return decompress().length() == 0;
	}

	private ByteData decompress() throws IOException {
		ByteData decompressed = this.decompressed;
		if (decompressed == null) {
			return this.decompressed = ZipCompressions.decompress(fileHeader);
		}
		return decompressed;
	}
}