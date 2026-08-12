package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.lljzip.util.MemorySegmentInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * {@link ByteSource} implemented via {@link MemorySegment}.
 *
 * @author xDark
 */
public final class MemorySegmentDataSource implements ByteSource, AutoCloseable {
	private final MemorySegment data;

	/**
	 * @param data
	 * 		Data to read bytes from.
	 */
	public MemorySegmentDataSource(MemorySegment data) {
		this.data = data;
	}

	@Override
	public void close() throws Exception {
	}

	@Nonnull
	@Override
	public MemorySegment readAll() throws IOException {
		return this.data;
	}

	@Nonnull
	@Override
	public byte[] peek(int count) {
		MemorySegment data = this.data;
		count = (int) Math.min(count, data.byteSize());
		return data.asSlice(0, count).toArray(ValueLayout.JAVA_BYTE);
	}

	@Nonnull
	@Override
	public InputStream openStream() {
		return new MemorySegmentInputStream(data);
	}

	@Nonnull
	@Override
	public MemorySegment mmap() {
		return data;
	}
}
