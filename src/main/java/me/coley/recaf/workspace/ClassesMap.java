package me.coley.recaf.workspace;

import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.ClassUtil;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Map for the classes. Values in this map are "cached" representations of what is stored in the
 * raw backing map.
 *
 * @author Matt
 */
public class ClassesMap extends HashMap<String, ClassNode> {
	private final Map<String, byte[]> raw = new HashMap<>();

	/**
	 * @param content
	 * 		Map of class names to their raw bytecode.
	 */
	public void putAllRaw(Map<String, byte[]> content) {
		content.entrySet().forEach(e -> putRaw(e.getKey(), e.getValue()));
	}

	/**
	 * @param key
	 * 		Class name.
	 * @param classFile
	 * 		Raw class bytecode.
	 */
	public void putRaw(String key, byte[] classFile) {
		raw.put(key, classFile);
	}

	/**
	 * @param key
	 * 		Class name.
	 *
	 * @return Raw class bytecode.
	 */
	public byte[] getRaw(String key) {
		return raw.get(key);
	}

	/**
	 * @param key
	 * 		Class name.
	 *
	 * @return Removed raw class bytecode.
	 */
	public byte[] removeRaw(String key) {
		return raw.remove(key);
	}

	/**
	 * This is called to remove the most recent "ClassNode" instance.
	 *
	 * @param key
	 * 		Class name.
	 *
	 * @return Removed instance.
	 */
	@Override
	public ClassNode remove(Object key) {
		return super.remove(key);
	}

	/**
	 * When a new class instance is put into the map, the raw backing map should be updated too.
	 *
	 * @param key
	 * 		Class name.
	 * @param value
	 * 		ClassNode instance.
	 *
	 * @return Previous associated value, if any.
	 */
	@Override
	public ClassNode put(String key, ClassNode value) {
		try {
			byte[] classFile = ClassUtil.getBytes(value);
			raw.put(key, classFile);
		} catch(Exception e) {
			Logging.warn("Failed to convert to raw byte[]: '" + value.name + "' due to the " +
					"following error: ");
			Logging.error(e);
		}
		return super.put(key, value);
	}

	/**
	 * Overriden so fetched values can be cached for later.
	 *
	 * @param key
	 * 		Class name.
	 *
	 * @return Cached ClassNode.
	 */
	@Override
	public ClassNode get(Object key) {
		// Is there a cached version?
		ClassNode value = super.get(key);
		if(value == null && raw.containsKey(key)) {
			// Nope. Lets create one.
			byte[] classFile = raw.get(key);
			value = ClassUtil.getNode(classFile);
			// Bypass our overriden "put" and just store the cached version.
			super.put(key.toString(), value);
		}
		return value;
	}
	// ========= Overridden since the raw map is larger than this wrapper ======== //

	@Override
	public boolean containsKey(Object key) {
		return raw.containsKey(key);
	}

	@Override
	public boolean isEmpty() {
		return raw.isEmpty();
	}

	@Override
	public int size() {
		return raw.size();
	}

	@Override
	public Set<String> keySet() {
		return raw.keySet();
	}

	@Override
	public Collection<ClassNode> values() {
		return raw.keySet().stream().map(name -> get(name)).collect(Collectors.toSet());
	}



	// NOTE: It seems like entrySet doesn't need to be overriden if keySet/values are.
}
