package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.ValidationVisitor;
import org.objectweb.asm.ClassReader;

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
	 * Allow modification of the output class name.
	 *
	 * @param className
	 * 		Original class name.
	 *
	 * @return Path in archive output.
	 */
	protected String filterOutputClassName(String className) {
		return className;
	}

	/**
	 * Allow modification of the input class name when it can't be found normally.
	 *
	 * @param className
	 * 		Original class name.
	 *
	 * @return Path to use in resource.
	 */
	protected String filterInputClassName(String className) {
		return className;
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
			new ClassReader(content).accept(new ValidationVisitor(), 0);
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
