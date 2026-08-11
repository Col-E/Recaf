package software.coley.recaf.util.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import software.coley.recaf.util.ByteHeaderUtil;

public class LargeByteArray {
	private static int ChunkSize = 1024 * 1024 * 1024;
	private final List<byte[]> data;

	private LargeByteArray(List<byte[]> data) {
		this.data = data;
	}

	public static LargeByteArray readAllBytes(Path path) throws IOException {
		var data = new ArrayList<byte[]>();

		try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
			var total = channel.size();
			for (long offset = 0; offset < total; offset += ChunkSize) {
				var size = (int) Math.min(ChunkSize, total - offset);
				var chunk = new byte[size];
				channel.map(FileChannel.MapMode.READ_ONLY, offset, size).get(chunk);
				data.add(chunk);
			}
		}

		if (data.isEmpty()) {
			data.add(new byte[0]);
		}

		return new LargeByteArray(data);
	}

	public static LargeByteArray from(byte[] data) {
		var tmp = new ArrayList<byte[]>();
		tmp.add(data);
		return new LargeByteArray(tmp);
	}

	/**
	 * Only use {@link #raw()} if the data ends up in something java-internal that
	 * only accepts byte[].
	 */
	public byte[] raw() throws UnsupportedOperationException {
		if (this.data.size() == 1) {
			return this.data.get(0);
		}

		throw new UnsupportedOperationException();
	}

	@Deprecated
	public byte[] rawToBeReplaced() throws UnsupportedOperationException {
		return raw();
	}

	public byte[] header() {
		var data = this.data.get(0);
		return Arrays.copyOfRange(data, 0, 64);
	}

	public boolean matchAtAnyOffset(int[] pattern) {
		var a = new byte[0];
		for (var b : this.data) {
			var combined = new byte[a.length + b.length];
			System.arraycopy(a, 0, combined, 0, a.length);
			System.arraycopy(b, 0, combined, a.length, b.length);

			if (ByteHeaderUtil.match(combined, pattern)) {
				return true;
			}
			a = Arrays.copyOfRange(b, b.length - pattern.length, b.length);
		}
		return false;
	}
}
