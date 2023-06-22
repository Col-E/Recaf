package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In some cases, malformed code can confuse OpenRewrite, triggering it to double-emit
 * certain blocks, but the second being all sorts of screwed up.
 * Some invalid inputs, like removing the ';' of a package declaration, can cause {@link J.ClassDeclaration}
 * types and others to have bogus spaces.
 * <p>
 * <b>WARNING:</b> Sometimes the code model you <i>want</i> is attached as a {@link Space},
 * and the remaining model of the actual {@link Tree} types do not emit valid code when calling
 * {@link Tree#print(Cursor)}.
 *
 * @author Matt Coley
 */
public class SpaceFilteringVisitor extends JavaIsoVisitor<ExecutionContext> {
	@Nonnull
	@Override
	public Space visitSpace(Space space, @Nonnull Space.Location loc, @Nonnull ExecutionContext ctx) {
		if (!space.getWhitespace().isBlank())
			return space.withWhitespace(" "); // Strip invalid whitespaces with non-whitespace characters.
		return super.visitSpace(space, loc, ctx);
	}

	/**
	 * @param units
	 * 		Units to patch.
	 *
	 * @return Patched units.
	 */
	@Nonnull
	public static List<J.CompilationUnit> patch(List<J.CompilationUnit> units) {
		List<J.CompilationUnit> mapped = new ArrayList<>(units.size());
		for (J.CompilationUnit unit : units)
			mapped.add(patch(unit));
		return mapped;
	}

	/**
	 * @param unit
	 * 		Unit to patch.
	 *
	 * @return Patched unit.
	 */
	@Nonnull
	public static J.CompilationUnit patch(J.CompilationUnit unit) {
		SpaceFilteringVisitor visitor = new SpaceFilteringVisitor();
		ExecutionContext spaceCtx = new InMemoryExecutionContext();
		return (J.CompilationUnit) Objects.requireNonNull(unit.acceptJava(visitor, spaceCtx));
	}
}
