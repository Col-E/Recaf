package me.coley.recaf.workspace;

import org.apache.commons.io.FileUtils;
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
	/**
	 * Constructs a class resource.
	 *
	 * @param file
	 * 		File reference to a class file.
	 */
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

	@Override
	protected Map<String, SourceCode> loadSources(File file) throws IOException {
		try {
			SourceCode code = new SourceCode(this, FileUtils.readFileToString(file, "UTF-8"));
			code.analyze();
			return Collections.singletonMap(code.getInternalName(), code);
		} catch(IOException ex) {
			throw new IOException("Failed to read from source file: " + file, ex);
		} catch (SourceCodeException ex) {
			throw new IOException("Invalid source code file: " + file, ex);
		}
	}
}
