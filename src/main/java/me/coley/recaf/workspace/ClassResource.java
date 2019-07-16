package me.coley.recaf.workspace;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.util.*;

/**
 * Importable class resource.
 *
 * @author Matt
 */
public class ClassResource extends FileSystemResource {
	public ClassResource(File file) {
		super(ResourceKind.CLASS, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		Map<String, byte[]> classes = new HashMap<>();
		try {
			// read & minimally parse for the name
			byte[] in = IOUtils.toByteArray(new FileInputStream(getFile()));
			String name = new ClassReader(in).getClassName();
			classes.put(name, in);
		} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			Logger.error("Invalid class \"{}\"", getFile().getName());
		}
		return classes;
	}

	@Override
	protected Map<String, byte[]> loadResources() {
		return Collections.emptyMap();
	}
}
