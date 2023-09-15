package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import org.openrewrite.java.tree.J;
import software.coley.recaf.path.PathNode;

/**
 * Wrapper for {@link AstContextHelper#resolve(J.CompilationUnit, int, String)} operations.
 *
 * @param isDeclaration
 * 		Flag indicating if resolved item is a declaration or reference.
 * @param path
 * 		Resolved value.
 *
 * @author Matt Coley
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
	public AstResolveResult asDeclaration() {
		return new AstResolveResult(true, path());
	}

	/**
	 * @return Copy of self, as a reference.
	 */
	public AstResolveResult asReference() {
		return new AstResolveResult(false, path());
	}

	/**
	 * @param other
	 * 		Other result to match {@link #isDeclaration()} state of.
	 *
	 * @return Copy of self, as matching state.
	 */
	public AstResolveResult matchDeclarationState(AstResolveResult other) {
		if (this == other)
			return this;
		if (isDeclaration == other.isDeclaration)
			return this;
		return other.isDeclaration ? asDeclaration() : asReference();
	}
}
