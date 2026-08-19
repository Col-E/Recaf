package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Bounds the amount of data inflated from entries in one ZIP archive.
 * Limits are checked against actual output, so forged header sizes cannot bypass them.
 *
 * @author jus7n
 */
public final class ZipDecompressionLimiter implements Decompressor {
	private static final int BUFFER_SIZE = 8192;
	private final long maxEntrySize;
	private final long maxTotalSize;
	private final int maxCompressionRatio;
	private final AtomicLong totalSize = new AtomicLong();

	/**
	 * @param maxEntrySize
	 * 		Maximum decompressed bytes for one entry.
	 * @param maxTotalSize
	 * 		Maximum decompressed bytes retained from the archive.
	 * @param maxCompressionRatio
	 * 		Maximum ratio of decompressed to compressed bytes.
	 */
	public ZipDecompressionLimiter(long maxEntrySize, long maxTotalSize, int maxCompressionRatio) {
		this.maxEntrySize = Math.max(0, maxEntrySize);
		this.maxTotalSize = Math.max(0, maxTotalSize);
		this.maxCompressionRatio = Math.max(1, maxCompressionRatio);
	}

	/**
	 * Accounts for an uncompressed entry.
	 *
	 * @param header
	 * 		Entry header.
	 *
	 * @return Entry data.
	 *
	 * @throws IOException
	 * 		When the entry exceeds a configured limit.
	 */
	@Nonnull
	public MemorySegment acceptStored(@Nonnull LocalFileHeader header) throws IOException {
		MemorySegment data = header.getFileData();
		long size = data.byteSize();
		if (size < 0 || (maxEntrySize > 0 && size > maxEntrySize))
			throw new ZipDecompressionLimitException("Refusing to decompress '" + header.getFileNameAsString() +
					"': entry expands beyond " + maxEntrySize + " bytes");
		reserveTotal(header, size);
		return data;
	}

	@Nonnull
	@Override
	public MemorySegment decompress(@Nonnull LocalFileHeader header, @Nonnull MemorySegment data) throws IOException {
		// Validate the input before setting up inflater state.
		if (header.getCompressionMethod() != ZipCompressions.DEFLATED)
			throw new IOException("LocalFileHeader contents not using 'Deflated'!");

		// Derive the effective entry limit from both its absolute size and compression ratio, saturating on overflow.
		long compressedSize = data.byteSize();
		long ratioLimit = compressedSize > Long.MAX_VALUE / maxCompressionRatio ? Long.MAX_VALUE : compressedSize * maxCompressionRatio;
		long entryLimit = Math.min(maxEntrySize, ratioLimit);

		// Reject honest oversized entries from their metadata before allocating any output buffer.
		long declaredSize = header.getUncompressedSize();
		if (declaredSize < 0 || (entryLimit > 0 && declaredSize > entryLimit))
			throw new ZipDecompressionLimitException("Refusing to decompress '" + header.getFileNameAsString() +
					"': entry expands beyond " + entryLimit + " bytes");

		// Set up reusable input/output chunks around a raw DEFLATE inflater, as required by the ZIP format.
		Inflater inflater = new Inflater(true);
		LimitedByteArrayOutputStream output = new LimitedByteArrayOutputStream();
		byte[] inputBuffer = new byte[BUFFER_SIZE];
		byte[] outputBuffer = new byte[BUFFER_SIZE];
		MemorySegment inputSegment = MemorySegment.ofArray(inputBuffer);
		long inputOffset = 0;
		long entrySize = 0;

		// Failed attempts return their reserved archive budget, and all paths release the inflater's native state.
		try {
			// Incrementally feed compressed input and account for every output chunk before retaining it.
			while (!inflater.finished()) {
				// Refill only when the inflater has consumed its prior input.
				if (inflater.needsInput()) {
					if (inputOffset >= data.byteSize())
						throw new ZipException("Truncated deflate stream");
					int count = (int) Math.min(inputBuffer.length, data.byteSize() - inputOffset);
					MemorySegment.copy(data, inputOffset, inputSegment, 0, count);
					inputOffset += count;
					inflater.setInput(inputBuffer, 0, count);
				}

				// Inflate one chunk, then enforce per-entry and archive-wide limits before writing it.
				int count = inflater.inflate(outputBuffer);
				if (count > 0) {
					long nextEntrySize = entrySize + count;
					if (nextEntrySize < 0 || (entryLimit > 0 && nextEntrySize > entryLimit))
						throw new ZipDecompressionLimitException("Refusing to decompress '" + header.getFileNameAsString() +
								"': entry expands beyond " + entryLimit + " bytes");
					reserveTotal(header, count);
					entrySize = nextEntrySize;
					output.write(outputBuffer, 0, count);
				} else if (inflater.needsDictionary()) {
					throw new ZipException("Deflate stream requires a preset dictionary");
				} else if (!inflater.needsInput()) {
					throw new ZipException("Deflate stream made no progress");
				}
			}
			return output.wrap();
		} catch (DataFormatException ex) {
			totalSize.addAndGet(-entrySize);
			throw (ZipException) new ZipException("Invalid ZLIB data format").initCause(ex);
		} catch (Throwable throwable) {
			totalSize.addAndGet(-entrySize);
			throw throwable;
		} finally {
			inflater.end();
		}
	}

	private void reserveTotal(@Nonnull LocalFileHeader header, long amount) throws IOException {
		while (true) {
			long current = totalSize.get();
			long next = current + amount;
			if (next < current || (maxTotalSize > 0 && next > maxTotalSize))
				throw new ZipDecompressionLimitException("Refusing to decompress '" + header.getFileNameAsString() +
						"': archive expands beyond " + maxTotalSize + " bytes");
			if (totalSize.compareAndSet(current, next))
				return;
		}
	}

	private static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
		private LimitedByteArrayOutputStream() {
			super(BUFFER_SIZE);
		}

		private MemorySegment wrap() {
			return MemorySegment.ofArray(buf).asSlice(0, count);
		}
	}

	static final class ZipDecompressionLimitException extends IOException {
		ZipDecompressionLimitException(String message) {
			super(message);
		}
	}
}
