package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

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
	public Stream<SourceFile> parseInputs(@Nonnull Iterable<Input> sources,
										  @Nullable Path relativeTo,
										  @Nonnull ExecutionContext ctx) {
		try {
			// The default source-set type generation logic is not well optimized.
			// We also do not gain significant benefits from it, so we can skip it entirely.
			ctx.putMessage(SKIP_SOURCE_SET_TYPE_GENERATION, true);
			return delegate.parseInputs(sources, relativeTo, ctx);
		} catch (Throwable t) {
			logger.error("Error while parsing source into AST", t);
			return Stream.empty();
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

	@Nonnull
	@Override
	public Path sourcePathFromSourceText(@Nonnull Path prefix, @Nonnull String sourceCode) {
		// Bogus, we do not want obfuscated inputs triggering path get operations.
		return Paths.get(UUID.randomUUID() + ".java");
	}
}
