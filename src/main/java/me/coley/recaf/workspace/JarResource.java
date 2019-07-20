package me.coley.recaf.workspace;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.pmw.tinylog.Logger;

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
	/**
	 * Constructs a jar resource.
	 *
	 * @param file
	 * 		File reference to a jar file.
	 */
	public JarResource(File file) {
		super(ResourceKind.JAR, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		Map<String, byte[]> classes = new HashMap<>();
		// iterate jar entries
		ZipFile zipFile = new ZipFile(getFile());
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while(entries.hasMoreElements()) {
			// simple verification to ensure non-classes are not loaded
			ZipEntry entry = entries.nextElement();
			if(!isValidClass(entry))
				continue;
			InputStream stream = zipFile.getInputStream(entry);
			// minimally parse for the name
			byte[] in = IOUtils.toByteArray(stream);
			try {
				String name = new ClassReader(in).getClassName();
				classes.put(name, in);
			} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				Logger.error("Invalid class in \"{}\" - \"{}\"", getFile().getName(), entry.getName());
			}
			stream.close();
		}
		return classes;
	}

	@Override
	protected Map<String, byte[]> loadResources() throws IOException {
		Map<String, byte[]> resources = new HashMap<>();
		// read & minimally parse for the name
		ZipFile zipFile = new ZipFile(getFile());
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while(entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if(!isValidResource(entry))
				continue;
			String name = entry.getName();
			InputStream stream = zipFile.getInputStream(entry);
			byte[] in = IOUtils.toByteArray(stream);
			resources.put(name, in);
			stream.close();
		}
		return resources;
	}

	private boolean isValidClass(ZipEntry entry) {
		if(!isValidResource(entry))
			return false;
		// Must end in class
		String name = entry.getName();
		return name.endsWith(".class");
	}

	private boolean isValidResource(ZipEntry entry) {
		if (entry.isDirectory())
			return false;
		String name = entry.getName();
		// name / directory escaping
		if (name.contains("../"))
			return false;
		// empty directory names is a no
		if (name.contains("//"))
			return false;
		// skip specified prefixes
		for (String prefix : getSkippedPrefixes())
			if (name.startsWith(prefix))
				return false;
		return true;
	}
}
