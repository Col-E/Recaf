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
 * Importable directory resource.
 *
 * @author Matt
 */
public class DirectoryResource extends ArchiveResource {
	private static final String SEPARATOR = System.getProperty("file.separator");


	/**
	 * Constructs a directory resource.
	 *
	 * @param path
	 * 		Path reference to a directory.
	 *
	 * @throws IOException
	 * 		When the path does not exist.
	 */
	public DirectoryResource(Path path) throws IOException {
		super(ResourceKind.DIRECTORY, path);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		// iterate jar entries
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		EntryLoader loader = getEntryLoader();
		Path root = getPath();
		List<Path> classFilePaths = Files.walk(root)
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		String absolutePath = IOUtil.toString(root);
		for (Path path : classFilePaths) {
			File file = path.toFile();
			String relative = file.getAbsolutePath().substring(absolutePath.length() + 1)
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
		Path root = getPath();
		List<Path> classFilePaths = Files.walk(root)
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		String absolutePath = IOUtil.toString(root);
		for (Path path : classFilePaths) {
			File file = path.toFile();
			String relative = file.getAbsolutePath().substring(absolutePath.length() + 1)
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
		if(!Files.isDirectory(getPath()))
			throw new IOException("The directory \"" + getPath().getFileName() + "\" does not exist!");
	}
}
