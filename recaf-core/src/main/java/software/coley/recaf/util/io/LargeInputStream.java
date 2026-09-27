package software.coley.recaf.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public class LargeInputStream extends InputStream {
	private MemorySegment data;
	private long pos;
	private long count;

	public LargeInputStream(MemorySegment data) {
		this.data = data;
		this.pos = 0;
		this.count = data.byteSize();
	}

	@Override
	public int read() throws IOException {
		if (this.pos >= this.count) {
			return -1;
		}

		var value = this.data.get(ValueLayout.JAVA_BYTE, this.pos);
		this.pos += 1;
		return value;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		if (this.pos >= this.count) {
			return -1;
		}

		long avail = this.count - this.pos;
		if (len > avail) {
			len = (int) avail;
		}

		if (len <= 0) {
			return 0;
		}

		MemorySegment.copy(this.data, ValueLayout.JAVA_BYTE, this.pos, b, off, len);
		this.pos += len;
		return len;
	}
}
