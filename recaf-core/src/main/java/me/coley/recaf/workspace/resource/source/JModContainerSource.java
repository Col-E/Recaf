package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSourceConsumer;
import me.coley.recaf.io.ByteSourceElement;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.Unchecked;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Java module container source.
 *
 * @author xDark
 */
public class JModContainerSource extends ContainerContentSource<String> {

	public JModContainerSource(Path path) {
		super(SourceType.JMOD, path);
	}

	@Override
	protected void consumeEach(ByteSourceConsumer<String> entryHandler) throws IOException {
		stream().forEach(ByteSources.consume(entryHandler));
	}

	@Override
	protected Stream<ByteSourceElement<String>> stream() throws IOException {
		ModuleFinder moduleFinder = Unchecked.get(() -> {
			Class<ModuleFinder> klass = (Class<ModuleFinder>) Class.forName("jdk.internal.module.ModulePath", true, null);
			Constructor<ModuleFinder> constructor = klass.getDeclaredConstructor(Runtime.Version.class, boolean.class, Class.forName("jdk.internal.module.ModulePatcher", true, null), Path[].class);
			constructor.setAccessible(true);
			return constructor.newInstance(JarFile.runtimeVersion(), true, null, new Path[] {getPath()});
		});
		return moduleFinder.findAll()
				.stream()
				.map(Unchecked.function(ModuleReference::open))
				.flatMap(Unchecked.function(reader -> {
					return reader.list()
							.map(v -> new ByteSourceElement<>(v, ByteSources.wrap(Unchecked.bmap((t, u) ->
									t.open(u).orElseThrow().readAllBytes(), reader, v))));
				}));
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
