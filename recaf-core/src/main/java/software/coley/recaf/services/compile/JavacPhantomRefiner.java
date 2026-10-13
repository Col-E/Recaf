package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.services.phantom.PhantomHint;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives compilation against generated phantoms in rounds. After each round, the errors javac reports against the
 * phantoms are translated into {@link PhantomHint hints}, and the phantoms are regenerated with them. Each round only
 * adds hints for what javac actually rejected, and refinement stops when a round finds nothing new or the round limit
 * is reached.
 *
 * @author jus7n
 * @see JavacDiagnosticHints
 */
final class JavacPhantomRefiner {
	/** Upper bound on rounds. */
	private static final int MAX_ROUNDS = 6;
	/** Placeholder class name to use for packages imported with star imports that have no recognized classes. */
	private static final String ABSENT_PACKAGE_MARKER = "$AbsentPackage";

	private JavacPhantomRefiner() {}

	/**
	 * @param arguments
	 * 		Arguments of the compilation.
	 * @param workspace
	 * 		Workspace the compilation runs against.
	 * @param listener
	 * 		Listener for the diagnostics of each round.
	 * @param compilation
	 * 		Runs one compilation.
	 *
	 * @return Result of the last round. When no phantoms were generated, this is the result of the only round.
	 */
	@Nonnull
	static CompilerResult compile(@Nonnull JavacArguments arguments, @Nullable Workspace workspace,
	                              @Nullable JavacListener listener, @Nonnull Compilation compilation) {
		Map<String, String> sources = arguments.getClassSources();
		List<PhantomHint> hints = new ArrayList<>();
		for (int round = 0; ; round++) {
			// Compile with the current set of hints, and see what phantoms are generated.
			Attempt attempt = compilation.compile(sources, listener, hints);

			// If no phantoms were generated, or the round limit was reached, return the result of this round.
			// Disabling phantom generation will lead to this early exit on the first round.
			GeneratedPhantomWorkspaceResource generated = attempt.generated();
			if (generated == null || round >= MAX_ROUNDS)
				return attempt.result();

			// Collect the hints from the diagnostics of this round, and prepare for the next round.
			List<CompilerDiagnostic> diagnostics = attempt.result().getDiagnostics();
			Set<PhantomHint> found = new LinkedHashSet<>(JavacDiagnosticHints.collect(diagnostics, generated, sources, sources, workspace, hints));

			// javac stops at an on-demand import of a package with no classes. We get nothing useful when this happens.
			// Compiling again with those imports removed will reveal which types are missing.
			Set<String> absent = JavacDiagnosticHints.absentStarPackages(diagnostics, sources.values());
			if (!absent.isEmpty()) {
				Map<String, String> stripped = new HashMap<>();
				sources.forEach((name, source) -> stripped.put(name, JavaSourceScan.withoutStarImports(source, absent)));

				// Compile again with the stripped sources, and collect any new hints from the diagnostics.
				Attempt discovery = compilation.compile(stripped, null, hints);
				if (discovery.generated() != null)
					found.addAll(JavacDiagnosticHints.collect(discovery.result().getDiagnostics(), discovery.generated(), sources, stripped, workspace, hints));

				// The user's imports must stay valid, so each absent package needs a class even when no name resolves into it.
				for (String pkg : absent) {
					PhantomHint marker = new PhantomHint.TypeOnly(pkg.replace('.', '/') + "/" + ABSENT_PACKAGE_MARKER);
					if (!hints.contains(marker))
						found.add(marker);
				}
			}

			// If no new hints were found, return the result of this round.
			// Otherwise, add the new hints and continue to the next round.
			if (found.isEmpty())
				return attempt.result();
			hints.addAll(found);
		}
	}

	/**
	 * One compilation attempt, with the phantom resource it was given.
	 *
	 * @param result
	 * 		Compilation result.
	 * @param generated
	 * 		Phantoms generated for this compilation, or {@code null} when none were generated for it.
	 */
	record Attempt(@Nonnull CompilerResult result, @Nullable GeneratedPhantomWorkspaceResource generated) {}

	/**
	 * Compiles one set of sources, generating phantoms with the given hints when phantom generation applies.
	 */
	@FunctionalInterface
	interface Compilation {
		/**
		 * @param classSources
		 * 		Sources to compile, keyed by internal class name.
		 * @param listener
		 * 		Listener to report diagnostics to, or {@code null} for a compilation whose diagnostics are only
		 * 		inspected internally.
		 * @param hints
		 * 		Hints to generate phantoms with.
		 *
		 * @return Compilation result and the phantoms it used.
		 */
		@Nonnull
		Attempt compile(@Nonnull Map<String, String> classSources, @Nullable JavacListener listener, @Nonnull List<PhantomHint> hints);
	}
}
