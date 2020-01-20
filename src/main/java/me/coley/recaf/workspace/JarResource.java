package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.*;
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
	 * @param file
	 * 		File reference to a jar file.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public JarResource(File file) throws IOException {
		super(ResourceKind.JAR, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are classes and valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if(!loader.isValidClass(entry))
					continue;
				if(!loader.isValidFile(entry))
					continue;
				out.reset();
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtil.toByteArray(stream, out, buffer);
				getEntryLoader().onClass(entry.getName(), in);
			}
		}
		getEntryLoader().finishClasses();
		return getEntryLoader().getClasses();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are not classes and are valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if(loader.isValidClass(entry))
					continue;
				if(!loader.isValidFile(entry))
					continue;
				out.reset();
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtil.toByteArray(stream, out, buffer);
				getEntryLoader().onFile(entry.getName(), in);
			}
		}
		getEntryLoader().finishFiles();
		return getEntryLoader().getFiles();
	}
}
