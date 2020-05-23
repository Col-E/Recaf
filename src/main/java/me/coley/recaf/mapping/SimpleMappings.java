package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple mappings file implementation where the old/new names are split by a space. The format of
 * the mappings matches the format outlined by
 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
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
		String[] lines = StringUtil.splitNewline(text);
		Map<String, String> map = new HashMap<>(lines.length);
		for (String line : lines) {
			// Skip comments and empty lines
			if (line.startsWith("#") || line.trim().isEmpty())
				continue;
			String[] args = line.split(" ");
			if (args.length > 2) {
				// Descriptor qualified field format
				map.put(args[0] + " " + args[1], args[2]);
			} else {
				// Any other format
				map.put(args[0], args[1]);
			}
		}
		return map;
	}
}
