package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

/**
 * Immediate byte source.
 *
 * @author xDark
 */
public final class ByteArraySource implements ByteSource {
	private final byte[] bytes;
	private final int off;
	private final int len;

	/**
	 * @param bytes
	 * 		Input bytes to wrap.
	 */
	public ByteArraySource(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/**
	 * @param bytes
	 * 		Input bytes to wrap.
	 * @param off
	 * 		Start offset.
	 * @param len
	 * 		Length of content.
	 */
	public ByteArraySource(byte[] bytes, int off, int len) {
		this.bytes = bytes;
		this.off = off;
		this.len = len;
	}

	@Nonnull
	@Override
	public byte[] readAll() {
		return Arrays.copyOfRange(bytes, off, len);
	}

	@Nonnull
	@Override
	public byte[] peek(int count) {
		count = Math.min(count, len);
		return Arrays.copyOfRange(bytes, off, count);
	}

	@Nonnull
	@Override
	public InputStream openStream() {
		return new ByteArrayInputStream(bytes, off, len);
	}

	@Nonnull
	@Override
	public MemorySegment mmap() throws IOException {
		return MemorySegment.ofArray(bytes).asSlice(off, len);
	}
}
