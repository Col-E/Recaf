package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import net.raphimc.javadowngrader.JavaDowngrader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wrapper of a string-to-bytecode map with additional utility methods.
 *
 * @author Matt Coley
 */
public class CompileMap extends TreeMap<String, byte[]> {
	private static final Logger logger = Logging.get(CompileMap.class);

	/**
	 * @param map
	 * 		Map to copy.
	 */
	public CompileMap(@Nonnull Map<String, byte[]> map) {
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

	/**
	 * Down sample all classes in the map to the target version.
	 *
	 * @param version
	 * 		Target version to downsample to.
	 */
	public void downsample(int version) {
		for (Map.Entry<String, byte[]> entry : new HashSet<>(entrySet())) {
			String key = entry.getKey();
			try {
				ClassNode node = new ClassNode();
				ClassReader reader = new ClassReader(entry.getValue());
				reader.accept(node, 0);
				if (node.version > version) {
					JavaDowngrader.downgrade(node, version);
					ClassWriter writer = new ClassWriter(reader, 0);
					node.accept(writer);
					put(key, writer.toByteArray());
				}
			} catch (Throwable t) {
				logger.error("Failed down sampling '{}' to version {}", key, version, t);
			}
		}
	}
}
