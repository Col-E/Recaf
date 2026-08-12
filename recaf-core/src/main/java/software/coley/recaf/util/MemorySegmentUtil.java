package software.coley.recaf.util;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

public class MemorySegmentUtil {
	private static int ChunkSize = 1024 * 1024 * 1024;

	public static MemorySegment read(Path path) throws IOException {
		var data = new LinkedList<byte[]>();
		try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
			var total = channel.size();
			for (long offset = 0; offset < total; offset += ChunkSize) {
				var size = (int) Math.min(ChunkSize, total - offset);
				var chunk = new byte[size];
				channel.map(FileChannel.MapMode.READ_ONLY, offset, size).get(chunk);
				data.add(chunk);
			}
		}

		return from(data);
	}

	public static Path write(Path path, MemorySegment data, boolean append) throws IOException {
		var option = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				option)) {
			for (var chunk : toChunks(data)) {
				channel.write(ByteBuffer.wrap(chunk));
			}
		}

		return path;
	}

	public static MemorySegment from(List<byte[]> data) {
		long size = 0;
		for (var chunk : data) {
			size += chunk.length;
		}

		var arena = Arena.ofAuto();
		var segment = arena.allocate(size);

		long offset = 0;
		for (var chunk : data) {
			MemorySegment.copy(chunk, 0, segment, ValueLayout.JAVA_BYTE, offset, chunk.length);
			offset += chunk.length;
		}

		return segment;
	}

	public static List<byte[]> toChunks(MemorySegment data) {
		var channel = new LinkedList<byte[]>();
		var total = data.byteSize();
		for (long offset = 0; offset < total; offset += ChunkSize) {
			long size = Math.min(ChunkSize, total - offset);
			var chunk = data.asSlice(offset, size).toArray(ValueLayout.JAVA_BYTE);
			channel.add(chunk);
		}
		return channel;
	}
}
