package me.coley.recaf.mapping;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Base for mapppings.
 *
 * @author Matt
 */
public abstract class Mappings {
	/**
	 * ASM formatted mappings.
	 */
	private Map<String, String> mappings;

	/**
	 * @param file
	 * 		Text file containing mappings.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	public Mappings(File file) throws IOException {
		read(file);
	}

	/**
	 * @param file
	 * 		Text file containing mappings.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	private void read(File file) throws IOException {
		String text = FileUtils.readFileToString(file, "UTF-8");
		mappings = parse(text);
	}

	/**
	 * See the
	 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)} docs for more
	 * information.
	 *
	 * @return ASM formatted mappings.
	 */
	public Map<String, String> getMappings() {
		return mappings;
	}

	/**
	 * Parses the mappings into the standard ASM format. See the
	 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)} docs for more
	 * information.
	 *
	 * @param text
	 * 		Text of the mappings.
	 *
	 * @return ASM formatted mappings.
	 */
	protected abstract Map<String, String> parse(String text);

	/**
	 * @param clazz
	 * 		Class bytecode.
	 */
	public byte[] accept(byte[] clazz) {
		SimpleRecordingRemapper mapper = new SimpleRecordingRemapper(getMappings());
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassRemapper adapter = new ClassRemapper(cw, mapper);
		new ClassReader(clazz).accept(adapter, ClassReader.SKIP_FRAMES);
		// Only return the modified class if any references to the mappings were found.
		if (mapper.isDirty())
			return cw.toByteArray();
		return clazz;
	}
}
