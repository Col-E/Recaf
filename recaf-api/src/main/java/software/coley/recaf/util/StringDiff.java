package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.shaded.jgit.diff.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
	public static List<Diff> diff(String a, String b) {
		// Track line info.
		String[] aLines = a.split("\n");
		String[] bLines = b.split("\n");
		int[] aPositions = new int[aLines.length + 1];
		int[] bPositions = new int[bLines.length + 1];
		for (int i = 0; i < aPositions.length - 2; i++)
			aPositions[i + 1] = aPositions[i] + aLines[i].length() + 1;
		for (int i = 0; i < bPositions.length - 2; i++)
			bPositions[i + 1] = bPositions[i] + bLines[i].length() + 1;
		aPositions[aLines.length] = Integer.MAX_VALUE;
		bPositions[bLines.length] = Integer.MAX_VALUE;

		// Use JGit to diff.
		EditList diffs = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS)
				.diff(RawTextComparator.DEFAULT,
						new RawText(a.getBytes(StandardCharsets.UTF_8)),
						new RawText(b.getBytes(StandardCharsets.UTF_8)));

		// Map to diff types that are easier to use.
		// We want raw positions, not line numbers, so we can do replace operations easily.
		List<Diff> ret = new ArrayList<>();
		for (Edit diff : diffs) {
			DiffType type = DiffType.from(diff.getType());
			int beginA = diff.getBeginA();
			int endA = diff.getEndA();
			int posStartA = aPositions[beginA];
			int posEndA = aPositions[endA];
			String contentA = pull(aLines, beginA, endA);
			int beginB = diff.getBeginB();
			int endB = diff.getEndB();
			int posStartB = bPositions[beginB];
			int posEndB = bPositions[endB];
			String contentB = pull(bLines, beginB, endB);

			// Shrink difference
			int lengthA = contentA.length();
			int lengthB = contentB.length();
			int commonLength = Math.min(lengthA, lengthB);
			int cutStart = 0;
			while (cutStart < commonLength && contentA.charAt(cutStart) == contentB.charAt(cutStart))
				cutStart++;
			int cutEnd = 0;
			while (cutEnd < commonLength && contentA.charAt(lengthA - cutEnd - 1) == contentB.charAt(lengthB - cutEnd - 1))
				cutEnd++;
			if (cutStart > 0) {
				posStartA += cutStart;
				posStartB += cutStart;
				contentA = contentA.substring(cutStart);
				contentB = contentB.substring(cutStart);
			}
			if (cutEnd > 0) {
				posEndA -= cutEnd;
				posEndB -= cutEnd;
				contentA = contentA.substring(0, contentA.length() - cutEnd);
				contentB = contentB.substring(0, contentB.length() - cutEnd);
			}

			// Check if shrinking changed type
			if (contentA.isEmpty() && !contentB.isEmpty())
				type = DiffType.INSERT;
			else if (!contentA.isEmpty() && contentB.isEmpty())
				type = DiffType.REMOVE;

			ret.add(new Diff(type, posStartA, posStartB, posEndA, posEndB, contentA, contentB));
		}
		return ret;
	}

	/**
	 * @param array
	 * 		Sources.
	 * @param start
	 * 		Source line start.
	 * @param end
	 * 		Source line end.
	 *
	 * @return Text from range.
	 */
	private static String pull(String[] array, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(array[i]).append("\n");
		return sb.toString();
	}

	/**
	 * @param type
	 * 		Diff type.
	 * @param startA
	 * 		Start position of original different text.
	 * @param startB
	 * 		Start position of modified different text.
	 * @param endA
	 * 		End position of original different text.
	 * @param endB
	 * 		End position of modified different text.
	 * @param textA
	 * 		Original text in range.
	 * @param textB
	 * 		Modified text in range.
	 *
	 * @author Matt Coley
	 */
	public record Diff(DiffType type, int startA, int startB, int endA, int endB, String textA, String textB) {
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

		public boolean inRangeA(int pos) {
			return pos >= startA && pos < endA;
		}

		public boolean inRangeB(int pos) {
			return pos >= startB && pos < endB;
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
		public static DiffType from(Edit.Type type) {
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
