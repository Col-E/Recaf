package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enigma mappings file implmentation.
 *
 * @author Matt
 */
public class EnigmaMappings extends Mappings {
	private static final String FAIL = "Invalid Enigma mappings, ";

	/**
	 * Constructs mappings from a given file.
	 *
	 * @param file
	 * 		A file containing enigma styled mappings.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	public EnigmaMappings(File file) throws IOException {
		super(file);
	}

	@Override
	protected Map<String, String> parse(String text) {
		Map<String, String> map = new HashMap<>();
		String[] lines = StringUtil.splitNewline(text);
		int line = 0;
		String currentClass = null;
		for(String lineStr : lines) {
			line++;
			String[] args = lineStr.trim().split(" ");
			String type = args[0];
			try {
				switch(type) {
					case "CLASS":
						currentClass = args[1];
						String renamedClass = args[2];
						map.put(currentClass, renamedClass);
						break;
					case "FIELD":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");
						String currentField = args[1];
						String renamedField = args[2];
						map.put(currentClass + "." + currentField, renamedField);
						break;
					case "METHOD":
						if (currentClass == null)
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");
						String currentMethod = args[1];
						String renamedMethod = args[2];
						String methodType = args[3];
						map.put(currentClass + "." + currentMethod + methodType, renamedMethod);
						break;
					case "ARG":
						// Do nothing, mapper does not support arg names
						break;
					default:
						Logger.trace("Unknown Engima mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch(IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
			}
		}
		return map;
	}
}
