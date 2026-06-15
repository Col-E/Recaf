package software.coley.recaf.ui.control.richtext.folding;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.Editor;

/**
 * A range of lines in an {@link Editor} that can be collapsed.
 * <br>
 * When collapsed, the {@link #startLine()} remains visible and all following lines (including {@link #endLine()}) are
 * hidden.
 *
 * @param startLine
 * 		Header line of the region, 1-indexed.
 * @param endLine
 * 		Last line of the region, 1-indexed, inclusive.
 */
public record FoldRegion(int startLine, int endLine) implements Comparable<FoldRegion> {
	/**
	 * @param lines
	 * 		Number of lines to shift by.
	 *
	 * @return Shifted copy of this region.
	 */
	@Nonnull
	public FoldRegion shifted(int lines) {
		return new FoldRegion(startLine + lines, endLine + lines);
	}

	/**
	 * @param lines
	 * 		Number of lines to grow <i>(or shrink when negative)</i> the region by.
	 *
	 * @return Extended copy of this region.
	 */
	@Nonnull
	public FoldRegion extended(int lines) {
		return new FoldRegion(startLine, endLine + lines);
	}

	@Override
	public int compareTo(FoldRegion o) {
		int cmp = Integer.compare(startLine, o.startLine);
		if (cmp == 0)
			cmp = Integer.compare(endLine, o.endLine);
		return cmp;
	}
}
