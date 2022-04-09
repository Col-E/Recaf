package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.ByteHeaderUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.nio.file.Path;

/**
 * Origin location information of files.
 *
 * @author Matt Coley
 */
public abstract class FileContentSource extends ContentSource {
	private final Path path;

	protected FileContentSource(SourceType type, Path path) {
		super(type);
		this.path = path;
	}

	/**
	 * @return Path to file source.
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * Check if the class can be parsed by ASM.
	 *
	 * @param content
	 * 		The class file content.
	 *
	 * @return {@code true} if ASM can parse the class.
	 */
	protected static boolean isParsableClass(byte[] content) {
		try {
			new ClassReader(content).accept(new ClassWriter(0), 0);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check if the byte array is prefixed by the class file magic header.
	 *
	 * @param content
	 * 		File content.
	 *
	 * @return If the content seems to be a class at a first glance.
	 */
	protected static boolean matchesClassMagic(byte[] content) {
		return content != null && content.length > 16 && ByteHeaderUtil.match(content, ByteHeaderUtil.CLASS);
	}

	@Override
	public String toString() {
		return path.getFileName().toString();
	}
}
