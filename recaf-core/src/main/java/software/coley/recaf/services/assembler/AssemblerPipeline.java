package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembler pipeline outline.
 *
 * @param <C>
 * 		Class type which will be assembled.
 * @param <R>
 * 		Compile return value for JASM {@link Compiler}.
 * @param <I>
 * 		Class intermediate representation type.
 *
 * @author Justus Garbe
 */
public interface AssemblerPipeline<C extends ClassInfo, R extends ClassResult<I>, I> {
	/**
	 * @param input
	 * 		The text to tokenize.
	 * @param source
	 * 		Identifier of where the text originates from.
	 *
	 * @return Outcome carrying a list of tokens on successful tokenization.
	 * Outcome carrying a list of tokenization errors otherwise.
	 */
	@Nonnull
	default Outcome<List<Token>> tokenize(@Nonnull String input, @Nonnull String source) {
		return new Tokenizer().tokenize(source, input);
	}

	/**
	 * Parses only AST declarations. The contents of those declarations are not parsed.
	 * You will want to pass this result into {@link #concreteParse(List)} to get all
	 * contents parsed.
	 * <br>
	 * Alternatively you can use {@link #fullParse(List)} which does these two steps for you.
	 *
	 * @param tokens
	 * 		Tokens to parse into a series of AST elements.
	 *
	 * @return Outcome carrying the partial constructed AST elements on successful parsing.
	 * Outcome carrying a list of parse errors otherwise.
	 *
	 * @see #concreteParse(List) Second step to fully parse AST elements.
	 * @see #fullParse(List) Full parse so that you do not have to do the two-step system yourself.
	 */
	@Nonnull
	default Outcome<List<ASTElement>> roughParse(@Nonnull List<Token> tokens) {
		return new DeclarationParser().parseDeclarations(tokens);
	}

