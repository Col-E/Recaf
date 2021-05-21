package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.source.EmptyContentSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Workspace unit that pulls classes from the current runtime.
 *
 * @author Matt Coley
 */
public class RuntimeResource extends Resource {
	private static final Logger logger = Logging.get(RuntimeResource.class);
	private static final RuntimeResource instance = new RuntimeResource();
	private final ClassMap runtimeMap = createRuntimeMap();

	private RuntimeResource() {
		super(new EmptyContentSource());
	}

	/**
	 * @return Runtime resource instance.
	 */
	public static RuntimeResource get() {
		return instance;
	}

	@Override
	public ClassMap getClasses() {
		return runtimeMap;
	}

	private ClassMap createRuntimeMap() {
		Map<String, ClassInfo> map = new HashMap<String, ClassInfo>() {
			private final Map<String, ClassInfo> cache = new HashMap<>();

			@Override
			public ClassInfo get(Object name) {
				if (name == null)
					return null;
				String key = name.toString();
				if (key.contains("."))
					key = key.replace('.', '/');
				if (cache.containsKey(key))
					return cache.get(key);
				// Can't do "computeIfAbsent" since we also want to store null values.
				byte[] value = null;
				try (InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class")) {
					if (in != null) {
						value = IOUtils.toByteArray(in);
					}
				} catch (IOException ex) {
					logger.error("Failed to fetch runtime bytecode of class: " + key, ex);
				}
				ClassInfo info = value == null ? null : ClassInfo.read(value);
				cache.put(key, info);
				return info;
			}

			@Override
			public boolean containsKey(Object key) {
				return get(key) != null;
			}
		};
		return new ClassMap(this, map);
	}
}
