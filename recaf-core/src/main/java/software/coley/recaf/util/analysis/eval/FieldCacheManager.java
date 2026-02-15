package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support for tracking instance and static field values during evaluation.
 *
 * @author Matt Coley
 */
public class FieldCacheManager {
	/** Maps instances to their field states. */
	protected final Map<ReValue, FieldCache> instanceFields = Collections.synchronizedMap(new IdentityHashMap<>());
	/** Maps class names to their static field states. */
	protected final Map<String, FieldCache> staticFields = new ConcurrentHashMap<>();

	/**
	 * Clear all instance and static field caches.
	 */
	public void reset() {
		instanceFields.clear();
		staticFields.clear();
	}

	/**
	 * Get the static field cache for a given class.
	 *
	 * @param className
	 * 		Internal class name.
	 *
	 * @return Field cache of static fields for the given class.
	 */
	@Nonnull
	public FieldCache getStaticFieldCache(@Nonnull String className) {
		return staticFields.computeIfAbsent(className, c -> new FieldCache());
	}

	/**
	 * Get the instance field cache for a given instance.
	 *
	 * @param instance
	 * 		Value instance.
	 *
	 * @return Field cache of static fields for the given class.
	 */
	@Nonnull
	public FieldCache getInstanceFieldCache(@Nonnull ReValue instance) {
		return instanceFields.computeIfAbsent(instance, c -> new FieldCache());
	}
}
