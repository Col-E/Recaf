package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.util.EscapeUtil;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;

/**
 * A single completion offered by {@link AssemblerTabCompleter}, one subtype per kind of completable assembler content.
 *
 * @author Matt Coley
 */
public sealed interface AssemblerCompletion {
	/**
	 * @return Text to insert.
	 */
	@Nonnull
	String text();

	/**
	 * @param text
	 * 		Opcode name.
	 */
	record Opcode(@Nonnull String text) implements AssemblerCompletion {}

	/**
	 * @param path
	 * 		Path to the class to complete.
	 * @param text
	 * 		Escaped text to insert, either an internal name or a descriptor.
	 */
	record Type(@Nonnull ClassPathNode path, @Nonnull String text) implements AssemblerCompletion {
		public Type(@Nonnull ClassPathNode path) {
			this(path, EscapeUtil.escapeLiteral(path.getValue().getName()));
		}
	}

	/**
	 * @param text
	 * 		Operand token to complete.
	 * @param kind
	 * 		Semantic category used to select an icon.
	 */
	record Operand(@Nonnull String text, @Nonnull OperandKind kind) implements AssemblerCompletion {}

	/**
	 * Category of an {@link Operand}, used to select its icon.
	 */
	enum OperandKind {
		REGISTER,
		LABEL,
		LOCAL,
		STRUCTURED_CONSTANT
	}

	/**
	 * @param path
	 * 		Path to the member to complete.
	 * @param text
	 * 		Escaped text to insert: the name and descriptor, or only the descriptor for descriptor-only completions.
	 */
	record Member(@Nonnull ClassMemberPathNode path, @Nonnull String text) implements AssemblerCompletion {
		public Member(@Nonnull ClassMemberPathNode path) {
			this(path, EscapeUtil.escapeLiteral(path.getValue().getName()) + " "
					+ EscapeUtil.escapeLiteral(path.getValue().getDescriptor()));
		}
	}
}
