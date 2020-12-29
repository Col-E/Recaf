package me.coley.recaf.workspace.resource.source;

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
			return new ClassReader(content).getClassName() != null;
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
		if (content == null || content.length < 16) {
			return false;
		}
		return content[0] == (byte) 0xCA &&
				content[1] == (byte) 0xFE &&
				content[2] == (byte) 0xBA &&
				content[3] == (byte) 0xBE;
	}
}
