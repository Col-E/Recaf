package me.coley.recaf.util;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.ClassHollowingVisitor;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.DexClassMap;
import me.coley.recaf.workspace.resource.MultiDexClassMap;
import me.coley.recaf.workspace.resource.Resource;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Workspace exporting utility.
 *
 * @author Matt Coley
 */
public class Exporter {
	private static final Logger logger = Logging.get(Exporter.class);
	private static final int MEGABYTE = 1048576;
	private final Path output;
	private final Map<String, byte[]> content = new TreeMap<>();
	private final Set<String> modifiedClasses = new TreeSet<>();
	private final Set<String> modifiedFiles = new TreeSet<>();
	public boolean compress = true;
	public boolean skipFiles;
	public boolean hollowClasses;
	public boolean shadeLibs;
	private long start;
	private long rawSize;

	/**
	 * @param output
	 * 		Base path to write to. See {@link #getOutput()} for details.
	 */
	public Exporter(Path output) {
		this.output = output;
	}

	/**
	 * The output path is the actual path in the case of using {@link #writeAsArchive()} or
	 * {@link #writeAsAPK()}. But for {@link #writeAsDirectory()} it is the root directory.
	 *
	 * @return Base path to write to.
	 */
	public Path getOutput() {
		return output;
	}

	/**
	 * Convenience call for {@link #addResource(Resource)}. Uses {@link #shadeLibs} to optionally include libs.
	 *
	 * @param workspace
	 * 		Workspace with resources to add to the output.
	 */
	public void addWorkspace(Workspace workspace) {
		if (shadeLibs)
			workspace.getResources().getLibraries().forEach(this::addResource);
		addResource(workspace.getResources().getPrimary());
	}

	/**
	 * Adds all content of the given resource to the output.
	 *
	 * @param resource
	 * 		Resource to add.
	 */
	public void addResource(Resource resource) {
		// TODO: Support for cases where class name is not the intended target path
		//   - war files with 'WEB-INF/classes/'

		// Add files
		if (!skipFiles) {
			resource.getFiles().forEach((key, info) -> {
				byte[] data = info.getValue();
				content.put(key, data);
				rawSize += data.length;
			});
		}
		// Add classes
		if (hollowClasses) {
			resource.getClasses().forEach((key, info) -> {
				byte[] data = info.getValue();
				content.put(key + ".class", hollow(data));
				rawSize += data.length;
			});
		} else {
			resource.getClasses().forEach((key, info) -> {
				byte[] data = info.getValue();
				content.put(key + ".class", data);
				rawSize += data.length;
			});
		}
		// Add dex classes
		MultiDexClassMap multiDex = resource.getDexClasses();
		if (!multiDex.isEmpty()) {
			for (Map.Entry<String, DexClassMap> entry : multiDex.getBackingMap().entrySet()) {
				String dexPath = entry.getKey();
				DexClassMap dex = entry.getValue();
				DexPool pool = new DexPool(dex.getOpcodes());
				for (ClassDef classDef : dex.getClasses()) {
					pool.internClass(classDef);
				}
				MemoryDataStore store = new MemoryDataStore();
				try {
					pool.writeTo(store);
				} catch (IOException ex) {
					logger.error("Failed writing workspace dex '{}' to byte[]", dexPath, ex);
					continue;
				}
				content.put(dexPath, store.getBuffer());
				rawSize += store.getSize();
			}
		}
		// Updated modified classes/files
		modifiedClasses.addAll(resource.getDexClasses().getDirtyItems());
		modifiedClasses.addAll(resource.getClasses().getDirtyItems());
		modifiedFiles.addAll(resource.getFiles().getDirtyItems());
	}

	/**
	 * Used to directly add classes to the output,
	 * with the assumption that they are all to be marked as being modified.
	 *
	 * @param classes
	 * 		Map of internal class names to their raw content.
	 */
	public void addRawClasses(Map<String, byte[]> classes) {
		if (hollowClasses) {
			classes.forEach((key, data) -> {
				content.put(key + ".class", hollow(data));
				rawSize += data.length;
			});
		} else {
			classes.forEach((key, data) -> {
				content.put(key + ".class", data);
				rawSize += data.length;
			});
		}
		modifiedClasses.addAll(classes.keySet());
	}

	/**
	 * Used to directly add files to the output,
	 * with the assumption that they are all to be marked as being modified.
	 *
	 * @param files
	 * 		Map of file names to their raw content.
	 */
	public void addRawFiles(Map<String, byte[]> files) {
		files.forEach((key, data) -> {
			content.put(key, data);
			rawSize += data.length;
		});
		modifiedFiles.addAll(files.keySet());
	}

	/**
	 * Writes to the output path as an Android APK.
	 *
	 * @throws IOException
	 * 		When the APK cannot be written to.
	 */
	public void writeAsAPK() throws IOException {
		// TODO: ZipAlign - https://developer.android.com/studio/command-line/zipalign
		// TODO: Signing  - https://developer.android.com/studio/command-line/apksigner
		//  - https://stackoverflow.com/questions/68855123/why-apk-could-not-be-installed-after-smali-patching
		writeAsArchive();
	}

