package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.JavaDowngraderUtil;
import software.coley.recaf.util.JavaVersion;
import xyz.wagyourtail.jvmdg.ClassDowngrader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

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
	 * @param targetJavaVersion
	 * 		Target version to downsample to. To target 8, simply pass {@code 8} <i>()</i>.
	 */
	public void downsample(int targetJavaVersion) {
		try {
			JavaDowngraderUtil.downgrade(targetJavaVersion, new HashMap<>(this), this::put);
		} catch (IOException ex) {
			logger.error("Failed down sampling to version {}",  targetJavaVersion, ex);
		}
	}
}
