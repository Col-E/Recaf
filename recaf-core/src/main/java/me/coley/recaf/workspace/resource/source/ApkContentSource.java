package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.workspace.resource.DexClassMap;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Origin location information for apks.
 *
 * @author Matt Coley
 */
public class ApkContentSource extends ArchiveFileContentSource {
	/**
	 * @param path
	 * 		Path to zip file.
	 */
	public ApkContentSource(Path path) {
		super(SourceType.APK, path);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		logger.info("Reading from file: {}", getPath());
		consumeEach((entry, content) -> {
			String name = getPathName(entry);
			if (name.endsWith(".dex")) {
				// TODO: Is there a way to determine what the correct API version is?
				Opcodes opcodes = Opcodes.getDefault();
				try (InputStream inputStream = new ByteArrayInputStream(content)){
					DexBackedDexFile file = DexBackedDexFile.fromInputStream(opcodes, inputStream);
					DexClassMap map = resource.getDexClasses().getBackingMap()
							.computeIfAbsent(name, k -> new DexClassMap(resource, opcodes));
					for (DexBackedClassDef dexClass : file.getClasses()) {
						DexClassInfo clazz = DexClassInfo.parse(dexClass);
						getListeners().forEach(l -> l.onDexClassEntry(clazz));
						map.put(clazz);
					}
				} catch (IOException ex) {
					logger.error("Failed parsing dex: " + name, ex);
				}
			} else if (name.endsWith(".arsc")) {
				// TODO: arsc resource extraction
				FileInfo file = new FileInfo(name, content);
				getListeners().forEach(l -> l.onFileEntry(file));
				resource.getFiles().initialPut(file);
			} else {
				FileInfo file = new FileInfo(name, content);
				getListeners().forEach(l -> l.onFileEntry(file));
				resource.getFiles().initialPut(file);
			}
		});
		// Summarize what has been found
		logger.info("Read {} classes, {} files", resource.getDexClasses().size(), resource.getFiles().size());
	}

	@Override
	public void onWrite(Resource resource, Path path) throws IOException {
		// Ensure parent directory exists
		Path parentDir = path.getParent();
		if (parentDir != null) {
			Files.createDirectories(parentDir);
		}
		// Collect content to put into export directory
		SortedMap<String, byte[]> outContent = new TreeMap<>();
		resource.getFiles().forEach((fileName, fileInfo) ->
				outContent.put(fileName, fileInfo.getValue()));
		for (Map.Entry<String, DexClassMap> entry : resource.getDexClasses().getBackingMap().entrySet()) {
			outContent.put(entry.getKey(), createDexFile(entry.getValue()));
		}
		// Log dirty classes
		Set<String> dirtyDexClasses = resource.getDexClasses().getDirtyItems();
		Set<String> dirtyFiles = resource.getFiles().getDirtyItems();
		logger.info("Attempting to write {} classes, {} files to: {}",
				resource.getClasses().size(), resource.getClasses().size(), path);
		logger.info("{}/{} classes have been modified, {}/{} files have been modified",
				resource.getClasses().size(), dirtyDexClasses.size(),
				resource.getClasses().size(), dirtyFiles.size());
		if (logger.isDebugEnabled()) {
			dirtyDexClasses.forEach(name -> logger.debug("Dirty class: " + name));
			dirtyFiles.forEach(name -> logger.debug("Dirty file: " + name));
		}
		// Write to file
		long startTime = System.currentTimeMillis();
		writeContent(path, outContent);
		logger.info("Write complete, took {}ms", System.currentTimeMillis() - startTime);
	}

	private byte[] createDexFile(DexClassMap dexMap) throws IOException {
		DexPool dexPool = new DexPool(dexMap.getOpcodes());
		for (ClassDef def : dexMap.getClasses()) {
			dexPool.internClass(def);
		}
		MemoryDataStore store = new MemoryDataStore();
		dexPool.writeTo(store);
		return store.getData();
	}
}
