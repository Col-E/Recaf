package software.coley.recaf.util.android;

import com.android.tools.r8.graph.DexProgramClass;
import jakarta.annotation.Nonnull;
import software.coley.dextranslator.model.ApplicationData;
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
		// Read dex file content
		ApplicationData data = ApplicationData.fromDex(dex);

		// Populate bundle
		BasicAndroidClassBundle classBundle = new BasicAndroidClassBundle();
		for (DexProgramClass dexClass : data.getApplication().classes()) {
			AndroidClassInfo classInfo = new AndroidClassInfoBuilder()
					.adaptFrom(dexClass)
					.build();
			classBundle.initialPut(classInfo);
		}
		return classBundle;
	}
}
