package me.coley.recaf.workspace.resource.source;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Origin location information of archive files.
 *
 * @author Matt Coley
 */
public class DirectoryContentSource extends ContainerContentSource<File> {
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
			// TODO Plugins: Export intercept plugin support?
			Path path = output.resolve(name);
			Files.createDirectories(path.getParent());
			Files.write(path, out);
		}
	}

	@Override
	protected void consumeEach(BiConsumer<File, byte[]> entryHandler) throws IOException {
		IOFileFilter anyMatch = TrueFileFilter.INSTANCE;
		Iterator<File> it = FileUtils.iterateFiles(getPath().toFile(), anyMatch, anyMatch);
		while (it.hasNext()) {
			File file = it.next();
			if (getEntryFilter().test(file)) {
				entryHandler.accept(file, FileUtils.readFileToByteArray(file));
			}
		}
	}

	@Override
	protected boolean isClass(File entry, byte[] content) {
		// If the entry name does not equal "class" and does not have the "CAFEBABE" magic header, its not a class.
		return "class".equals(getExtension(entry.getName())) && matchesClassMagic(content);
	}

	@Override
	protected String getPathName(File entry) {
		String absolutePath = getPath().toAbsolutePath().toString();
		String absoluteEntry = entry.getAbsolutePath();
		return absoluteEntry.substring(absolutePath.length() + 1);
	}

	@Override
	protected Predicate<File> createDefaultFilter() {
		// Only allow files
		return File::isFile;
	}
}
