package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

import java.util.Map;

/**
 * Model of a single completion result for Java code.
 *
 * @param kind
 * 		Completion kind <i>("Is this a field, method, keyword... etc.?")</i>
 * @param displayText
 * 		Text to show in the completion popup.
 * @param insertionText
 * 		Text to insert into the editor when this completion is selected.
 * @param rank
 * 		Rank of the completion, where lower is better.
 * @param path
 * 		Path to the type being completed, if applicable.
 * 		May be {@code null} for completions that don't have a direct path reference.
 * @param sortKey
 * 		Key used to sort this completion among others.
 * @param caretBacktrack
 * 		Number of characters to move the caret back after insertion, to position it correctly for further typing.
 * @param trailingSuffix
 * 		Optional suffix to append after the main insertion text when the completion is accepted.
 *
 * @author Matt Coley
 */
public record JavaCompletion(@Nonnull CompletionKind kind,
                             @Nonnull String displayText,
                             @Nonnull String insertionText,
                             int rank,
                             @Nullable PathNode<?> path,
                             @Nonnull String sortKey,
                             int caretBacktrack,
                             @Nonnull String trailingSuffix) {
	public JavaCompletion(@Nonnull CompletionKind kind,
	                      @Nonnull String displayText,
	                      @Nonnull String insertionText,
	                      int rank,
	                      @Nullable PathNode<?> path,
	                      @Nonnull String sortKey,
	                      int caretBacktrack) {
		this(kind, displayText, insertionText, rank, path, sortKey, caretBacktrack, "");
	}

	/**
	 * @param completions
	 * 		Map of existing completions, keyed by their deduplication key.
	 * @param candidate
	 * 		Candidate completion to add or replace in the map.
	 */
	public static void addOrReplace(@Nonnull Map<String, JavaCompletion> completions, @Nonnull JavaCompletion candidate) {
		JavaCompletion existing = completions.get(candidate.dedupeKey());
		if (existing == null || candidate.rank() < existing.rank())
			completions.put(candidate.dedupeKey(), candidate);
	}

	/**
	 * @return Key used to deduplicate completions.
	 * Completions with the same key will be merged together, with the one with the lowest rank winning.
	 */
	@Nonnull
	public String dedupeKey() {
		return switch (kind) {
			case TYPE, KEYWORD, LOCAL, PACKAGE -> kind + ":" + insertionText;
			case FIELD, METHOD -> kind + ":" + displayText;
		};
	}

	/**
	 * @return {@code true} when the completion is for a type that is an annotation.
	 */
	public boolean annotationOnly() {
		if (path instanceof ClassPathNode classPath)
			return (classPath.getValue().getAccess() & Opcodes.ACC_ANNOTATION) != 0;
		return false;
	}

	/**
	 * @return Full text to insert when the completion is accepted.
	 */
	@Nonnull
	public String fullInsertionText() {
		return insertionText + trailingSuffix;
	}
}
