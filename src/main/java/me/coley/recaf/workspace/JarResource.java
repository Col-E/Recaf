package me.coley.recaf.workspace;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Importable jar resource.
 *
 * @author Matt
 */
public class JarResource extends FileSystemResource {
	private JarEntryLoader entryLoader = new JarEntryLoader();

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
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are classes and valid resource items
				ZipEntry entry = entries.nextElement();
				if (doSkip(entry))
					continue;
				if(!entryLoader.isValidClass(entry))
					continue;
				if(!entryLoader.isValidResource(entry))
					continue;

				InputStream stream = zipFile.getInputStream(entry);
				// minimally parse for the name
				byte[] in = IOUtils.toByteArray(stream);
				entryLoader.onClass(entry.getName(), in);
			}
		}
		entryLoader.finishClasses();
		return entryLoader.getClasses();
	}

	@Override
	protected Map<String, byte[]> loadResources() throws IOException {
		// iterate jar entries
		try (ZipFile zipFile = new ZipFile(getFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				// verify entries are not classes and are valid resource items
				ZipEntry entry = entries.nextElement();
				if (doSkip(entry))
					continue;
				if(entryLoader.isValidClass(entry))
					continue;
				if(!entryLoader.isValidResource(entry))
					continue;
				InputStream stream = zipFile.getInputStream(entry);
				byte[] in = IOUtils.toByteArray(stream);
				entryLoader.onResource(entry.getName(), in);
			}
		}
		entryLoader.finishResources();
		return entryLoader.getResources();
	}

	/**
	 * @return Loader used to read content from jar files.
	 */
	public JarEntryLoader getEntryLoader() {
		return entryLoader;
	}

	/**
	 * Set the jar entry loader. Custom entry loaders could allow handling of some non-standard
	 * inputs such as obfuscated or packed jars.
	 *
	 * @param entryLoader
	 * 		Loader used to read content from jar files.
	 */
	public void setEntryLoader(JarEntryLoader entryLoader) {
		this.entryLoader = entryLoader;
	}

	private boolean doSkip(ZipEntry entry) {
		String name = entry.getName();
		for (String prefix : getSkippedPrefixes())
			if (name.startsWith(prefix))
				return true;
		return false;
	}
}
