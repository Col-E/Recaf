package software.coley.recaf.services.compile;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Caches classpath listings from fallback file managers.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CompileClasspathCache implements Service {
	public static final String SERVICE_ID = "java-classpath-cache";
	private final CompileClasspathCacheConfig config;
	private final Object cacheLock = new Object();
	private Cache<ClassPathListKey, List<JavaFileObject>> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(15, TimeUnit.MINUTES)
			.maximumSize(100)
			.build();

	@Inject
	public CompileClasspathCache(@Nonnull CompileClasspathCacheConfig config) {
		this.config = config;

		config.getCacheMaxSize().addChangeListener((ob, old, cur) -> rebuildCache());
		config.getCacheLifespanMinutes().addChangeListener((ob, old, cur) -> rebuildCache());

		rebuildCache();
	}

	/**
	 * Rebuilds the cache with the current configuration.
	 */
	private void rebuildCache() {
		synchronized (cacheLock) {
			if (cache != null)
				cache.invalidateAll();
			cache = CacheBuilder.newBuilder()
					.expireAfterWrite(config.getCacheLifespanMinutes().getValue(), TimeUnit.MINUTES)
					.maximumSize(config.getCacheMaxSize().getValue())
					.build();
		}
	}

	/**
	 * Gets the fallback manager's classpath listing.
	 *
	 * @param fileManager
	 * 		Fallback file manager to list from.
	 * @param classPath
	 * 		Compiler classpath, used to distinguish cached fallback results from other compilations.
	 * @param versionTarget
	 * 		Compiler target release, used to distinguish multi-release classpath results.
	 * @param location
	 * 		Location to list from.
	 * @param packageName
	 * 		Package name to begin the search from.
	 * @param kinds
	 * 		Kinds of file objects to include in the result.
	 * @param recurse
	 *        {@code true} to include subpackages.
	 *
	 * @return File objects matching the given criteria.
	 *
	 * @throws IOException
	 * 		When an IO error occurred,
	 * 		or if {@link JavaFileManager#close} has been called and this file manager cannot be reopened.
	 */
	@Nonnull
	public List<JavaFileObject> getFallbackClassPathList(@Nonnull JavaFileManager fileManager,
	                                                     @Nullable String classPath,
	                                                     int versionTarget,
	                                                     @Nonnull JavaFileManager.Location location,
	                                                     @Nonnull String packageName,
	                                                     @Nonnull Set<JavaFileObject.Kind> kinds,
	                                                     boolean recurse) throws IOException {
		// If caching is disabled, just return the result of the fallback manager.
		if (!config.getCacheClasspath().getValue())
			return toList(fileManager.list(location, packageName, kinds, recurse));

		// Check for previously cached results.
		ClassPathListKey key = new ClassPathListKey(classPath, versionTarget, packageName, Set.copyOf(kinds), recurse);
		List<JavaFileObject> cached = cache.getIfPresent(key);
		if (cached != null)
			return cached;

		// Synchronize the miss path so concurrent compilations do not all repeat the same expensive empty lookup.
		synchronized (cacheLock) {
			cached = cache.getIfPresent(key);
			if (cached != null)
				return cached;

			List<JavaFileObject> list = toList(fileManager.list(location, packageName, kinds, recurse));
			if (list.isEmpty() || config.getCacheNonEmptyLookups().getValue())
				cache.put(key, list);
			return list;
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CompileClasspathCacheConfig getServiceConfig() {
		return config;
	}

	@Nonnull
	private static List<JavaFileObject> toList(@Nonnull Iterable<JavaFileObject> iterable) {
		List<JavaFileObject> list = new ArrayList<>();
		iterable.forEach(list::add);
		return List.copyOf(list);
	}

	private record ClassPathListKey(@Nullable String classPath,
	                                int versionTarget,
	                                @Nonnull String packageName,
	                                @Nonnull Set<JavaFileObject.Kind> kinds,
	                                boolean recurse) {}
}
