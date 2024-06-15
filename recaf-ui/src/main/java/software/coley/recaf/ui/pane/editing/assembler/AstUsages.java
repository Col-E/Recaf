package software.coley.recaf.ui.pane.editing.assembler;

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
 */
public record AstUsages(@Nonnull List<ASTElement> readers, @Nonnull List<ASTElement> writers) {
	/**
	 * Empty usage.
	 */
	public static final AstUsages EMPTY_USAGE = new AstUsages(Collections.emptyList(), Collections.emptyList());

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
	public AstUsages withNewRead(@Nonnull ASTElement element) {
		List<ASTElement> newReaders = new ArrayList<>(readers);
		newReaders.add(element);
		return new AstUsages(newReaders, writers);
	}

	/**
	 * @param element
	 * 		Element to add as a writer.
	 *
	 * @return Copy with added element.
	 */
	@Nonnull
	public AstUsages withNewWrite(@Nonnull ASTElement element) {
		List<ASTElement> newWriters = new ArrayList<>(writers);
		newWriters.add(element);
		return new AstUsages(readers, newWriters);
	}
}
