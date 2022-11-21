package me.coley.recaf.io;

import me.coley.recaf.util.threading.ThreadLocals;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link ByteSource} implemented via {@link ByteData} from LLJ-zip.
 *
 * @author xDark
 */
public final class ByteDataSource implements ByteSource, AutoCloseable {
	private final ByteData data;

	/**
	 * @param data
	 * 		Data to read bytes from.
	 */
	public ByteDataSource(ByteData data) {
		this.data = data;
	}

	@Override
	public void close() throws Exception {
		data.close();
	}

	@Override
	public byte[] readAll() throws IOException {
		ByteData data = this.data;
		if (data.length() > Integer.MAX_VALUE - 8) {
			throw new IOException("Too large content");
		}
		return ByteDataUtil.toByteArray(data);
	}

	@Override
	public byte[] peek(int count) throws IOException {
		ByteData data = this.data;
		count = (int) Math.min(count, data.length());
		byte[] buf = new byte[count];
		data.get(0L, buf, 0, count);
		return buf;
	}

	@Override
	public InputStream openStream() throws IOException {
		return new ByteDataInputStream(data);
	}

	private static final class ByteDataInputStream extends InputStream {
		private final ByteData data;
		private long read;
		private volatile boolean closed;

		ByteDataInputStream(ByteData data) {
			this.data = data;
		}

		@Override
		public int read() throws IOException {
			ensureOpen();
			ByteData data = this.data;
			if (read >= data.length()) {
				return -1;
			}
			return data.get(read++);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			ensureOpen();
			ByteData data = this.data;
			long read = this.read;
			long length = data.length();
			if (read >= length) {
				return -1;
			}
			long remaining = length - read;
			len = (int) Math.min(remaining, len);
			data.get(read, b, off, len);
			this.read += len;
			return len;
		}

		@Override
		public byte[] readNBytes(int len) throws IOException {
			ensureOpen();
			ByteData data = this.data;
			long read = this.read;
			long length = data.length();
			if (read >= length) {
				return new byte[0];
			}
			long remaining = length - read;
			len = (int) Math.min(remaining, len);
			byte[] buf = new byte[len];
			data.get(read, buf, 0, len);
			this.read += len;
			return buf;
		}

		@Override
		public long skip(long n) throws IOException {
			ensureOpen();
			ByteData data = this.data;
			long read = this.read;
			long length = data.length();
			if (read >= length) {
				return 0;
			}
			n = Math.min(n, length - read);
			this.read += n;
			return n;
		}

		@Override
		public int available() throws IOException {
			ensureOpen();
			ByteData data = this.data;
			long length = data.length();
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
			if (!closed) {
				synchronized (this) {
					if (closed)
						return;
					closed = true;
					data.close();
				}
			}
		}

		@Override
		public long transferTo(OutputStream out) throws IOException {
			ensureOpen();
			ByteData data = this.data;
			long length = data.length();
			long read = this.read;
			if (read >= length) {
				return 0L;
			}
			long remaining = length - read;
			data.transferTo(out, ThreadLocals.getByteBuffer());
			this.read = length;
			return remaining;
		}

		private void ensureOpen() throws IOException {
			if (closed)
				throw new IOException("Stream closed");
		}
	}
}
