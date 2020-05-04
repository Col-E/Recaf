package me.coley.recaf.workspace;

import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.util.IOUtil;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public ClassResource(File file) throws IOException {
		super(ResourceKind.CLASS, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		EntryLoader loader = getEntryLoader();
		try (FileInputStream stream = new FileInputStream(getFile())) {
			byte[] value = IOUtil.toByteArray(stream);
			loader.onClass(getFile().getName(), value);
			loader.finishClasses();
			return loader.getClasses();
		} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			throw new IOException("Failed to load class '" + getFile().getName() + "'", ex);
		}
	}

	@Override
	protected Map<String, byte[]> loadFiles() {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, SourceCode> loadSources(File file) throws IOException {
		if (file.getName().endsWith(".java")) {
			try {
				SourceCode code = new SourceCode(this, FileUtils.readFileToString(file, StandardCharsets.UTF_8));
				code.analyze();
				return Collections.singletonMap(code.getInternalName(), code);
			} catch(IOException ex) {
				throw new IOException("Failed to read from source file: " + file, ex);
			} catch(SourceCodeException ex) {
				throw new IOException("Invalid source code file: " + file, ex);
			}
		}
		return super.loadSources(file);
	}
}
