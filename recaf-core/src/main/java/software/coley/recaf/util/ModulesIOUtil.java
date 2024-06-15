package software.coley.recaf.util;

import software.coley.collections.Unchecked;
import software.coley.recaf.util.io.ByteSourceElement;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Java module reading.
 *
 * @author xDark
 */
public class ModulesIOUtil {
	private static final MethodHandles.Lookup lookup = LookupUtil.lookup();

	/**
	 * @param modulesFilePath
	 * 		Path to {@code modules} file.
	 *
	 * @return Stream of contents within the file.
	 *
	 * @throws IOException
	 * 		When the modules file cannot be read from.
	 */
	@SuppressWarnings("Convert2MethodRef") // javac doesn't like reference on 'bmap(...)'
	public static Stream<ByteSourceElement<Entry>> stream(Path modulesFilePath) throws IOException {
		return Unchecked.map(path -> {
			// Cannot directly use internals... ugh.
			Class<?> imageReaderClass = Class.forName("jdk.internal.jimage.ImageReader", true, null);
			Method m = imageReaderClass.getDeclaredMethod("open", Path.class);
			m.setAccessible(true);
			Object reader = m.invoke(null, path);
			m = imageReaderClass.getDeclaredMethod("getEntryNames");
			m.setAccessible(true);
			String[] entries = (String[]) m.invoke(reader);
			Class<?> imageLocationClass = Class.forName("jdk.internal.jimage.ImageLocation", true, null);
			MethodHandle getResourceBuffer = lookup.findVirtual(imageReaderClass, "getResourceBuffer", MethodType.methodType(ByteBuffer.class, imageLocationClass))
					.bindTo(reader);
			MethodHandle findLocation = lookup.findVirtual(imageReaderClass, "findLocation", MethodType.methodType(imageLocationClass, String.class))
					.bindTo(reader);
			return Arrays.stream(entries)
					.filter(entryName -> entryName.indexOf('/', 1) > 1)
					.map(entryName -> {
						// Follows the pattern: /<module-name>/<file-name>
						int firstSlash = entryName.indexOf('/', 1);
						String moduleName = entryName.substring(1, firstSlash);
						String fileName = entryName.substring(entryName.indexOf('/', 1) + 1);

						// Get content source
						Object imageLocation = Unchecked.bmap((t, u) -> t.invoke(u), findLocation, entryName);
						ByteBuffer buffer = Unchecked.bmap((t, u) -> (ByteBuffer) t.invoke(u), getResourceBuffer, imageLocation);

						// Wrap into element
						return new ByteSourceElement<>(new Entry(moduleName, fileName), ByteSources.forBuffer(buffer));
					}).onClose(() -> IOUtil.closeQuietly((AutoCloseable) reader));
		}, modulesFilePath);
	}

	public static class Entry {
		private final String moduleName;
		private final String fileName;

		private Entry(String moduleName, String fileName) {
			this.moduleName = moduleName;
			this.fileName = fileName;
		}

		/**
		 * @return The original full path from the {@code modules} file,
		 * of which the module name and file path are derived from.
		 */
		public String getOriginalPath() {
			return '/' + moduleName + '/' + fileName;
		}

		/**
		 * @return The file's associated module.
		 */
		public String getModuleName() {
			return moduleName;
		}

		/**
		 * @return The file's path name.
		 */
		public String getFileName() {
			return fileName;
		}
	}
}
