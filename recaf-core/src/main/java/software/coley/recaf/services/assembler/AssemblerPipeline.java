package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.ParsingResult;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

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
public interface AssemblerPipeline<C extends ClassInfo, R extends ClassResult, I extends ClassRepresentation> {
	/**
	 * @param input
	 * 		The text to tokenize.
	 * @param source
	 * 		Identifier of where the text originates from.
	 *
	 * @return Result wrapping a list of tokens on successful tokenization.
	 * Result wrapping a list of tokenization errors otherwise.
	 */
	@Nonnull
	default Result<List<Token>> tokenize(@Nonnull String input, @Nonnull String source) {
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
	 * @return Result wrapping the partial constructed AST elements on successful parsing.
	 * Result wrapping a list of parse errors otherwise.
	 *
	 * @see #concreteParse(List) Second step to fully parse AST elements.
	 * @see #fullParse(List) Full parse so that you do not have to do the two-step system yourself.
	 */
	@Nonnull
	default ParsingResult<List<ASTElement>> roughParse(@Nonnull List<Token> tokens) {
		return new DeclarationParser().parseDeclarations(tokens);
	}

	/**
	 * The second step used after {@link #roughParse(List)}.
	 *
	 * @param elements
	 * 		Declaration elements to complete parsing of.
	 *
	 * @return Result wrapping fully constructed AST elements on successful parsing.
	 * Result wrapping a list of parse errors otherwise.
	 */
	@Nonnull
	Result<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements);

	/**
	 * The full parse operation if you do not want to call both {@link #roughParse(List)} and
	 * {@link #concreteParse(List)} individually.
	 *
	 * @param tokens
	 * 		Tokens to parse into a series of AST elements.
	 *
	 * @return Result wrapping fully constructed AST elements on successful parsing.
	 * Result wrapping a list of parse errors otherwise.
	 */
	@Nonnull
	default Result<List<ASTElement>> fullParse(@Nonnull List<Token> tokens) {
		var result = roughParse(tokens);
		if (result.isOk()) {
			return concreteParse(result.get());
		} else {
			return result;
		}
	}

	/**
	 * Takes a list of AST elements, assumed to be fully parsed, and assembles it to the target class type.
	 *
	 * @param elements
	 * 		List of AST elements representing a class to construct into a class.
	 * @param path
	 * 		Path to the expected class destination in the workspace.
	 *
	 * @return Result wrapping the assembled class on successful assembling.
	 * Result wrapping a list of assemble errors otherwise.
	 */
	@Nonnull
	Result<R> assemble(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path);

	/**
	 * Takes a list of AST elements, assumed to be fully parsed, and assembles it to the target class type.
	 *
	 * @param elements
	 * 		List of AST elements representing a class to construct into a class.
	 * @param path
	 * 		Path to the expected class destination in the workspace.
	 *
	 * @return Result wrapping the assembled class on successful assembling.
	 * Result wrapping a list of assemble errors otherwise.
	 */
	@Nonnull
	default Result<C> assembleAndWrap(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		return assemble(elements, path)
				.flatMap(r -> Result.ok(getClassInfo((I) r.representation())));
	}

	/**
	 * @param path
	 * 		Path to class to disassemble.
	 *
	 * @return Result wrapping the disassembled class on successful disassembling.
	 * Result wrapping disassemble errors otherwise.
	 */
	@Nonnull
	Result<String> disassemble(@Nonnull ClassPathNode path);

	/**
	 * @param path
	 * 		Path to a field or method to disassemble.
	 *
	 * @return Result wrapping the disassembled field/method on successful disassembling.
	 * Result wrapping disassemble errors otherwise.
	 */
	@Nonnull
	Result<String> disassemble(@Nonnull ClassMemberPathNode path);

	/**
	 * @param path
	 * 		Path to annotation to disassemble.
	 *
	 * @return Result wrapping the disassembled annotation on successful disassembling.
	 * Result wrapping disassemble errors otherwise.
	 */
	@Nonnull
	Result<String> disassemble(@Nonnull AnnotationPathNode path);

	/**
	 * @param path
	 * 		Path to some item that can be disassembled.
	 *
	 * @return Result wrapping the disassembled item on successful disassembling.
	 * Result wrapping disassemble errors otherwise.
	 */
	@Nonnull
	default Result<String> disassemble(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPathNode)
			return disassemble(classPathNode);
		if (path instanceof ClassMemberPathNode classMemberPathNode)
			return disassemble(classMemberPathNode);
		if (path instanceof AnnotationPathNode annotationPathNode)
			return disassemble(annotationPathNode);
		return Result.err(Error.of("Unsupported node type: " + path.getClass().getName(), null));
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