	/**
	 * Writes to the output path as an archive.
	 *
	 * @throws IOException
	 * 		When the archive cannot be written to.
	 */
	public void writeAsArchive() throws IOException {
		// TODO: Update class info model to support prefixes and metadata
		//  - Some files may be prefixed
		//    - War:    "WEB-INF/classes"
		//    - Spring: "BOOT-INF/classes"
		//  - Zip entry metadata like comments should be transferred
		//    - Need to setup metadata tracking first
		//    - Each file/class entry should be capable of tracking info even with mappings applied
		preWrite();
		// Ensure parent directory exists
		Path parentDir = output.getParent();
		if (!Files.isDirectory(parentDir))
			Files.createDirectories(parentDir);
		// Determine stream type based on extension
		String extension = IOUtil.getExtension(output);
		// Use buffered streams, reduce overall file write operations
		OutputStream os = new BufferedOutputStream(Files.newOutputStream(output), MEGABYTE);
		try (ZipOutputStream jos = ("jar".equals(extension)) ? new JarOutputStream(os) : new ZipOutputStream(os)) {
			Set<String> dirsVisited = new HashSet<>();
			CRC32 crc = new CRC32();
			// Contents is iterated in sorted order (because 'archiveContent' is TreeMap).
			// This allows us to insert directory entries before file entries of that directory occur.
			for (Map.Entry<String, byte[]> entry : content.entrySet()) {
				String key = entry.getKey();
				byte[] out = entry.getValue();
				// Write directories for upcoming entries if necessary
				// - Ugly, but does the job.
				if (key.contains("/")) {
					// Record directories
					String parent = key;
					List<String> toAdd = new ArrayList<>();
					do {
						parent = parent.substring(0, parent.lastIndexOf('/'));
						if (dirsVisited.add(parent)) {
							toAdd.add(0, parent + '/');
						} else break;
					} while (parent.contains("/"));
					// Put directories in order of depth
					for (String dir : toAdd) {
						jos.putNextEntry(new JarEntry(dir));
						jos.closeEntry();
					}
				}
				// Write entry content
				crc.reset();
				crc.update(out, 0, out.length);
				JarEntry outEntry = new JarEntry(key);
				outEntry.setMethod(compress ? ZipEntry.DEFLATED : ZipEntry.STORED);
				if (!compress) {
					outEntry.setSize(out.length);
					outEntry.setCompressedSize(out.length);
				}
				outEntry.setCrc(crc.getValue());
				jos.putNextEntry(outEntry);
				jos.write(out);
				jos.closeEntry();
			}
		}
		postWrite();
	}

	/**
	 * Writes to the output path as a directory structure.
	 *
	 * @throws IOException
	 * 		When the directory cannot be written to.
	 */
	public void writeAsDirectory() throws IOException {
		preWrite();
		// Ensure parent directory exists
		Path parentDir = output.getParent();
		if (!Files.isDirectory(parentDir))
			Files.createDirectories(parentDir);
		// Write all entries
		for (Map.Entry<String, byte[]> entry : content.entrySet()) {
			String name = entry.getKey();
			byte[] out = entry.getValue();
			Path path = output.resolve(name);
			Files.createDirectories(path.getParent());
			Files.write(path, out);
		}
		postWrite();
	}

	/**
	 * Writes to the output path as a single <i>(class)</i> file.
	 *
	 * @throws IOException
	 * 		When the file cannot be written to.
	 */
	public void writeAsSingleFile() throws IOException {
		preWrite();
		// Ensure parent directory exists
		Path parentDir = output.getParent();
		if (!Files.isDirectory(parentDir))
			Files.createDirectories(parentDir);
		// Write all entries
		if (content.size() > 1) {
			logger.error("Tried to export to class '{}' but more than 1 file is recorded for exporting!", output);
			return;
		}
		for (Map.Entry<String, byte[]> entry : content.entrySet()) {
			byte[] out = entry.getValue();
			Files.write(output, out);
			break;
		}
		postWrite();
	}

	private void preWrite() {
		start = System.currentTimeMillis();
		logger.info("Writing to {}.\n - Modified classes: {}\n - Modified files: {}",
				output.getFileName(), modifiedClasses.size(), modifiedFiles.size());
	}

	private void postWrite() throws IOException {
		long now = System.currentTimeMillis();
		long actualSize;
		if (Files.isDirectory(output)) {
			try (Stream<Path> walk = Files.walk(output)) {
				actualSize = walk
						.filter(Files::isRegularFile)
						.mapToLong(path -> {
							try {
								return Files.size(path);
							} catch (IOException e) {
								logger.debug("Failed to get size of {}", path, e);
								return 0L;
							}
						})
						.sum();

			}
		} else {
			actualSize = output.toFile().length();
		}
		String compressionRatio = String.format("%.2f", ((rawSize - actualSize) / (double) rawSize) * 100);
		logger.info("Written to {} in {}ms. Compression ratio: {}%",
				output.getFileName(), now - start, compressionRatio);
	}

	private static byte[] hollow(byte[] value) {
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor hollower = new ClassHollowingVisitor(cw);
		new ClassReader(value).accept(hollower, ClassReader.SKIP_FRAMES);
		return cw.toByteArray();
	}
}
