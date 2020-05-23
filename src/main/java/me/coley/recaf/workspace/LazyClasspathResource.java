package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Resource for lazyily fetching classes via runtime lookups.
 *
 * @author Matt
 */
public class LazyClasspathResource extends JavaResource {
	private static final ResourceLocation LOCATION = LiteralResourceLocation.ofKind(ResourceKind.JAR, "ClassPath");
	private static final LazyClasspathResource INSTANCE = new LazyClasspathResource();

	private LazyClasspathResource() {
		super(ResourceKind.JAR);
	}

	/**
	 * @return The classpath resource instance.
	 */
	public static LazyClasspathResource get() {
		return INSTANCE;
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return new HashMap<String, byte[]>() {
			private final Map<String, byte[]> cache = new HashMap<>();

			@Override
			public byte[] get(Object name) {
				if (name == null)
					return null;
				String key = name.toString();
				if (key.contains("."))
					key = key.replace('.', '/');
				if(cache.containsKey(key))
					return cache.get(key);
				// Can't do "computeIfAbsent" since we also want to store null values.
				byte[] value = null;
				try (InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class")) {
					if (in != null) {
						value = IOUtil.toByteArray(in);
					}
				} catch (IOException ex) {
					Log.error(ex, "Failed to fetch runtime bytecode of class '{}'", key);
				}
				cache.put(key, value);
				return value;
			}

			@Override
			public boolean containsKey(Object key) {
				return get(key) != null;
			}
		};
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> copyMap(Map<String, byte[]> map) {
		return map;
	}

	@Override
	public ResourceLocation getShortName() {
		return LOCATION;
	}

	@Override
	public ResourceLocation getName() {
		return LOCATION;
	}
}
