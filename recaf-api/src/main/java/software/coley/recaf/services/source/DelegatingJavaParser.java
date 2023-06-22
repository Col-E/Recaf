package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Delegating implementation of {@link JavaParser}.
 *
 * @author Matt Coley
 */
public class DelegatingJavaParser implements JavaParser {
	private static final Logger logger = Logging.get(DelegatingJavaParser.class);
	private final JavaParser delegate;

	/**
	 * @param delegate
	 * 		Backing parser value.
	 */
	public DelegatingJavaParser(@Nonnull JavaParser delegate) {
		this.delegate = delegate;
	}

	@Nonnull
	@Override
	public List<J.CompilationUnit> parseInputs(@Nonnull Iterable<Input> sources,
											   @Nullable Path relativeTo,
											   @Nonnull ExecutionContext ctx) {
		try {
			// The default source-set type generation logic is not well optimized.
			// We also do not gain significant benefits from it, so we can skip it entirely.
			ctx.putMessage(SKIP_SOURCE_SET_TYPE_GENERATION, true);
			return delegate.parseInputs(sources, relativeTo, ctx);
		} catch (Throwable t) {
			logger.error("Error while parsing source into AST", t);
			return Collections.emptyList();
		}
	}

	@Nonnull
	@Override
	public JavaParser reset() {
		return delegate.reset();
	}

	@Nonnull
	@Override
	public JavaParser reset(@Nonnull Collection<URI> uris) {
		return delegate.reset(uris);
	}

	@Override
	public void setClasspath(@Nonnull Collection<Path> classpath) {
		delegate.setClasspath(classpath);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setSourceSet(@Nonnull String sourceSet) {
		delegate.setSourceSet(sourceSet);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public JavaSourceSet getSourceSet(@Nonnull ExecutionContext ctx) {
		return delegate.getSourceSet(ctx);
	}

	@Nonnull
	@Override
	public Path sourcePathFromSourceText(@Nonnull Path prefix, @Nonnull String sourceCode) {
		// Bogus, we do not want obfuscated inputs triggering path get operations.
		return Paths.get(UUID.randomUUID().toString());
	}
}
