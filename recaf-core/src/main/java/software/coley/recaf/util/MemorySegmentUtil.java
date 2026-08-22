package software.coley.recaf.util;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

import software.coley.recaf.util.io.ByteHeaderUtil;

public class MemorySegmentUtil {
	private static int ChunkSize = 1024 * 1024 * 1024;

	public static MemorySegment read(Path path) throws IOException {
		try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
			return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), Arena.ofAuto());
		}
	}

	public static Path write(Path path, MemorySegment data, boolean append) throws IOException {
		var options = new OpenOption[] {
			StandardOpenOption.READ,
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE,
		};
		try (FileChannel channel = FileChannel.open(path, options)) {
			if (!append) {
				channel.truncate(0);
			}

			var target = channel.map(FileChannel.MapMode.READ_WRITE, channel.size(), data.byteSize(), Arena.ofAuto());
			MemorySegment.copy(data, 0, target, 0, data.byteSize());
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

	public static HashCode hash(MemorySegment data, HashFunction fn) {
		var hasher = fn.newHasher();
		for (var chunk : toChunks(data)) {
			hasher.putBytes(chunk);
		}
		return hasher.hash();
	}

	public static byte[] header(MemorySegment data) {
		return data.asSlice(0, Math.min(data.byteSize(), 64)).toArray(ValueLayout.JAVA_BYTE);
	}

	public static boolean matchAtAnyOffset(MemorySegment data, int[] pattern) {
		if (data.byteSize() < pattern.length) {
			return false;
		}

		var a = new byte[0];
		for (var b : toChunks(data)) {
			var combined = new byte[a.length + b.length];
			System.arraycopy(a, 0, combined, 0, a.length);
			System.arraycopy(b, 0, combined, a.length, b.length);

			if (ByteHeaderUtil.matchAtAnyOffset(combined, pattern)) {
				return true;
			}
			a = Arrays.copyOfRange(b, b.length - pattern.length, b.length);
		}

		return false;
	}

	public static boolean equals(MemorySegment a, MemorySegment b) {
		if (a.byteSize() != b.byteSize()) {
			return false;
		}

		var chunkA = toChunks(a);
		var chunkB = toChunks(b);

		for (int i = 0; i < chunkA.size(); i++) {
			if (!Arrays.equals(chunkA.get(i), chunkB.get(i))) {
				return false;
			}
		}

		return true;
	}
}
