package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Importable jar resource.
 *
 * @author Matt
 */
public class JarResource extends ArchiveResource {
	/**
	 * Constructs a jar resource.
	 *
	 * @param path
	 * 		Path reference to a jar file.
	 *
	 * @throws IOException
	 * 		When the path does not exist.
	 */
	public JarResource(Path path) throws IOException {
		super(ResourceKind.JAR, path);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(getPath().toFile()))) {
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {
				// verify entries are classes and valid files
				// - skip intentional garbage / zip file abnormalities
				if (shouldSkip(entry.getName()))
					continue;

				out.reset();
				byte[] in;
				if (!loader.isValidClassEntry(entry)) {
					// The class file might not end with .class or .class/
					// so we also check it's header.
					in = IOUtil.toByteArray(zis, out, buffer, 4);
					if (!loader.isValidClassFile(new ByteArrayInputStream(in))) {
						continue;
					}
				}

				in = IOUtil.toByteArray(zis, out, buffer);

				// There is no possible way a "class" under 30 bytes is valid
				if (in.length < 30)
					continue;

				loader.onClass(entry.getName(), in);
			}
		} catch (ZipException e) {
			if (e.getMessage().contains("invalid entry CRC")) {
				// "ZipFile"/"JarFile" reads the entire ZIP file structure before letting us do any entry parsing.
				// This may not always be ideal, but this way has one major bonus. It totally ignores CRC validity.
				// It also ignores a few other zip entry values.
				// Since somebody can intentionally write bogus data there to crash "ZipInputStream" this way works.
				try (ZipFile zf = new ZipFile(getPath().toString())) {
					Enumeration<? extends ZipEntry> entries = zf.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();

						if (shouldSkip(entry.getName()))
							continue;

						out.reset();
						byte[] in;

						if (!loader.isValidClassEntry(entry)) {
							// The class file might not end with .class or .class/
							// so we also check it's header.
							out.reset();
							try (InputStream zis = zf.getInputStream(entry)) {
								in = IOUtil.toByteArray(zis, out, buffer, 4);
							}
							if (!loader.isValidClassFile(new ByteArrayInputStream(in))) {
								continue;
							}
						}

						out.reset();
						try (InputStream zis = zf.getInputStream(entry)) {
							in = IOUtil.toByteArray(zis, out, buffer);
						}

						loader.onClass(entry.getName(), in);
					}
				}
			}
		}
		loader.finishClasses();
		return loader.getClasses();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		try (ZipFile zipFile = new ZipFile(getPath().toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are not classes and are valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if(loader.isValidClassEntry(entry))
					continue;
				if(!loader.isValidFileEntry(entry))
					continue;
				out.reset();
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtil.toByteArray(stream, out, buffer);
				loader.onFile(entry.getName(), in);
			}
		}
		loader.finishFiles();
		return loader.getFiles();
	}
}
