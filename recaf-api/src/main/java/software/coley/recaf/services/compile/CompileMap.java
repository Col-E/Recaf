package software.coley.recaf.services.compile;

import java.util.Map;
import java.util.TreeMap;

/**
 * Wrapper of a string-to-bytecode map with additional utility methods.
 *
 * @author Matt Coley
 */
public class CompileMap extends TreeMap<String, byte[]> {
	/**
	 * @param map
	 * 		Map to copy.
	 */
	public CompileMap(Map<String, byte[]> map) {
		super(map);
	}

	/**
	 * @return {@code true} when multiple classes are in the map.
	 */
	public boolean hasMultipleClasses() {
		return size() > 1;
	}

	/**
	 * @return {@code true} when multiple classes are in the map,
	 * and one of them is an inner class of one of the others.
	 */
	public boolean hasInnerClasses() {
		if (hasMultipleClasses()) {
			for (String name : keySet()) {
				// Name contains the inner class separator "$" and the outer-class is also in the map
				if (name.contains("$") && containsKey(name.substring(0, name.lastIndexOf("$")))) {
					return true;
				}
			}
		}
		return false;
	}
}
