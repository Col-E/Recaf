package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Origin location information of archive files.
 *
 * @author Matt Coley
 */
public abstract class ArchiveFileContentSource extends ContainerContentSource<ZipEntry> {
	private static final int BUFFER_SIZE = (int) Math.pow(2, 20);

	protected ArchiveFileContentSource(SourceType type, Path path) {
		super(type, path);
	}

	@Override
	protected void writeContent(Path output, SortedMap<String, byte[]> content) throws IOException {
		OutputStream fos = new BufferedOutputStream(Files.newOutputStream(output), BUFFER_SIZE);
		try (ZipOutputStream zos = new ZipOutputStream(fos)) {
			Set<String> dirsVisited = new HashSet<>();
			// Contents are in sorted order, so we can insert directory entries before file entries occur.
			for (Map.Entry<String, byte[]> entry : content.entrySet()) {
				String key = entry.getKey();
				byte[] out = entry.getValue();
				// Write directories for upcoming entries if necessary
				// - Ugly, but does the job.
				if (key.contains("/")) {
					// Record directories
					String parent = key;
					List<String> toAdd = new ArrayList<>();
					do {
						parent = parent.substring(0, parent.lastIndexOf('/'));
						if (dirsVisited.add(parent)) {
							toAdd.add(0, parent + '/');
						} else break;
					} while (parent.contains("/"));
					// Put directories in order of depth
					for (String dir : toAdd) {
						zos.putNextEntry(new JarEntry(dir));
						zos.closeEntry();
					}
				}
				// Write entry content
				zos.putNextEntry(new JarEntry(key));
				zos.write(out);
				zos.closeEntry();
			}
		}
		fos.flush();
		fos.close();
	}

	@Override
	protected void consumeEach(BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		Predicate<ZipEntry> filter = getEntryFilter();
		byte[] buffer = IOUtil.newByteBuffer();
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(getPath()))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (filter.test(entry)) {
					byte[] content = IOUtil.toByteArray(zis, buffer);
					entryHandler.accept(entry, content);
				}
			}
		}
	}

	@Override
	protected boolean isClass(ZipEntry entry, byte[] content) {
		// We do not check for equality because of some zip file tricks like "class/" being technically valid
		// If the entry name does not contain "class" and does not have the "CAFEBABE" magic header, its not a class.
		String ext = getExtension(entry.getName());
		return ext != null && ext.contains("class") && matchesClassMagic(content);
	}

	@Override
	protected String getPathName(ZipEntry entry) {
		return entry.getName();
	}

	@Override
	protected Predicate<ZipEntry> createDefaultFilter() {
		return entry -> {
			String name = entry.getName();
			// If the entry is a directory, then skip it....
			// Unless its a "fake" directory because archive manipulation by obfuscation
			boolean hasClassExt = name.endsWith(".class") || name.endsWith(".class/");
			if (entry.isDirectory() && !hasClassExt) {
				return false;
			}
			// Skip relative path names / directory escaping
			if (name.contains("../")) {
				return false;
			}
			// Skip if path contains zero-width sub-directory name.
			return !name.contains("//");
		};
	}
}
