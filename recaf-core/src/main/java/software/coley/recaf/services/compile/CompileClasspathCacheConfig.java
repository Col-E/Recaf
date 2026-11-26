package software.coley.recaf.services.compile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link CompileClasspathCache}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CompileClasspathCacheConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean cacheClasspath = new ObservableBoolean(true);
	private final ObservableBoolean cacheNonEmptyLookups = new ObservableBoolean(true);
	private final ObservableInteger cacheLifespanMinutes = new ObservableInteger(15);
	private final ObservableInteger cacheMaxSize = new ObservableInteger(100);

	@Inject
	public CompileClasspathCacheConfig() {
		super(ConfigGroups.SERVICE_COMPILE, CompileClasspathCache.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("cache-classpath", boolean.class, cacheClasspath));
		addValue(new BasicConfigValue<>("cache-non-empty-lookups", boolean.class, cacheNonEmptyLookups));
		addValue(new BasicConfigValue<>("cache-lifespan-minutes", int.class, cacheLifespanMinutes));
		addValue(new BasicConfigValue<>("cache-max-size", int.class, cacheMaxSize));
	}

	/**
	 * @return {@code true} when overall caching is enabled. {@code false} to disable caching entirely.
	 */
	public ObservableBoolean getCacheClasspath() {
		return cacheClasspath;
	}

	/**
	 * Empty lookup results are often the most common result of classpath listings, and are generally safe to cache.
	 * Non-empty results can be volatile, but generally unlikely in our context.
	 *
	 * @return {@code true} when including non-empty lookups in the cache. {@code false} to only cache empty lookups.
	 */
	public ObservableBoolean getCacheNonEmptyLookups() {
		return cacheNonEmptyLookups;
	}

	/**
	 * @return Lifespan of cache entries in minutes.
	 */
	public ObservableInteger getCacheLifespanMinutes() {
		return cacheLifespanMinutes;
	}

	/**
	 * @return Maximum number of cache entries to store.
	 */
	public ObservableInteger getCacheMaxSize() {
		return cacheMaxSize;
	}
}
