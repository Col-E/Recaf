package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.workspace.resource.DexClassMap;
import me.coley.recaf.workspace.resource.Resource;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

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
				try (InputStream inputStream = new ByteArrayInputStream(content)) {
					DexBackedDexFile file = DexBackedDexFile.fromInputStream(opcodes, inputStream);
					DexClassMap map = resource.getDexClasses().getBackingMap()
							.computeIfAbsent(name, k -> new DexClassMap(resource, opcodes));
					for (DexBackedClassDef dexClass : file.getClasses()) {
						DexClassInfo clazz = DexClassInfo.parse(name, opcodes, dexClass);
						getListeners().forEach(l -> l.onDexClassEntry(clazz));
						map.put(clazz);
					}
				} catch (IOException ex) {
					logger.error("Failed parsing dex: " + name, ex);
				}
			} else {
				FileInfo file = new FileInfo(name, content);
				getListeners().forEach(l -> l.onFileEntry(file));
				resource.getFiles().initialPut(file);
			}
		});
		// Summarize what has been found
		logger.info("Read {} classes, {} files", resource.getDexClasses().size(), resource.getFiles().size());
	}
}
