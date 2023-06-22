package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.Decompiler;

import java.util.HashMap;
import java.util.Map;

/**
 * Built in property to cache decompilation results for {@link software.coley.recaf.info.ClassInfo} instances,
 * reducing wasted duplicate work on decompiling the same code over and over again.
 *
 * @author Matt Coley
 */
public class CachedDecompileProperty extends BasicProperty<CachedDecompileProperty.Cache> {
	public static final String KEY = "cached-decompiled-code";

	/**
	 * New empty cache.
	 */
	public CachedDecompileProperty() {
		super(KEY, new Cache());
	}

	/**
	 * @param classInfo
	 * 		Class to cache decompilation of.
	 * @param decompiler
	 * 		Associated  decompiler.
	 * @param result
	 * 		Decompiler result to cache.
	 */
	public static void set(@Nonnull ClassInfo classInfo, @Nonnull Decompiler decompiler,
						   @Nonnull DecompileResult result) {
		Cache cache = classInfo.getPropertyValueOrNull(KEY);
		if (cache == null) {
			CachedDecompileProperty property = new CachedDecompileProperty();
			classInfo.setProperty(property);
			cache = property.value();
		}
		// Save to cache
		cache.save(decompiler.getName(), result);
	}

	/**
	 * @param classInfo
	 * 		Class with cached decompilation.
	 * @param decompiler
	 * 		Associated decompiler.
	 *
	 * @return Cached decompilation result, or {@code null} when no cached value exists.
	 */
	@Nullable
	public static DecompileResult get(@Nonnull ClassInfo classInfo, @Nonnull Decompiler decompiler) {
		Cache cache = classInfo.getPropertyValueOrNull(KEY);
		if (cache == null) return null;
		return cache.get(decompiler.getName());
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull ClassInfo info) {
		info.removeProperty(KEY);
	}

	@Override
	public boolean persistent() {
		// We should disregard decompilation results between 'versions' of an info object.
		return false;
	}

	/**
	 * Basic cache for decompiler results.
	 */
	public static class Cache {
		private final Map<String, DecompileResult> implToCode = new HashMap<>();

		/**
		 * @param decompilerId
		 * 		Unique ID of decompiler.
		 *
		 * @return Decompiler result of prior run.
		 */
		public DecompileResult get(String decompilerId) {
			return implToCode.get(decompilerId);
		}

		/**
		 * @param decompilerId
		 * 		Unique ID of decompiler.
		 * @param result
		 * 		Decompiler result to cache.
		 */
		public void save(String decompilerId, DecompileResult result) {
			implToCode.put(decompilerId, result);
		}
	}
}
