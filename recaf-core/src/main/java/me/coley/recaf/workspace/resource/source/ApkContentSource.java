package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.DexClassInfo;
import me.coley.recaf.workspace.resource.DexClassMap;
import me.coley.recaf.workspace.resource.FileInfo;
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
				// TODO: Determine what the correct API version is
				Opcodes opcodes = Opcodes.forApi(29);
				try (InputStream inputStream = new ByteArrayInputStream(content)){
					DexBackedDexFile file = DexBackedDexFile.fromInputStream(opcodes, inputStream);
					DexClassMap map = resource.getDexClasses().getBackingMap()
							.computeIfAbsent(name, k -> new DexClassMap(resource));
					for (DexBackedClassDef dexClass : file.getClasses()) {
						map.put(DexClassInfo.parse(dexClass));
					}
				} catch (IOException ex) {
					logger.error("Failed parsing dex: " + name, ex);
				}
			} else if (name.endsWith(".arsc")) {
				// TODO: arsc resource extraction
				resource.getFiles().initialPut(new FileInfo(name, content));
			} else {
				// TODO Plugins: Read intercept plugin support?
				resource.getFiles().initialPut(new FileInfo(name, content));
			}
		});
		// Summarize what has been found
		logger.info("Read {} classes, {} files", resource.getDexClasses().size(), resource.getFiles().size());
	}
}
