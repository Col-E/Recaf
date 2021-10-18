package me.coley.recaf.workspace.resource.source;

import com.google.common.primitives.Bytes;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
		Path path = getPath();
		boolean delete = false;
		if (!IOUtil.isOnDefaultFileSystem(path)) {
			Files.copy(path, path = Files.createTempFile("recaf", ".jar"));
			delete = true;
		}
		try {
			handle(path, entryHandler, true);
		} finally {
			if (delete)
				IOUtil.deleteQuietly(path);
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

	private void handle(Path path, BiConsumer<ZipEntry, byte[]> entryHandler, boolean checkHeader) throws IOException {
		Predicate<ZipEntry> filter = getEntryFilter();
		try (InputStream stream = new FileInputStream(path.toFile())) {
			readFrom(stream, filter, entryHandler);
		} catch (Exception ex) {
			logger.debug("Malformed Zip, attempting to patch: {} - {}", path, ex.getMessage());
			checkInvalidCRC(path, ex, filter, entryHandler);
			if (checkHeader)
				checkBogusHeaderPK(path, ex, entryHandler);
		}
	}

	private void checkBogusHeaderPK(Path path, Exception ex,
									BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// Second check, ZIP data is just garbled nonsense. For example:
		// java.lang.IllegalArgumentException: MALFORMED
		//   at java.util.zip.ZipCoder.toString(ZipCoder.java:58)
		//   at java.util.zip.ZipCoder.toStringUTF8(ZipCoder.java:117)
		//   at java.util.zip.ZipInputStream.readLOC(ZipInputStream.java:299)
		//   at java.util.zip.ZipInputStream.getNextEntry(ZipInputStream.java:122)
		// Typically this means they're using the duplicate PK header trick.
		// The JVM can read jars with multiple PK ZIP headers. The first one can be total garbage
		// but if the second one is valid it'll start reading from there.
		String message = ex.getMessage();
		if (message != null && message.contains("MALFORMED")) {
			// Read from file and zero out existing header
			byte[] file = Files.readAllBytes(path);
			for (int i = 0; i < Math.min(4, file.length); i++)
				file[i] = 0;
			// Search for the next 'PK..' header
			byte[] pattern = ByteHeaderUtil.convert(ByteHeaderUtil.ZIP);
			int headerMatch = Bytes.indexOf(file, pattern);
			if (headerMatch >= 4) {
				file = Arrays.copyOfRange(file, headerMatch, file.length);
				Files.write(path, file);
				// Try again with the patched file
				handle(path, entryHandler, false);
			}
		}
	}

	private void checkInvalidCRC(Path path, Exception ex, Predicate<ZipEntry> filter,
								 BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// First check, ZIP CRC values are falsified. For example:
		// java.util.zip.ZipException: invalid entry CRC (expected 0xaaaaaaaa but got 0xbbbbbbbb)
		//   at java.util.zip.ZipInputStream.readEnd(ZipInputStream.java:394)
		//   at java.util.zip.ZipInputStream.read(ZipInputStream.java:196)
		//   at java.io.FilterInputStream.read(FilterInputStream.java:107)
		// For some reason using JarFile ignores them...
		String message = ex.getMessage();
		if (message != null && message.contains("invalid entry CRC")) {
			try (ZipFile zf = new JarFile(path.toFile())) {
				readFromAlt(zf, filter, entryHandler);
			}
		}
	}

	private void readFrom(InputStream stream, Predicate<ZipEntry> filter,
						  BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// "ZipInputStream" allows us to parse a ZIP file structure without needing to read the
		// entire thing before any processing gets done. This is nice in case somebody intentionally
		// screws up the ZIP structure's ending sequence, because that will crash "ZipFile"/"JarFile"
		ZipInputStream zis = new ZipInputStream(stream);
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			if (filter.test(entry)) {
				byte[] content = IOUtil.toByteArray(zis);
				entryHandler.accept(entry, content);
			}
		}
	}

	private void readFromAlt(ZipFile zf, Predicate<ZipEntry> filter,
							 BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// "ZipFile"/"JarFile" reads the entire ZIP file structure before letting us do any entry parsing.
		// This may not always be ideal, but this way has one major bonus. It totally ignores CRC validity.
		// Since somebody can intentionally write bogus data there to crash "ZipInputStream" this way works.
		Enumeration<? extends ZipEntry> entries = zf.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (filter.test(entry)) {
				InputStream zis = zf.getInputStream(entry);
				byte[] content = IOUtil.toByteArray(zis);
				entryHandler.accept(entry, content);
			}
		}
	}
}
