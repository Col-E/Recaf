package me.coley.recaf.workspace.resource.source;

import me.coley.cafedude.classfile.VersionConstants;
import me.coley.recaf.util.ByteHeaderUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.DataInputStream;
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
	protected static boolean matchesClass(byte[] content) {
		// Null and size check
		// The smallest valid class possible that is verifiable is 37 bytes AFAIK, but we'll be generous here.
		if (content == null ||	content.length <= 16 )
			return false;
		// We want to make sure the 'magic' is correct.
		if (!ByteHeaderUtil.match(content, ByteHeaderUtil.CLASS))
			return false;
		// 'dylib' files can also have CAFEBABE as a magic header... Gee, thanks Apple :/
		// Because of this we'll employ some more sanity checks.
		// Version number must be non-zero
		int version = (content[8] << 8) + content[9];
		if (version < VersionConstants.JAVA1)
			return false;
		// Must include some constant pool entries.
		// The smallest number includes:
		//  - utf8  - name of current class
		//  - class - wrapper of prior
		//  - utf8  - name of object class
		//  - class - wrapper of prior
		int cpSize = (content[10] << 8) + content[11];
		return cpSize >= 4;
	}

	@Override
	public String toString() {
		return path.getFileName().toString();
	}
}
