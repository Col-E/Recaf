package software.coley.recaf.util.io;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import software.coley.recaf.util.MemorySegmentUtil;

public class LargeOutputStream extends OutputStream {
	private final List<byte[]> data = new LinkedList<>();

	@Override
	public void write(int b) {
		this.data.add(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] b) {
		this.data.add(b);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		this.data.add(Arrays.copyOfRange(b, off, off + len));
	}

	public MemorySegment toMemorySegment() {
		return MemorySegmentUtil.from(this.data);
	}
}
