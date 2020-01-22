package me.coley.recaf.workspace;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 * Importable instrumentation resource.
 *
 * @author Matt
 */
public class InstrumentationResource extends JavaResource {
	private final Instrumentation instrumentation;

	/**
	 * Constructs an instrumentation resource.
	 *
	 * @param instrumentation
	 * 		Instrumentation instance provided by the tools api.
	 */
	public InstrumentationResource(Instrumentation instrumentation) {
		super(ResourceKind.INSTRUMENTATION);
		this.instrumentation = instrumentation;
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		Map<String, byte[]> classes = new HashMap<>();
		// iterate over loaded classes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		for(Class<?> c : instrumentation.getAllLoadedClasses()) {
			String name = Type.getInternalName(c);
			// skip specified prefixes
			if(shouldSkip(name))
				continue;
			String path = name.concat(".class");
			ClassLoader loader = c.getClassLoader();
			try(InputStream in = (loader != null) ?
					loader.getResourceAsStream(path) : ClassLoader.getSystemResourceAsStream(path)) {
				if(in != null) {
					out.reset();
					classes.put(name, IOUtils.toByteArray(in));
				}
			}
		}
		return classes;
	}

	@Override
	protected Map<String, byte[]> loadFiles() {
		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return "Instrumentation";
	}
}
