package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSourceConsumer;
import me.coley.recaf.io.ByteSourceElement;
import me.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Stream;

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
	protected void consumeEach(ByteSourceConsumer<Path> entryHandler) throws IOException {
		Files.walkFileTree(getPath(), new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Actually fallback to java.io package if possible,
				// because IO is faster than NIO when for file status checking.
				if (IOUtil.isRegularFile(file)) {
					entryHandler.accept(file, new PathByteSource(file));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	protected Stream<ByteSourceElement<Path>> stream() throws IOException {
		return Files.walk(getPath(), Integer.MAX_VALUE)
				.filter(IOUtil::isRegularFile)
				.map(x -> new ByteSourceElement<>(x, new PathByteSource(x)));
	}

	@Override
	protected boolean isClass(Path entry, ByteSource content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, its not a class.
		return matchesClassMagic(content.peek(17));
	}

	@Override
	protected boolean isClass(Path entry, byte[] content) throws IOException {
		// If the entry does not have the "CAFEBABE" magic header, its not a class.
		return matchesClassMagic(content);
	}

	@Override
	protected String getPathName(Path entry) {
		String absolutePath = getPath().toAbsolutePath().toString();
		String absoluteEntry = entry.toAbsolutePath().toString();
		return absoluteEntry.substring(absolutePath.length() + 1);
	}
	
	private static final class PathByteSource implements ByteSource {
		private final Path path;

		PathByteSource(Path path) {
			this.path = path;
		}

		@Override
		public byte[] readAll() throws IOException {
			return Files.readAllBytes(path);
		}

		@Override
		public byte[] peek(int count) throws IOException {
			try (InputStream in = Files.newInputStream(path)) {
				byte[] buf = new byte[count];
				int offset = 0;
				int r;
				while ((r = in.read(buf, offset, count)) != -1) {
					offset += r;
					count -= r;
				}
				return count == 0 ? buf : Arrays.copyOf(buf, offset);
			}
		}
	}
}
