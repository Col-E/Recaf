package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Context model. A context supplies the text the popup replaces and the completions to offer for it.
 *
 * @author Matt Coley
 */
sealed interface AssemblerCompletionContext {
	/**
	 * @return Completions to offer.
	 */
	@Nonnull
	default List<AssemblerCompletion> complete() {
		return Collections.emptyList();
	}

	/**
	 * @return Text to replace with a completion.
	 */
	@Nonnull
	default String partialMatchedText() {
		return "";
	}

	/**
	 * Nothing to offer.
	 */
	record Empty() implements AssemblerCompletionContext {}

	/**
	 * Context for opcode completion.
	 *
	 * @param partialInput
	 * 		Text to replace with a completion.
	 * @param opcodes
	 * 		All opcode names for the target context.
	 */
	record OpcodeToken(@Nonnull String partialInput,
	                   @Nonnull List<String> opcodes) implements AssemblerCompletionContext {
		@Nonnull
		@Override
		public String partialMatchedText() {
			return partialInput;
		}

		@Nonnull
		@Override
		public List<AssemblerCompletion> complete() {
			return AssemblerCompletionEngine.opcodeCompletions(opcodes, partialInput).stream()
					.map(AssemblerCompletion.Opcode::new)
					.map(AssemblerCompletion.class::cast)
					.toList();
		}
	}

	/**
	 * Context for operand completion.
	 *
	 * @param matchedText
	 * 		Text to replace with a completion.
	 * @param candidates
	 * 		Completions to offer.
	 */
	record OperandToken(@Nonnull String matchedText,
	                    @Nonnull List<AssemblerCompletion> candidates) implements AssemblerCompletionContext {
		public OperandToken {
			// Sort by text so the order does not depend on workspace or set iteration order.
			candidates = candidates.stream()
					.sorted(Comparator.comparing(AssemblerCompletion::text))
					.toList();
		}

		@Nonnull
		@Override
		public String partialMatchedText() {
			return matchedText;
		}

		@Nonnull
		@Override
		public List<AssemblerCompletion> complete() {
			return candidates;
		}
	}
}
