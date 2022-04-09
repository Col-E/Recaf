package me.coley.recaf.workspace.resource.source;

import com.google.common.primitives.Bytes;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Origin location information of archive files.
 *
 * @author Matt Coley
 */
public abstract class ArchiveFileContentSource extends ContainerContentSource<ZipEntry> {
	private static final Logger logger = Logging.get(ArchiveFileContentSource.class);

	protected ArchiveFileContentSource(SourceType type, Path path) {
		super(type, path);
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
		// If the entry name does not have the "CAFEBABE" magic header, its not a class.
		return matchesClassMagic(content);
	}

	@Override
	protected String getPathName(ZipEntry entry) {
		return entry.getName();
	}

	private void handle(Path path, BiConsumer<ZipEntry, byte[]> entryHandler, boolean checkHeader) throws IOException {
		// TODO: Use PatchingZipWriterStrategy - something like...
		//             ZipArchive archive = ZipIO.readJvm(Files.readAllBytes(path));
		//             ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//             new PatchingZipWriterStrategy().write(archive, baos);
		//             byte[] fixed = baos.toByteArray();
		//       But first we need to ensure the patcher is stable and doesn't break anything.
		try (InputStream stream = new FileInputStream(path.toFile())) {
			readFrom(stream, entryHandler);
		} catch (Exception ex) {
			logger.debug("Malformed Zip, attempting to patch: {} - {}", path, ex.getMessage());
			checkInvalidEntryData(path, ex, entryHandler);
			if (checkHeader)
				checkBogusHeaderPK(path, ex, entryHandler);
		}
	}

	private void checkInvalidEntryData(Path path, Exception ex, BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// Check if ZIP entries have falsified CRC values. This will cause:
		// java.util.zip.ZipException: invalid entry CRC (expected 0xaaaaaaaa but got 0xbbbbbbbb)
		//   at java.util.zip.ZipInputStream.readEnd(ZipInputStream.java:394)
		//   at java.util.zip.ZipInputStream.read(ZipInputStream.java:196)
		//   at java.io.FilterInputStream.read(FilterInputStream.java:107)
		// For some reason using "ZipFile"/"JarFile" ignores CRC validity.
		// Similarly, it also ignores invalid entry sizes.
		String message = ex.getMessage();
		if (message != null && (message.contains("invalid entry CRC") || message.contains("invalid entry size"))) {
			try (ZipFile zf = new JarFile(path.toFile())) {
				readFromAlt(zf, entryHandler);
			}
		}
	}

	private void checkBogusHeaderPK(Path path, Exception ex,
									BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// Check if ZIP has bogus data after the file header. This will cause:
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

	private void readFrom(InputStream stream, BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// "ZipInputStream" allows us to parse a ZIP file structure without needing to read the
		// entire thing before any processing gets done. This is nice in case somebody intentionally
		// screws up the ZIP structure's ending sequence, because that will crash "ZipFile"/"JarFile"
		byte[] buf = new byte[16384];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipInputStream zis = new ZipInputStream(stream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				baos.reset();
				IOUtil.copy(zis, baos, buf);
				entryHandler.accept(entry, baos.toByteArray());
			}
		}
	}

	private void readFromAlt(ZipFile zf, BiConsumer<ZipEntry, byte[]> entryHandler) throws IOException {
		// "ZipFile"/"JarFile" reads the entire ZIP file structure before letting us do any entry parsing.
		// This may not always be ideal, but this way has one major bonus. It totally ignores CRC validity.
		// It also ignores a few other zip entry values.
		// Since somebody can intentionally write bogus data there to crash "ZipInputStream" this way works.
		byte[] buf = new byte[16384];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Enumeration<? extends ZipEntry> entries = zf.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			baos.reset();
			try (InputStream zis = zf.getInputStream(entry)) {
				IOUtil.copy(zis, baos, buf);
			}
			entryHandler.accept(entry, baos.toByteArray());
		}
	}
}
