package software.coley.recaf.util.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Models AST usage for things that can be read/written to <i>(literally or in an abstract sense)</i>.
 *
 * @param readers
 * 		Elements that read from the target item.
 * @param writers
 * 		Elements that write to the target item.
 * @param isParameter
 *        {@code true} when this variable is defined as a method parameter.
 *
 * @author Matt Coley
 */
public record JasmAstUsages(@Nonnull List<ASTElement> readers, @Nonnull List<ASTElement> writers, boolean isParameter) {
	/**
	 * Empty usage.
	 */
	public static final JasmAstUsages EMPTY_USAGE = new JasmAstUsages(Collections.emptyList(), Collections.emptyList(), false);

	/**
	 * @return Stream of both readers and writers.
	 */
	@Nonnull
	public Stream<ASTElement> readersAndWriters() {
		return Stream.concat(readers.stream(), writers.stream());
	}

	/**
	 * @param element
	 * 		Element to add as a reader.
	 *
	 * @return Copy with added element.
	 */
	@Nonnull
	public JasmAstUsages withNewRead(@Nonnull ASTElement element) {
		List<ASTElement> newReaders = new ArrayList<>(readers);
		newReaders.add(element);
		return new JasmAstUsages(newReaders, writers, isParameter);
	}

	/**
	 * @param element
	 * 		Element to add as a writer.
	 *
	 * @return Copy with added element.
	 */
	@Nonnull
	public JasmAstUsages withNewWrite(@Nonnull ASTElement element) {
		List<ASTElement> newWriters = new ArrayList<>(writers);
		newWriters.add(element);
		return new JasmAstUsages(readers, newWriters, isParameter);
	}

	/**
	 * @return Copy with {@link #isParameter()} set to {@code true}.
	 */
	@Nonnull
	public JasmAstUsages asParameter() {
		return new JasmAstUsages(readers, writers, true);
	}
}
