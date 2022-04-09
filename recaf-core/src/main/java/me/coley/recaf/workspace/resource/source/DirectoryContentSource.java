package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;

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
	protected void consumeEach(BiConsumer<Path, byte[]> entryHandler) throws IOException {
		Files.walkFileTree(getPath(), new SimpleFileVisitor<>() {
			private final byte[] buffer = IOUtil.newByteBuffer();

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Actually fallback to java.io package if possible,
				// because IO is faster than NIO when for file status checking.
				if ((IOUtil.isOnDefaultFileSystem(file) && file.toFile().isFile()) || Files.isRegularFile(file)) {
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
}
