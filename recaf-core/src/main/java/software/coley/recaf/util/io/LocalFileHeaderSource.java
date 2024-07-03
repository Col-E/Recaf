package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.MemorySegmentUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Byte source from {@link LocalFileHeader}.
 *
 * @author xDark
 */
public final class LocalFileHeaderSource implements ByteSource {
	private final LocalFileHeader fileHeader;
	private final boolean isAndroid;
	private MemorySegment decompressed;

	public LocalFileHeaderSource(LocalFileHeader fileHeader) {
		this(fileHeader, false);
	}

	public LocalFileHeaderSource(LocalFileHeader fileHeader, boolean isAndroid) {
		this.fileHeader = fileHeader;
		this.isAndroid = isAndroid;
	}

	@Nonnull
	@Override
	public byte[] readAll() throws IOException {
		return MemorySegmentUtil.toByteArray(decompress());
	}

	@Nonnull
	@Override
	public byte[] peek(int count) throws IOException {
		MemorySegment data = decompress();
		long length = data.byteSize();
		if (length < count)
			count = (int) length;
		return data.asSlice(0, count).toArray(ValueLayout.JAVA_BYTE);
	}

	@Nonnull
	@Override
	public InputStream openStream() throws IOException {
		// Delegate to byte source
		return ByteSources.forMemorySegment(decompress()).openStream();
	}

	@Nonnull
	@Override
	public MemorySegment mmap() throws IOException {
		return decompress();
	}

	/**
	 * @return {@code true} when the data length of the content is 0.
	 *
	 * @throws IOException
	 * 		When data cannot be decompressed to determine true content length.
	 */
	public boolean isEmpty() throws IOException {
		if (fileHeader.getCompressionMethod() == ZipCompressions.STORED)
			return fileHeader.getFileData().byteSize() == 0;
		return decompress().byteSize() == 0;
	}

	private MemorySegment decompress() throws IOException {
		MemorySegment decompressed = this.decompressed;
		if (decompressed == null) {
			// From: https://cs.android.com/android/_/android/platform/frameworks/base/+/b3559643b946829933a76ed45750d13edfefad30:tools/aapt/ZipFile.cpp;l=436
			//  - If the compression mode given fails, it will get treated as STORED as a fallback
			if (isAndroid) {
				try {
					return this.decompressed = ZipCompressions.decompress(fileHeader);
				} catch (IOException ex) {
					return this.decompressed = fileHeader.getFileData();
				}
			}

			// In other cases, malformed content should throw an exception and be handled by the caller.
			return this.decompressed = ZipCompressions.decompress(fileHeader);
		}
		return decompressed;
	}
}