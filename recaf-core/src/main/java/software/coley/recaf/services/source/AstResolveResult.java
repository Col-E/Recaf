package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;
import software.coley.sourcesolver.resolve.result.Resolution;

/**
 * Wrapper for {@link Resolution} values.
 *
 * @param isDeclaration
 * 		Flag indicating if resolved item is a declaration or reference.
 * @param path
 * 		Resolved value.
 *
 * @author Matt Coley
 * @see ResolverAdapter
 */
public record AstResolveResult(boolean isDeclaration, @Nonnull PathNode<?> path) {
	/**
	 * @param path
	 * 		Path to wrap.
	 *
	 * @return Result of declaration for the given path.
	 */
	@Nonnull
	public static AstResolveResult declared(@Nonnull PathNode<?> path) {
		return new AstResolveResult(true, path);
	}

	/**
	 * @param path
	 * 		Path to wrap.
	 *
	 * @return Result of reference to the given path.
	 */
	@Nonnull
	public static AstResolveResult reference(@Nonnull PathNode<?> path) {
		return new AstResolveResult(false, path);
	}

	/**
	 * @return Copy of self, as a declaration.
	 */
	@Nonnull
	public AstResolveResult asDeclaration() {
		return new AstResolveResult(true, path());
	}

	/**
	 * @return Copy of self, as a reference.
	 */
	@Nonnull
	public AstResolveResult asReference() {
		return new AstResolveResult(false, path());
	}

	/**
	 * @param other
	 * 		Other result to match {@link #isDeclaration()} state of.
	 *
	 * @return Copy of self, as matching state.
	 */
	@Nonnull
	public AstResolveResult matchDeclarationState(@Nonnull AstResolveResult other) {
		if (this == other)
			return this;
		if (isDeclaration == other.isDeclaration)
			return this;
		return other.isDeclaration ? asDeclaration() : asReference();
	}
}
