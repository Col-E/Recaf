package software.coley.recaf.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class MemorySegmentUtilTest {
	@Test
	void readWriteAppend() throws IOException {
		var temp = Files.createTempFile("recaf", ".dat");
		var a = "Hello ";
		var b = "World!";
		var c = a + b;

		MemorySegmentUtil.write(temp, MemorySegment.ofArray("dummy".getBytes()), false);
		MemorySegmentUtil.write(temp, MemorySegment.ofArray(a.getBytes()), false);
		MemorySegmentUtil.write(temp, MemorySegment.ofArray(b.getBytes()), true);

		var result = MemorySegmentUtil.read(temp);
		assertArrayEquals(c.getBytes(), result.toArray(ValueLayout.JAVA_BYTE));
	}
}
