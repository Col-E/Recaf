package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Importable jar resource.
 *
 * @author Matt
 */
public class DirectoryResource extends ArchiveResource {
	private static final String SEPARATOR = System.getProperty("file.separator");

	/**
	 * Constructs a jar resource.
	 *
	 * @param file
	 * 		File reference to a jar file.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public DirectoryResource(File file) throws IOException {
		super(ResourceKind.DIRECTORY, file);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		List<Path> classFilePaths = Files.walk(getFile().toPath())
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		File root = getFile();
		for (Path path : classFilePaths) {
			File file = path.toFile();
			String relative = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)
					.replace(SEPARATOR, "/");
			if (shouldSkip(relative))
				continue;
			if(!loader.isFileValidClassName(relative))
				continue;
			out.reset();
			byte[] in = IOUtil.toByteArray(new FileInputStream(file), out, buffer);
			loader.onClass(relative, in);
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
		List<Path> classFilePaths = Files.walk(getFile().toPath())
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		File root = getFile();
		for (Path path : classFilePaths) {
			File file = path.toFile();
			String relative = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)
					.replace(SEPARATOR, "/");
			if (shouldSkip(relative))
				continue;
			if(loader.isFileValidClassName(relative))
				continue;
			out.reset();
			byte[] in = IOUtil.toByteArray(new FileInputStream(file), out, buffer);
			loader.onFile(relative, in);
		}
		loader.finishFiles();
		return loader.getFiles();
	}

	@Override
	protected void verify() throws IOException {
		if(!getFile().isDirectory())
			throw new IOException("The directory \"" + getFile().getName() + "\" does not exist!");
	}
}
