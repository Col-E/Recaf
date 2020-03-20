package me.coley.recaf.mapping;

import me.coley.recaf.workspace.Workspace;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Extended base for mappings that load mappings from a given file.
 * Implementations will create file-loading logic for different mapping types.
 *
 * @author Matt
 */
public abstract class FileMappings extends Mappings {
	/**
	 * @param file
	 * 		Text file containing mappings.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	public FileMappings(File file, Workspace workspace) throws IOException {
		super(workspace);
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
		setMappings(parse(text));
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
}
