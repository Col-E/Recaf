package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
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
	private final ZipDecompressionLimiter limiter;
	private MemorySegment decompressed;

	public LocalFileHeaderSource(LocalFileHeader fileHeader) {
		this(fileHeader, false, null);
	}

	public LocalFileHeaderSource(LocalFileHeader fileHeader, boolean isAndroid) {
		this(fileHeader, isAndroid, null);
	}

	public LocalFileHeaderSource(LocalFileHeader fileHeader, boolean isAndroid, ZipDecompressionLimiter limiter) {
		this.fileHeader = fileHeader;
		this.isAndroid = isAndroid;
		this.limiter = limiter;
	}

	@Nonnull
	@Override
	public MemorySegment readAll() throws IOException {
		return decompress();
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
		try {
			MemorySegment decompressed = this.decompressed;
			if (decompressed == null) {
				// From: https://cs.android.com/android/_/android/platform/frameworks/base/+/b3559643b946829933a76ed45750d13edfefad30:tools/aapt/ZipFile.cpp;l=436
				//  - If the compression mode given fails, it will get treated as STORED as a fallback
				if (isAndroid) {
					try {
						return this.decompressed = decompressEntry();
					} catch (ZipDecompressionLimiter.ZipDecompressionLimitException ex) {
						throw ex;
					} catch (IOException ex) {
						return this.decompressed = limiter == null ?
								fileHeader.getFileData() : limiter.acceptStored(fileHeader);
					}
				}

				// In other cases, malformed content should throw an exception and be handled by the caller.
				return this.decompressed = decompressEntry();
			}
			return decompressed;
		} catch (OutOfMemoryError error) {
			// This is cringe, I know. However, in practice it will immediately free the memory
			// used in the failed decompression. In a profile you'll the heap look like a saw-tooth with this.
			// Without, the heap just grows. It may go down a little, but not to the baseline of just calling gc().
			System.gc();
			throw new IOException("Insufficient memory to decompress '" + fileHeader.getFileNameAsString() + "'", error);
		}
	}

	private MemorySegment decompressEntry() throws IOException {
		if (limiter == null)
			return ZipCompressions.decompress(fileHeader);
		return switch (fileHeader.getCompressionMethod()) {
			case ZipCompressions.STORED -> limiter.acceptStored(fileHeader);
			case ZipCompressions.DEFLATED -> fileHeader.decompress(limiter);
			default -> ZipCompressions.decompress(fileHeader);
		};
	}
}
