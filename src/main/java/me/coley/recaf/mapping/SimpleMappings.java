package me.coley.recaf.mapping;

import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.util.StringUtil.splitNewline;
import static me.coley.recaf.util.EscapeUtil.*;

/**
 * Simple mappings file implementation where the old/new names are split by a space.
 * The input format of the mappings is based on the format outlined by
 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
 * <br>
 * Differences include:
 * <ul>
 *     <li>Support for {@code #comment} lines</li>
 *     <li>Support for unicode escape sequences ({@code \\uXXXX})</li>
 *     <li>Support for fields specified by their name <i>and descriptor</i></li>
 * </ul>
 *
 * @author Matt
 */
public class SimpleMappings extends FileMappings {
	/**
	 * Constructs mappings from a given file.
	 *
	 * @param path
	 * 		A path to a file containing asm styled mappings.
	 * 		See {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	SimpleMappings(Path path, Workspace workspace) throws IOException {
		super(path, workspace);
	}

	@Override
	protected Map<String, String> parse(String text) {
		String[] lines = splitNewline(text);
		Map<String, String> map = new HashMap<>(lines.length);
		// # Comment
		// BaseClass TargetClass
		// BaseClass.baseField targetField
		// BaseClass.baseField baseDesc targetField
		// BaseClass.baseMethod(BaseMethodDesc) targetMethod
		for (String line : lines) {
			// Skip comments and empty lines
			if (line.trim().startsWith("#") || line.trim().isEmpty())
				continue;
			String[] args = line.split(" ");
			String baseName = unescape(args[0]);
			if (args.length > 2) {
				// Descriptor qualified field format
				String baseDesc = unescape(args[1]);
				String targetName = unescape(args[2]);
				map.put(baseName + " " + baseDesc, targetName);
			} else {
				// Any other format
				String targetName = unescape(args[1]);
				map.put(baseName, targetName);
			}
		}
		return map;
	}
}
