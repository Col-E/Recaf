package software.coley.recaf.info.properties.builtin;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Built in property to track the version a {@link JvmClassInfo} belongs to in
 * {@link WorkspaceResource#getVersionedJvmClassBundles()}
 *
 * @author Matt Coley
 */
public class VersionedClassProperty extends BasicProperty<Integer> {
	private static final Int2ObjectMap<VersionedClassProperty> cache = new Int2ObjectArrayMap<>();
	public static final String KEY = "meta-inf-versioned";

	/**
	 * @param value
	 * 		Version value
	 */
	private VersionedClassProperty(int value) {
		super(KEY, value);
	}

	/**
	 * @param classInfo
	 * 		Class info to link with a version.
	 *
	 * @return Version associated with the class,
	 * used as key for {@link WorkspaceResource#getVersionedJvmClassBundles()}.
	 * May be {@code null} if no version association exists.
	 */
	@Nullable
	public static Integer get(@Nonnull JvmClassInfo classInfo) {
		return classInfo.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param classInfo
	 * 		Class info to link with a version.
	 * @param version
	 * 		Version associated with the class,
	 * 		used as key for {@link WorkspaceResource#getVersionedJvmClassBundles()}.
	 */
	public static void set(@Nonnull JvmClassInfo classInfo, int version) {
		synchronized (cache) {
			classInfo.setProperty(cache.computeIfAbsent(version, VersionedClassProperty::new));
		}
	}

	/**
	 * @param classInfo
	 * 		Class info to unlink with a version.
	 */
	public static void remove(@Nonnull JvmClassInfo classInfo) {
		classInfo.removeProperty(KEY);
	}
}
