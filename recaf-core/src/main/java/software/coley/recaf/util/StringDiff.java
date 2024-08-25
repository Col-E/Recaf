package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.jgit.diff.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Wrapping around JGit diff logic.
 *
 * @author Matt Coley
 */
public class StringDiff {
	/**
	 * @param a
	 * 		Input.
	 * @param b
	 * 		Modified input.
	 *
	 * @return Diffs from {@code a} --> {@code b}.
	 */
	@Nonnull
	public static List<Diff> diff(@Nonnull String a, @Nonnull String b) {
		// Use JGit to diff.
		EditList diffs = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS)
				.diff(RawTextComparator.DEFAULT,
						new RawText(a.getBytes(StandardCharsets.UTF_8)),
						new RawText(b.getBytes(StandardCharsets.UTF_8)));

		int[] newlineOffsetsA = computeNewlineOffsets(a);
		int[] newlineOffsetsB = computeNewlineOffsets(b);

		// Map to diff types that are easier to use.
		// We want raw positions, not line numbers, so we can do replace operations easily.
		List<Diff> ret = new ArrayList<>(diffs.size());
		for (Edit diff : diffs) {
			int newlineStartA = diff.getBeginA(); // Line start of diff-A
			int newlineEndA = diff.getEndA(); // Line end of diff-A
			int offsetStartA = newlineOffsetsA[newlineStartA]; // Starting text offset of diff-A
			int offsetEndA = newlineOffsetsA[newlineEndA]; // Ending text offset of diff-A
			String contentA = a.substring(offsetStartA, offsetEndA);

			int newlineStartB = diff.getBeginB(); // Line start of diff-B
			int newlineEndB = diff.getEndB(); // Line end of diff-B
			int offsetStartB = newlineOffsetsB[newlineStartB]; // Starting text offset of diff-B
			int offsetEndB = newlineOffsetsB[newlineEndB]; // Ending text offset of diff-B
			String contentB = b.substring(offsetStartB, offsetEndB);

			// Shrink difference by trimming out the common prefix and suffix.
			String commonPrefix = StringUtil.getCommonPrefix(contentA, contentB);
			if (!commonPrefix.isEmpty()) {
				int common = commonPrefix.length();
				offsetStartA += common;
				offsetStartB += common;
				contentA = a.substring(offsetStartA, offsetEndA);
				contentB = b.substring(offsetStartB, offsetEndB);
			}
			String commonSuffix = StringUtil.getCommonSuffix(contentA, contentB);
			if (!commonSuffix.isEmpty()) {
				int common = commonSuffix.length();
				offsetEndA -= common;
				offsetEndB -= common;
				contentA = a.substring(offsetStartA, offsetEndA);
				contentB = b.substring(offsetStartB, offsetEndB);
			}

			// Compute the diff type. It will have changed from what JGit reports if our
			// shrinking has shrunk either 'A' or 'B' to an empty string.
			DiffType type = DiffType.from(diff.getType());
			if (contentA.isEmpty() && !contentB.isEmpty())
				type = DiffType.INSERT;
			else if (!contentA.isEmpty() && contentB.isEmpty())
				type = DiffType.REMOVE;

			// Add our mapped diff.
			ret.add(new Diff(type, offsetStartA, offsetStartB, offsetEndA, offsetEndB, contentA, contentB));
		}
		return ret;
	}

	/**
	 * @param text
	 * 		Text to scan.
	 *
	 * @return Offsets of newline separators, with included cases for the start and end of
	 * the string even if {@code "\n"} does not prefix or suffix the string.
	 */
	@Nonnull
	private static int[] computeNewlineOffsets(@Nonnull String text) {
		// Find existing newline offsets.
		int[] offsets = StringUtil.indicesOf(text, '\n');

		int textLength = text.length();
		if (offsets.length == 0) {
			// Base cases for strings without newlines.
			return text.isEmpty() ? new int[]{0} : new int[]{0, textLength};
		} else {
			// Ensure we start with a newline offset.
			if (offsets[0] != 0) {
				int[] newOffsets = new int[offsets.length + 1];
				newOffsets[0] = 0;
				System.arraycopy(offsets, 0, newOffsets, 1, offsets.length);
				offsets = newOffsets;
			}

			// Ensure we end with a newline offset.
			if (offsets[offsets.length - 1] != textLength - 1) {
				int[] newOffsets = new int[offsets.length + 1];
				newOffsets[offsets.length] = textLength;
				System.arraycopy(offsets, 0, newOffsets, 0, offsets.length);
				offsets = newOffsets;
			}
		}

		return offsets;
	}

	/**
	 * @param type
	 * 		Diff type.
	 * @param startA
	 * 		Start position of original text.
	 * @param startB
	 * 		Start position of modified text.
	 * @param endA
	 * 		End position of original text.
	 * @param endB
	 * 		End position of modified text.
	 * @param textA
	 * 		Original text in range.
	 * @param textB
	 * 		Modified text in range.
	 *
	 * @author Matt Coley
	 */
	public record Diff(@Nonnull DiffType type, int startA, int startB, int endA, int endB,
	                   @Nonnull String textA, @Nonnull String textB) {
		/**
		 * Compare diffs by start positions.
		 */
		public static Comparator<Diff> COMPARE_BY_START_OFFSET = (da, db) -> {
			int cmp = Integer.compare(da.startA, db.startA);
			if (cmp == 0)
				cmp = -Integer.compare(da.endA, db.endA);
			if (cmp == 0)
				cmp = da.textA.compareTo(db.textB);
			return cmp;
		};

		/**
		 * @return {@code true} when {@link #type()} is {@link DiffType#CHANGE}.
		 */
		public boolean isReplace() {
			return type == DiffType.CHANGE;
		}

		/**
		 * @return {@code true} when {@link #type()} is {@link DiffType#REMOVE}.
		 */
		public boolean isRemoval() {
			return type == DiffType.REMOVE;
		}

		/**
		 * @return {@code true} when {@link #type()} is {@link DiffType#INSERT}.
		 */
		public boolean isInsert() {
			return type == DiffType.INSERT;
		}

		/**
		 * @return {@code true} when {@link #type()} is {@link DiffType#EMPTY}.
		 */
		public boolean isEmpty() {
			return type == DiffType.EMPTY;
		}

		/**
		 * @param pos
		 * 		Position to check.
		 *
		 * @return {@code true} when {@code pos} is in the range {@code [startA, endA)}.
		 */
		public boolean inRangeA(int pos) {
			return pos >= startA && pos < endA;
		}

		/**
		 * @param pos
		 * 		Position to check.
		 *
		 * @return {@code true} when {@code pos} is in the range {@code [startB, endB)}.
		 */
		public boolean inRangeB(int pos) {
			return pos >= startB && pos < endB;
		}

		/**
		 * @param inputA
		 * 		Full text for input "A".
		 *
		 * @return Patched text to conform to input "B".
		 */
		@Nonnull
		public String apply(@Nonnull String inputA) {
			String prefix = inputA.substring(0, startA);
			String suffix = inputA.substring(endA);
			return prefix + textB + suffix;
		}

		/**
		 * @param inputA
		 * 		Full text for input "A".
		 * @param diffs
		 * 		Series of diffs to apply.
		 *
		 * @return Patched text to conform to input "B".
		 */
		@Nonnull
		public static String apply(@Nonnull String inputA, @Nonnull Collection<Diff> diffs) {
			List<Diff> sortedDiffs = diffs.stream().sorted(COMPARE_BY_START_OFFSET).toList();
			for (int i = sortedDiffs.size() - 1; i >= 0; i--)
				inputA = sortedDiffs.get(i).apply(inputA);
			return inputA;
		}
	}

	/**
	 * Type of diff.
	 *
	 * @author Matt Coley
	 */
	public enum DiffType {
		CHANGE,
		INSERT,
		REMOVE,
		EMPTY;

		/**
		 * @param type
		 * 		JGit diff type.
		 *
		 * @return Our diff type.
		 */
		@Nonnull
		public static DiffType from(@Nonnull Edit.Type type) {
			return switch (type) {
				case INSERT -> INSERT;
				case DELETE -> REMOVE;
				case REPLACE -> CHANGE;
				case EMPTY -> EMPTY;
			};
		}
	}

	/**
	 * Helper that holds diff information between the original text, and the test that the tree believes it is modeling.
	 * In some instances these models may not be aligned and will have to be de-conflicted.
	 */
	public static class DiffHelper {
		private final List<StringDiff.Diff> diffs;

		/**
		 * @param backingText
		 * 		Original text that yields the tree.
		 * @param tree
		 * 		Tree model to compare against. Will be turned into a string representation.
		 */
		public DiffHelper(@Nonnull String backingText, @Nonnull Tree tree) {
			String treePrintText = tree.print(new Cursor(null, tree));
			this.diffs = StringDiff.diff(backingText, treePrintText);
		}

		/**
		 * Replacements can be detected in both the original and AST text representations.
		 * Thus, we can search in the 'b' ranges, which are those belonging to the AST and still maintain
		 * the ability to grab a result.
		 *
		 * @param pos
		 * 		Position in the AST's version of the text.
		 *
		 * @return Replace diff encompassing the given position,
		 * or {@code null} if no replace diff wraps the requested range.
		 */
		@Nullable
		public StringDiff.Diff getReplaced(int pos) {
			for (StringDiff.Diff diff : diffs) {
				if (diff.isReplace() && diff.inRangeB(pos)) {
					return diff;
				}
			}
			return null;
		}

		/**
		 * Removals can only be detected in the original text representation.
		 * Thus, we will search in the 'a' ranges, which are those belonging to the original backing text.
		 *
		 * @param pos
		 * 		Position in the original backing version of the text.
		 *
		 * @return Removal diff encompassing the given position,
		 * or {@code null} if no removal diff wraps the requested range.
		 */
		@Nullable
		public StringDiff.Diff getRemoved(int pos) {
			for (StringDiff.Diff diff : diffs) {
				if (diff.isRemoval() && diff.inRangeA(pos)) {
					return diff;
				}
			}
			return null;
		}

		/**
		 * Insertions can only be detected in the AST's text representation.
		 * Thus, we will search in the 'b' ranges, which are those belonging to the AST.
		 *
		 * @param pos
		 * 		Position in the AST's version of the text.
		 *
		 * @return Insertion diff encompassing the given position,
		 * or {@code null} if no insertion diff wraps the requested range.
		 */
		@Nullable
		public StringDiff.Diff getInserted(int pos) {
			for (StringDiff.Diff diff : diffs) {
				if (diff.isInsert() && diff.inRangeB(pos)) {
					return diff;
				}
			}
			return null;
		}
	}
}
