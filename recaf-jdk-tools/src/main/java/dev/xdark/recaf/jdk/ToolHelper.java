package dev.xdark.recaf.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tool helper.
 *
 * @author xDark
 */
public class ToolHelper {

	/**
	 * Packs a jar with tool classes.
	 *
	 * @param path
	 * 		Path to jar file.
	 * @param classes
	 * 		Classes to pack.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static void prepareJar(Path path, Collection<Class<?>> classes) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(path))) {
			for (Class<?> tool : classes) {
				String className = tool.getName().replace('.', '/') + ".class";
				zos.putNextEntry(new ZipEntry(className));
				try (InputStream in = tool.getClassLoader().getResourceAsStream(className)) {
					byte[] buf = new byte[1024];
					int r;
					while ((r = in.read(buf)) >= 0) {
						zos.write(buf, 0, r);
					}
				}
			}
		}
	}

	/**
	 * Packs a jar with tool classes.
	 *
	 * @param path
	 * 		Path to jar file.
	 * @param tool
	 * 		Tool class.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static void prepareJar(Path path, Class<?> tool) throws IOException {
		prepareJar(path, Collections.singletonList(tool));
	}
}