	/**
	 * The second step used after {@link #roughParse(List)}.
	 *
	 * @param elements
	 * 		Declaration elements to complete parsing of.
	 *
	 * @return Outcome carrying fully constructed AST elements on successful parsing.
	 * Outcome carrying a list of parse errors otherwise.
	 */
	@Nonnull
	Outcome<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements);

	/**
	 * The full parse operation if you do not want to call both {@link #roughParse(List)} and
	 * {@link #concreteParse(List)} individually.
	 *
	 * @param tokens
	 * 		Tokens to parse into a series of AST elements.
	 *
	 * @return Outcome carrying fully constructed AST elements on successful parsing.
	 * Outcome carrying a list of parse errors otherwise.
	 */
	@Nonnull
	default Outcome<List<ASTElement>> fullParse(@Nonnull List<Token> tokens) {
		return fullParse(tokens, null);
	}

	/**
	 * Performs a full parse while optionally reporting the outcome of each stage.
	 * <p>
	 * Intended for use in scripts and other situations where you don't need full control
	 * over the input/output state of all stages and just want to get the final parsed result.
	 *
	 * @param tokens
	 * 		Tokens to parse.
	 * @param observer
	 * 		Optional observer for rough and concrete parse outcomes.
	 *
	 * @return Combined parse outcome, with diagnostics ordered by stage.
	 *
	 * @see #assembleSource(String, String, PathNode) For a full pipeline that tokenizes, parses, and assembles in one operation.
	 */
	@Nonnull
	default Outcome<List<ASTElement>> fullParse(@Nonnull List<Token> tokens, @Nullable ParseObserver observer) {
		// First stage is rough parsing, which only parses declarations.
		// The contents of those declarations are not parsed.
		Outcome<List<ASTElement>> rough = roughParse(tokens);
		if (observer != null)
			observer.onStage(AssemblyStage.ROUGH, rough);
		if (!rough.isSuccess())
			return rough;

		// Then we do the concrete parsing, which parses the contents of the declarations.
		Outcome<List<ASTElement>> concrete = concreteParse(rough.requireValue());
		if (observer != null)
			observer.onStage(AssemblyStage.CONCRETE, concrete);
		if (rough.diagnostics().isEmpty())
			return concrete;

		// Combine diagnostics from both stages, with rough diagnostics first.
		List<Diagnostic> diagnostics = new ArrayList<>(rough.diagnostics());
		diagnostics.addAll(concrete.diagnostics());
		return Outcome.of(concrete.value(), diagnostics);
	}

	/**
	 * Takes a list of AST elements, assumed to be fully parsed <i>(Such as output from {@link #fullParse(List)})</i>,
	 * and assembles it to the target class type.
	 *
	 * @param elements
	 * 		List of AST elements representing a class to construct into a class.
	 * @param path
	 * 		Path to the expected class destination in the workspace.
	 *
	 * @return Outcome carrying the assembled class on successful assembling.
	 * Outcome carrying a list of assemble errors otherwise.
	 */
	@Nonnull
	Outcome<R> assemble(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path);

	/**
	 * Takes a list of AST elements, assumed to be fully parsed <i>(Such as output from {@link #fullParse(List)})</i>,
	 * and assembles it to the target class type.
	 *
	 * @param elements
	 * 		List of AST elements representing a class to construct into a class.
	 * @param path
	 * 		Path to the expected class destination in the workspace.
	 *
	 * @return Outcome carrying the assembled {@link ClassInfo} on successful assembling.
	 * Outcome carrying a list of assemble errors otherwise.
	 */
	@Nonnull
	default Outcome<C> assembleAndWrap(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		Outcome<R> outcome = assemble(elements, path);
		if (!outcome.hasValue())
			return Outcome.of(null, outcome.diagnostics());

		I representation = outcome.requireValue().representation();
		if (representation == null) {
			if (outcome.hasErrors())
				return Outcome.of(null, outcome.diagnostics());
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.BACKEND_EMISSION,
					DiagnosticCode.INTERNAL_INVARIANT, "Compiler produced no class representation", null));
		}

		return Outcome.of(getClassInfo(representation), outcome.diagnostics());
	}

	/**
	 * Tokenizes, parses, and assembles source in one operation.
	 * <p>
	 * Intended for use in scripts and other situations where you don't need full control
	 * over the input/output state of all stages and just want to get the final assembled class.
	 *
	 * @param input
	 * 		Source text to assemble.
	 * @param source
	 * 		Source identifier used by diagnostics.
	 * @param path
	 * 		Path to the expected class destination in the workspace.
	 *
	 * @return Outcome carrying the assembled {@link ClassResult} and all stage diagnostics.
	 */
	@Nonnull
	default Outcome<R> assembleSource(@Nonnull String input, @Nonnull String source, @Nonnull PathNode<?> path) {
		Outcome<List<Token>> tokenized = tokenize(input, source);
		if (tokenized.hasValue() && !tokenized.isSuccess()) {
			// Parse partial token output to collect additional diagnostics, but never assemble it.
			Outcome<List<ASTElement>> parsed = fullParse(tokenized.requireValue());
			List<Diagnostic> diagnostics = new ArrayList<>(tokenized.diagnostics());
			diagnostics.addAll(parsed.diagnostics());
			return Outcome.of(null, diagnostics);
		}

		return tokenized.mapValueElse(tokens -> {
			Outcome<List<ASTElement>> parsed = fullParse(tokens);
			return parsed.mapValueElse(elements -> {
				// A partial parse may carry elements, but compilation requires a clean parse.
				if (!parsed.isSuccess())
					return Outcome.failure(parsed.diagnostics());
				return assemble(elements, path);
			}, failure -> Outcome.failure(failure.diagnostics()));
		}, failure -> Outcome.failure(failure.diagnostics()));
	}

	/**
	 * @param path
	 * 		Path to class to disassemble.
	 *
	 * @return Outcome carrying the disassembled class on successful disassembling.
	 * Outcome carrying disassemble errors otherwise.
	 */
	@Nonnull
	Outcome<String> disassemble(@Nonnull ClassPathNode path);

	/**
	 * @param path
	 * 		Path to a field or method to disassemble.
	 *
	 * @return Outcome carrying the disassembled field/method on successful disassembling.
	 * Outcome carrying disassemble errors otherwise.
	 */
	@Nonnull
	Outcome<String> disassemble(@Nonnull ClassMemberPathNode path);

	/**
	 * @param path
	 * 		Path to annotation to disassemble.
	 *
	 * @return Outcome carrying the disassembled annotation on successful disassembling.
	 * Outcome carrying disassemble errors otherwise.
	 */
	@Nonnull
	Outcome<String> disassemble(@Nonnull AnnotationPathNode path);

	/**
	 * @param path
	 * 		Path to some item that can be disassembled.
	 *
	 * @return Outcome carrying the disassembled item on successful disassembling.
	 * Outcome carrying disassemble errors otherwise.
	 */
	@Nonnull
	default Outcome<String> disassemble(@Nonnull PathNode<?> path) {
		return switch (path) {
			case ClassPathNode classPathNode -> disassemble(classPathNode);
			case ClassMemberPathNode classMemberPathNode -> disassemble(classMemberPathNode);
			case AnnotationPathNode annotationPathNode -> disassemble(annotationPathNode);
			default -> Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION, DiagnosticCode.UNCLASSIFIED,
					"Unsupported node type: " + path.getClass().getName(), null));
		};
	}

	/**
	 * @param info
	 * 		Class info to convert into the intermediate representation format.
	 *
	 * @return IR.
	 */
	@Nonnull
	I getRepresentation(@Nonnull C info);

	/**
	 * @param representation
	 * 		Intermediate representation format to map into Recaf's class info type.
	 *
	 * @return Class info.
	 */
	@Nonnull
	C getClassInfo(@Nonnull I representation);

	/**
	 * @return Pipeline's specific config.
	 */
	@Nonnull
	AssemblerPipelineConfig getConfig();
}
