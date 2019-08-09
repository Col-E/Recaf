package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple mappings file implmentation where the old/new names are split by a space. The format of
 * the mappings matches the format outlined by
 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
 *
 * @author Matt
 */
public class SimpleMappings extends Mappings {
	/**
	 * Constructs mappings from a given file.
	 *
	 * @param file
	 * 		A file containing asm styled mappings.
	 * 		See {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be read.
	 */
	public SimpleMappings(File file) throws IOException {
		super(file);
	}

	@Override
	protected Map<String, String> parse(String text) {
		String[] lines = StringUtil.splitNewline(text);
		Map<String, String> map = new HashMap<>(lines.length);
		for(String line : lines) {
			String[] args = line.split(" ");
			map.put(args[0], args[1]);
		}
		return map;
	}
}
