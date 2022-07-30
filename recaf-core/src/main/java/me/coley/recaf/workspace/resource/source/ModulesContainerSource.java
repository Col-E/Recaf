package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSourceConsumer;
import me.coley.recaf.io.ByteSourceElement;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.LookupUtil;
import me.coley.recaf.util.Unchecked;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Java modules source.
 *
 * @author xDark
 */
public class ModulesContainerSource extends ContainerContentSource<String> {
	/**
	 * @param path
	 * 		Path to modules file.
	 */
	public ModulesContainerSource(Path path) {
		super(SourceType.MODULES, path);
	}

	@Override
	protected void consumeEach(ByteSourceConsumer<String> entryHandler) throws IOException {
		try (Stream<ByteSourceElement<String>> stream = stream()) {
			stream.forEach(ByteSources.consume(entryHandler));
		}
	}

	@Override
	@SuppressWarnings("Convert2MethodRef") // javac doesn't like reference on 'bmap(...)'
	protected Stream<ByteSourceElement<String>> stream() throws IOException {
		return Unchecked.map(path -> {
			Class<?> imageReaderClass = Class.forName("jdk.internal.jimage.ImageReader", true, null);
			Method m = imageReaderClass.getDeclaredMethod("open", Path.class);
			m.setAccessible(true);
			Object reader = m.invoke(null, path);
			m = imageReaderClass.getDeclaredMethod("getEntryNames");
			m.setAccessible(true);
			String[] entries = (String[]) m.invoke(reader);
			MethodHandles.Lookup lookup = LookupUtil.lookup();
			Class<?> imageLocationClass = Class.forName("jdk.internal.jimage.ImageLocation", true, null);
			MethodHandle getResourceBuffer = lookup.findVirtual(imageReaderClass, "getResourceBuffer", MethodType.methodType(ByteBuffer.class, imageLocationClass))
					.bindTo(reader);
			MethodHandle findLocation = lookup.findVirtual(imageReaderClass, "findLocation", MethodType.methodType(imageLocationClass, String.class))
					.bindTo(reader);
			return Arrays.stream(entries)
					.map(x -> {
						String normalizedName = x.substring(x.indexOf('/', 1) + 1);
						Object imageLocation = Unchecked.bmap((t, u) -> t.invoke(u), findLocation, x);
						ByteBuffer buffer = Unchecked.bmap((t, u) -> (ByteBuffer) t.invoke(u), getResourceBuffer, imageLocation);
						return new ByteSourceElement<>(normalizedName, ByteSources.forBuffer(buffer));
					}).onClose(() -> IOUtil.closeQuietly((AutoCloseable) reader));
		}, getPath());
	}

	@Override
	protected boolean isClass(String entry, ByteSource content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, it's not a class.
		return matchesClass(content.peek(17));
	}

	@Override
	protected boolean isClass(String entry, byte[] content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, it's not a class.
		return matchesClass(content);
	}

	@Override
	protected String getPathName(String entry) {
		return entry;
	}
}
