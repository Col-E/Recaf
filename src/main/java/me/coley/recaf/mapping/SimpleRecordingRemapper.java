package me.coley.recaf.mapping;

import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

/**
 * An extension of the SimpleRemapper that logs if a class has been modified in the renaming
 * process.
 *
 * @author Matt
 */
public class SimpleRecordingRemapper extends SimpleRemapper {
	private boolean dirty;

	/**
	 * Constructs a recording remapper.
	 *
	 * @param mapping
	 * 		Map of asm styled mappings. See
	 * 		{@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}.
	 */
	public SimpleRecordingRemapper(Map<String, String> mapping) {
		super(mapping);
	}

	/**
	 * If a class contains no references to anything in the mappings there will be no reason to
	 * update it within Recaf, so we record if any changes were made. If no changes are made we
	 * can disregard the remapped output.
	 *
	 * @return {@code} true if the class has been modified in the remapping process.
	 */
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public String map(final String key) {
		// Get mapped value from key
		String mapped = super.map(key);
		// Check if the key is null and does not contain a splitter (.)
		// - the splitter would indicate the key represents a field/method
		if (mapped == null && !key.contains(".")) {
			// No mapping for this inner class, at least ensure the quantified outer name is mapped
			int index = key.lastIndexOf("$");
			if(index > 1) {
				// key is an inner class
				String outer = key.substring(0, index);
				String inner = key.substring(index);
				String mappedOuter = map(outer);
				if(mappedOuter != null)
					return mappedOuter + inner;
			}
		}
		// Mark as dirty if mappings found
		if(mapped != null)
			dirty = true;
		return mapped;
	}
}
