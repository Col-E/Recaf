package me.coley.recaf.workspace;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
	 * @param file
	 * 		File reference to a war file.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public WarResource(File file) throws IOException {
		super(ResourceKind.WAR, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate war entries
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are classes and valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if(!getEntryLoader().isValidClass(entry))
					continue;
				if(!getEntryLoader().isValidFile(entry))
					continue;
				InputStream stream = zipFile.getInputStream(entry);
				String name = entry.getName();
				if (name.startsWith(WAR_CLASS_PREFIX))
					name = name.substring(WAR_CLASS_PREFIX.length());
				byte[] in = IOUtils.toByteArray(stream);
				getEntryLoader().onClass(name, in);
			}
		}
		getEntryLoader().finishClasses();
		return getEntryLoader().getClasses();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		// iterate war entries
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are not classes and are valid files
				// - skip intentional garbage / zip file abnormalities
				ZipEntry entry = entries.nextElement();
				if (shouldSkip(entry.getName()))
					continue;
				if(getEntryLoader().isValidClass(entry))
					continue;
				if(!getEntryLoader().isValidFile(entry))
					continue;
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtils.toByteArray(stream);
				getEntryLoader().onFile(entry.getName(), in);
			}
		}
		getEntryLoader().finishFiles();
		return getEntryLoader().getFiles();
	}
}
