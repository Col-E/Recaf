package me.coley.recaf.workspace.resource;

import com.google.common.reflect.ClassPath;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.threading.ThreadLocals;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.source.EmptyContentSource;
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
	private static final Object STUB = new Object();
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
		Map<String, ClassInfo> map = new HashMap<>() {
			private final Map<String, Object> cache = new HashMap<>();

			@Override
			public ClassInfo get(Object name) {
				if (name == null)
					return null;
				String key = name.toString();
				if (key.indexOf('.') >= 0)
					key = key.replace('.', '/');
				Object present = cache.get(key);
				if (present == STUB)
					return null;
				if (present instanceof ClassInfo)
					return (ClassInfo) present;
				// Can't do "computeIfAbsent" since we also want to store null values.
				byte[] value = null;
				try (InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class")) {
					if (in != null) {
						value = IOUtil.toByteArray(in, ThreadLocals.getByteBuffer());
					}
				} catch (IOException ex) {
					logger.error("Failed to fetch runtime bytecode of class: " + key, ex);
				}
				if (value == null) {
					cache.put(key, STUB);
					return null;
				}
				ClassInfo info = ClassInfo.read(value);
				cache.put(key, info);
				return info;
			}

			@Override
			public boolean containsKey(Object key) {
				return get(key) != null;
			}

			@Override
			public void clear() {
				// Prevent clearing
			}
		};
		return new ClassMap(this, map);
	}
}
