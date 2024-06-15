package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import software.coley.collections.Unchecked;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service for tracking shared data for AST parsing.
 *
 * @author Matt Coley
 */
@WorkspaceScoped
public class AstService implements Service {
	public static final String ID = "ast";
	private final AstServiceConfig config;
	private final JavaTypeCache javaTypeCache = new JavaTypeCacheExt();
	private final Workspace workspace;

	@Inject
	public AstService(@Nonnull AstServiceConfig config,
					  @Nonnull Workspace workspace) {
		this.config = config;
		this.workspace = workspace;
	}

	// TODO: Expose code-formatting system, which we can use to post-process code in decompilers
	//  - We may be able to allow the user to tweak styles like the IntelliJ format preview
	//  - See: 'org.openrewrite.java.style'

	/**
	 * Allocates a parser with the class-path of classes referenced by the given class.
	 *
	 * @param target
	 * 		Class to target.
	 *
	 * @return New parser instance to handle source of the given class.
	 */
	@Nonnull
	public JavaParser newParser(@Nonnull JvmClassInfo target) {
		// Collect names of classes referenced.
		Set<String> classNames = target.getReferencedClasses();

		// Collect bytes of all referenced classes.
		// For android classes, it is assumed 'asJvmClass()' will lazily convert to JVM classes.
		byte[][] classpath = classNames.stream()
				.map(workspace::findClass)
				.filter(Objects::nonNull)
				.map(path -> path.getValue().asJvmClass().getBytecode())
				.toArray(byte[][]::new);
		JavaParser parser = JavaParser.fromJavaVersion()
				.classpath(classpath)
				.typeCache(javaTypeCache)
				.build();
		return new DelegatingJavaParser(parser);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public AstServiceConfig getServiceConfig() {
		return config;
	}

	/**
	 * Modified cache impl that does not compress keys.
	 * For more memory cost, we get some additional performance.
	 */
	private static class JavaTypeCacheExt extends JavaTypeCache {
		private final Map<Object, Object> internalCache;

		@SuppressWarnings("unchecked")
		private JavaTypeCacheExt() {
			internalCache = (Map<Object, Object>) Unchecked.get(() -> ReflectUtil.getDeclaredField(JavaTypeCache.class, "typeCache").get(this));
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T get(@Nonnull String signature) {
			return (T) internalCache.get(signature);
		}

		@Override
		public void put(@Nonnull String signature, @Nonnull Object o) {
			internalCache.put(signature, o);
		}
	}
}
