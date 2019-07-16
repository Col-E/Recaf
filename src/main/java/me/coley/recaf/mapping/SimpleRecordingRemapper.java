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
		// Check if the key is an inner class
		if (!key.contains(".")) {
			// key is not a method/field
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
		String mapped = super.map(key);
		if(mapped != null)
			dirty = true;
		return mapped;
	}
}
