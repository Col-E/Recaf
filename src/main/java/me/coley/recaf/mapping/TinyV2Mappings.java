package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.util.Log.trace;

/**
 * Tiny-V2 mappings file implementation.
 *
 * @author Matt
 */
public class TinyV2Mappings extends FileMappings {
	private static final String FAIL = "Invalid Tiny-V2 mappings, ";

	/**
	 * Constructs mappings from a given file.
	 *
	 * @param path
	 * 		A path to a file containing Tiny-V2 styled mappings.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	TinyV2Mappings(Path path, Workspace workspace) throws IOException {
		super(path, workspace);
	}

	@Override
	protected Map<String, String> parse(String text) {
		Map<String, String> map = new HashMap<>();
		String[] lines = StringUtil.splitNewline(text);
		int line = 0;
		String currentClass = null;
		for(String lineStr : lines) {
			line++;
			// Skip initial header
			if (lineStr.startsWith("tiny\t"))
				continue;
			String[] args = lineStr.trim().split("\t");
			String type = args[0];
			try {
				switch(type) {
					case "c":
						currentClass = args[1];
						String renamedClass = args[2];
						map.put(currentClass, renamedClass);
						break;
					case "f":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");
						String currentField = args[2];
						String renamedField = args[3];
						map.put(currentClass + "." + currentField, renamedField);
						break;
					case "m":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");
						String methodType = args[1];
						String currentMethod = args[2];
						String renamedMethod = args[3];
						map.put(currentClass + "." + currentMethod + methodType, renamedMethod);
						break;
					default:
						trace("Unknown Tiny-V2 mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch(IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
			}
		}
		return map;
	}
}
