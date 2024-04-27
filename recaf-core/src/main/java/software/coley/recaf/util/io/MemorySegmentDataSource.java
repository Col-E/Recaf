package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	public byte[] readAll() throws IOException {
		MemorySegment data = this.data;
		if (data.byteSize() > Integer.MAX_VALUE - 8) {
			throw new IOException("Too large content");
		}
		return data.toArray(ValueLayout.JAVA_BYTE);
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
	public MemorySegment mmap() throws IOException {
		return data;
	}

	static final class MemorySegmentInputStream extends InputStream {
		private final MemorySegment data;
		private long read;
		private long markedOffset = -1;
		private long markedLimit;
		private volatile boolean closed;

		MemorySegmentInputStream(MemorySegment data) {
			this.data = data;
		}

		private void checkMarkLimit() {
			if (markedOffset > -1) {
				// Discard if we passed the read limit for our mark
				long diff = read - markedOffset;
				if (diff > markedLimit) {
					markedOffset = -1;
				}
			}
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public synchronized void mark(int limit) {
			// Record current position and read-limit
			markedOffset = read;
			markedLimit = limit;
		}

		@Override
		public synchronized void reset() {
			// Revert read to marked position.
			read = markedOffset;
		}

		@Override
		public int read() throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			if (read >= data.byteSize()) {
				return -1;
			}
			byte b = data.get(ValueLayout.JAVA_BYTE, read++);
			checkMarkLimit();
			return b & 0xff;
		}

		@Override
		public int read(@Nonnull byte[] b, int off, int len) throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			long read = this.read;
			long length = data.byteSize();
			if (read >= length) {
				return -1;
			}
			long remaining = length - read;
			len = (int) Math.min(remaining, len);
			MemorySegment.copy(data, read, MemorySegment.ofArray(b), off, len);
			this.read += len;
			checkMarkLimit();
			return len;
		}

		@Override
		public byte[] readNBytes(int len) throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			long read = this.read;
			long length = data.byteSize();
			if (read >= length) {
				return new byte[0];
			}
			long remaining = length - read;
			len = (int) Math.min(remaining, len);
			byte[] buf = new byte[len];
			MemorySegment.copy(data, read, MemorySegment.ofArray(buf), 0, len);
			this.read += len;
			checkMarkLimit();
			return buf;
		}

		@Override
		public long skip(long n) throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			long read = this.read;
			long length = data.byteSize();
			if (read >= length) {
				return 0;
			}
			n = Math.min(n, length - read);
			this.read += n;
			checkMarkLimit();
			return n;
		}

		@Override
		public int available() throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			long length = data.byteSize();
			long read = this.read;
			if (read >= length) {
				return 0;
			}
			long remaining = length - read;
			if (remaining > Integer.MAX_VALUE)
				return Integer.MAX_VALUE;
			return (int) remaining;
		}

		@Override
		public void close() throws IOException {
			closed = true;
		}

		@Override
		public long transferTo(OutputStream out) throws IOException {
			ensureOpen();
			MemorySegment data = this.data;
			long length = data.byteSize();
			long read = this.read;
			if (read >= length) {
				return 0L;
			}
			long remaining = length - read;
			byte[] buffer = IOUtil.newByteBuffer();
			MemorySegment bufferSegment = MemorySegment.ofArray(buffer);
			while (read < length) {
				int copyable = (int) Math.min(buffer.length, length - read);
				MemorySegment.copy(data, read, bufferSegment, 0, copyable);
				out.write(buffer, 0, copyable);
				read += copyable;
			}
			this.read = length;
			checkMarkLimit();
			return remaining;
		}

		private void ensureOpen() throws IOException {
			if (closed)
				throw new IOException("Stream closed");
		}
	}
}
