package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Resource for dynamic runtime lookups.
 *
 * @author Matt
 */
public class ClasspathResource extends JavaResource {
	private static final ClasspathResource INSTANCE = new ClasspathResource();

	private ClasspathResource() {
		super(ResourceKind.JAR);
	}

	/**
	 * @return The classpath resource instance.
	 */
	public static ClasspathResource get() {
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
				InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class");
				byte[] value = null;
				if (in != null) {
					try {
						value = IOUtil.toByteArray(in, new ByteArrayOutputStream(in.available()), new byte[4096]);
					} catch (IOException ex) {
						Log.error(ex, "I/O error");
					} finally {
						IOUtil.close(in);
					}
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



}
