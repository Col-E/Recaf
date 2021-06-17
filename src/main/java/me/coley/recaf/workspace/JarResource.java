package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
		try (ZipFile zipFile = new ZipFile(getPath().toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are classes and valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if (!loader.isValidClassEntry(entry)) {
					// Maybe it is actually valid?
					try (InputStream in = zipFile.getInputStream(entry)) {
						if (!loader.isValidClassFile(in)) {
							continue;
						}
					}
				}
				out.reset();
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtil.toByteArray(stream, out, buffer);
				// There is no possible way a "class" under 30 bytes is valid
				if (in.length < 30)
					continue;
				loader.onClass(entry.getName(), in);
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
