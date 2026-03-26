package software.coley.recaf.util.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.file.DexHeader;
import me.darknet.dex.file.DexMap;
import me.darknet.dex.io.Input;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;

import java.io.IOException;

/**
 * Dex file reading and writing.
 *
 * @author Matt Coley
 */
public class DexIOUtil {
	/**
	 * @param source
	 * 		Content source to read from. Must be wrapping a dex file.
	 *
	 * @return Bundle of classes from the dex file.
	 *
	 * @throws IOException
	 * 		When the dex file cannot be read from.
	 */
	@Nonnull
	public static AndroidClassBundle read(@Nonnull ByteSource source) throws IOException {
		return read(source.readAll());
	}

	/**
	 * @param dex
	 * 		Raw bytes of a dex file.
	 *
	 * @return Bundle of classes from the dex file.
	 *
	 * @throws IOException
	 * 		When the dex file cannot be read from.
	 */
	@Nonnull
	public static AndroidClassBundle read(@Nonnull byte[] dex) throws IOException {
		DexHeader header = DexHeader.CODEC.read(Input.wrap(dex));
		DexMap map = header.map();
		DexFile dexFile = DexFile.CODEC.map(header, map);

		// Populate bundle
		BasicAndroidClassBundle classBundle = new BasicAndroidClassBundle(header.version(), header.link());
		for (ClassDefinition dexClass : dexFile.definitions()) {
			AndroidClassInfo classInfo = new AndroidClassInfoBuilder()
					.adaptFrom(dexClass)
					.build();
			classBundle.initialPut(classInfo);
		}
		return classBundle;
	}
}
