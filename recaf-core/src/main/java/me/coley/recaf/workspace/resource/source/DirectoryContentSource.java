package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Origin location information of archive files.
 *
 * @author Matt Coley
 */
public class DirectoryContentSource extends ContainerContentSource<Path> {
	/**
	 * @param path
	 * 		Root directory containing content.
	 */
	public DirectoryContentSource(Path path) {
		super(SourceType.DIRECTORY, path);
	}

	@Override
	protected void writeContent(Path output, SortedMap<String, byte[]> content) throws IOException {
		for (Map.Entry<String, byte[]> entry : content.entrySet()) {
			String name = entry.getKey();
			byte[] out = entry.getValue();
			Path path = output.resolve(name);
			Files.createDirectories(path.getParent());
			Files.write(path, out);
		}
	}

	@Override
	protected void consumeEach(BiConsumer<Path, byte[]> entryHandler) throws IOException {
		Predicate<Path> predicate = getEntryFilter();
		Files.walkFileTree(getPath(), new SimpleFileVisitor<Path>() {

			private final byte[] buffer = IOUtil.newByteBuffer();

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (predicate.test(file)) {
					byte[] content;
					try (InputStream in = Files.newInputStream(file)) {
						content = IOUtil.toByteArray(in, buffer);
					}
					entryHandler.accept(file, content);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	protected boolean isClass(Path entry, byte[] content) {
		// If the entry does not have the "CAFEBABE" magic header, its not a class.
		return matchesClassMagic(content);
	}

	@Override
	protected String getPathName(Path entry) {
		String absolutePath = getPath().toAbsolutePath().toString();
		String absoluteEntry = entry.toAbsolutePath().toString();
		return absoluteEntry.substring(absolutePath.length() + 1);
	}

	@Override
	protected Predicate<Path> createDefaultFilter() {
		// Only allow files
		return path -> {
			// Actually fallback to java.io package if possible,
			// because IO is faster than NIO when for file status checking.
			if (IOUtil.isOnDefaultFileSystem(path)) {
				return path.toFile().isFile();
			}
			return Files.isRegularFile(path);
		};
	}
}
