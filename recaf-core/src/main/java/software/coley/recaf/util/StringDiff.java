package software.coley.recaf.util;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
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
		int[] newlineOffsetsA = computeNewlineOffsets(a);
		int[] newlineOffsetsB = computeNewlineOffsets(b);
		List<String> aa = Arrays.asList(StringUtil.splitNewline(a));
		List<String> bb = Arrays.asList(StringUtil.splitNewline(b));
		Patch<String> stringPatch = DiffUtils.diff(aa, bb);
		List<Diff> diffs = new ArrayList<>();
		for (AbstractDelta<String> delta : stringPatch.getDeltas()) {
			if (delta.getType() == DeltaType.EQUAL)
				continue;
			int newlineStartA = delta.getSource().getPosition();
			int newlineStartB = delta.getTarget().getPosition();
			int offsetStartA = newlineOffsetsA[newlineStartA];
			int offsetStartB = newlineOffsetsB[newlineStartB];
			int offsetEndA = newlineOffsetsA[Math.max(newlineStartA + 1, delta.getSource().last() + 1)];
			int offsetEndB = newlineOffsetsB[Math.max(newlineStartB + 1, delta.getTarget().last() + 1)];
			String contentA = a.substring(offsetStartA, offsetEndA);
			String contentB = b.substring(offsetStartB, offsetEndB);

			// Shrink difference by trimming out the common prefix and suffix.
			String commonPrefix = StringUtil.getCommonPrefix(contentA, contentB);
			if (!commonPrefix.isEmpty()) {
				int common = commonPrefix.length();
				offsetStartA += common;
				offsetStartB += common;
				contentA = contentA.substring(common);
				contentB = contentB.substring(common);
			}
			String commonSuffix = StringUtil.getCommonSuffix(contentA, contentB);
			if (!commonSuffix.isEmpty()) {
				int common = commonSuffix.length();
				offsetEndA -= common;
				offsetEndB -= common;
				contentA = contentA.substring(0, contentA.length() - common);
				contentB = contentB.substring(0, contentB.length() - common);
			}

			// Compute the diff type. It will have changed from what JGit reports if our
			// shrinking has shrunk either 'A' or 'B' to an empty string.
			DiffType type = DiffType.from(delta.getType());
			if (contentA.isEmpty() && !contentB.isEmpty())
				type = DiffType.INSERT;
			else if (!contentA.isEmpty() && contentB.isEmpty())
				type = DiffType.REMOVE;

			// Add our mapped diff.
			diffs.add(new Diff(type, offsetStartA, offsetStartB, offsetEndA, offsetEndB, contentA, contentB));
		}
		return diffs;
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
		 * 		Delta type.
		 *
		 * @return Our diff type.
		 */
		@Nonnull
		public static DiffType from(@Nonnull DeltaType type) {
			return switch (type) {
				case INSERT -> INSERT;
				case DELETE -> REMOVE;
				case CHANGE -> CHANGE;
				case EQUAL -> EMPTY;
			};
		}
	}
}
