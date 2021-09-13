package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Importable war resource.
 *
 * @author Matt
 */
public class WarResource extends ArchiveResource {
	public static final String WAR_CLASS_PREFIX = "WEB-INF/classes/";

	/**
	 * Constructs a war resource.
	 *
	 * @param path
	 * 		Path reference to a war file.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public WarResource(Path path) throws IOException {
		super(ResourceKind.WAR, path);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate war entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		ZipInputStream zis = new ZipInputStream(new FileInputStream(getPath().toFile()));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			// verify entries are classes and valid files
			// - skip intentional garbage / zip file abnormalities
			if (shouldSkip(entry.getName()))
				continue;
			if(!loader.isValidFileEntry(entry))
				continue;
			if (loader.isValidClassEntry(entry)) {
				out.reset();
				byte[] in = IOUtil.toByteArray(zis, out, buffer);
				// There is no possible way a "class" under 30 bytes is valid
				if (in.length < 30)
					continue;
				String name = entry.getName();
				if (name.startsWith(WAR_CLASS_PREFIX))
					name = name.substring(WAR_CLASS_PREFIX.length());
				loader.onClass(name, in);
			}
		}
		loader.finishClasses();
		return loader.getClasses();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		// iterate war entries
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
