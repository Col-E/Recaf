package software.coley.recaf.util.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

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

		return new LargeByteArray(data);
	}

	public static LargeByteArray from(byte[] data) {
		var tmp = new ArrayList<byte[]>();
		tmp.add(data);
		return new LargeByteArray(tmp);
	}

	public byte[] raw() {
		if (this.data.size() == 1) {
			return this.data.get(0);
		}

		throw new UnsupportedOperationException();
	}
}
