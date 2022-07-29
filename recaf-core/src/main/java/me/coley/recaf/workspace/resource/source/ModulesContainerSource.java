package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSourceConsumer;
import me.coley.recaf.io.ByteSourceElement;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.LookupUtil;
import me.coley.recaf.util.Unchecked;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class ModulesContainerSource extends ContainerContentSource<String> {

	public ModulesContainerSource(Path path) {
		super(SourceType.MODULES, path);
	}

	@Override
	protected void consumeEach(ByteSourceConsumer<String> entryHandler) throws IOException {
		try (Stream<ByteSourceElement<String>> x = stream()) {
			x.forEach(ByteSources.consume(entryHandler));
		}
	}

	@Override
	protected Stream<ByteSourceElement<String>> stream() throws IOException {
		return Unchecked.map(path -> {
			Class<?> imageReaderClass = Class.forName("jdk.internal.jimage.ImageReader", true, null);
			Method m = imageReaderClass.getDeclaredMethod("open", Path.class);
			m.setAccessible(true);
			Object reader = m.invoke(null, path);
			m = imageReaderClass.getDeclaredMethod("getEntryNames");
			m.setAccessible(true);
			String[] entries = (String[]) m.invoke(reader);
			m = imageReaderClass.getDeclaredMethod("getResource", String.class);
			m.setAccessible(true);
			MethodHandle getResource = LookupUtil.lookup()
					.unreflect(m)
					.bindTo(reader);
			return Arrays.stream(entries)
					.map(x -> {
						return new ByteSourceElement<>(x.substring(x.indexOf('/', 1) + 1), Unchecked.bmap((t, u) ->
								ByteSources.wrap((byte[]) t.invoke(u)), getResource, x));
					}).onClose(() -> IOUtil.closeQuietly((AutoCloseable) reader));
		}, getPath());
	}

	@Override
	protected boolean isClass(String entry, ByteSource content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, its not a class.
		return matchesClass(content.peek(17));
	}

	@Override
	protected boolean isClass(String entry, byte[] content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, its not a class.
		return matchesClass(content);
	}

	@Override
	protected String getPathName(String entry) {
		return entry;
	}
}
