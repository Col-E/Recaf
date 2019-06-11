package me.coley.recaf.workspace;

import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.ClassUtil;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;


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

	@Override
	public ClassNode get(Object key) {
		ClassNode value = super.get(key);
		if(value == null && raw.containsKey(key)) {
			try {
				byte[] classFile = raw.get(key);
				value = ClassUtil.getNode(classFile);
				put(key.toString(), value);
			} catch(Exception e) {
				Logging.fatal(e);
			}
		}
		return value;
	}
}
