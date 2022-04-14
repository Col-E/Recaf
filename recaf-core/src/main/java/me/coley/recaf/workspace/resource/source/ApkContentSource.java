package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.logging.Logging;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Origin location information for apks.
 *
 * @author Matt Coley
 */
public class ApkContentSource extends ArchiveFileContentSource {
	private static final Logger logger = Logging.get(ApkContentSource.class);

	/**
	 * @param path
	 * 		Path to zip file.
	 */
	public ApkContentSource(Path path) {
		super(SourceType.APK, path);
	}

	@Override
	protected void onRead(ContentCollection collection) throws IOException {
		logger.info("Reading from file: {}", getPath());
		consumeEach((entry, content) -> {
			String name = getPathName(entry);
			if (name.endsWith(".dex")) {
				// TODO: Is there a way to determine what the correct API version is?
				Opcodes opcodes = Opcodes.getDefault();
				try {
					DexBackedDexFile file = DexBackedDexFile.fromInputStream(opcodes, new ByteArrayInputStream(content.readAll()));
					collection.addDexClasses(name, file);
				} catch (IOException ex) {
					logger.error("Failed parsing dex: " + name, ex);
				}
			} else {
				FileInfo file = new FileInfo(name, content.readAll());
				collection.addFile(file);
			}
		});
		// Summarize what has been found
		logger.info("Read {} classes, {} files", collection.getDexClassCount(), collection.getFileCount());
	}
}
