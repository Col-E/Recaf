package software.coley.recaf.ui.control.richtext.folding;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Tracking for foldable line regions to display in an {@link Editor}.
 */
public class FoldTracking implements EditorComponent, Consumer<PlainTextChange> {
	public static final String COMPONENT_KEY = "fold-tracking";

	private static final DebuggingLogger logger = Logging.get(FoldTracking.class);

	private final NavigableMap<Integer, FoldRegion> regions = new TreeMap<>();
	private Editor editor;

	/**
	 * Replace all tracked regions.
	 *
	 * @param newRegions
	 * 		Regions to track.
	 */
	public void setRegions(@Nonnull Collection<FoldRegion> newRegions) {
		synchronized (regions) {
			regions.clear();
			for (var region : newRegions) {
				if (region.endLine() > region.startLine())
					regions.merge(region.startLine(), region, (a, b) -> a.endLine() >= b.endLine() ? a : b);
			}
		}
	}

	/**
	 * Clear all tracked regions.
	 */
	public void clear() {
		synchronized (regions) {
			regions.clear();
		}
	}

	public boolean isEmpty() {
		synchronized (regions) {
			return regions.isEmpty();
		}
	}

	/**
	 * @param line
	 * 		Line number, 1-indexed.
	 *
	 * @return Region with the given line as its header, if found.
	 */
	@Nullable
	public FoldRegion getRegionStartingAt(int line) {
		synchronized (regions) {
			return regions.get(line);
		}
	}

	@Override
	public void accept(PlainTextChange change) {
		if (editor == null || isEmpty())
			return;

		try {
			var lineInserted = change.getInserted().contains("\n");
			var lineRemoved = change.getRemoved().contains("\n");

			if (lineRemoved) {
				var lastDocumentSnapshot = editor.getLastDocumentSnapshot();
				var start = lastDocumentSnapshot.offsetToPosition(change.getPosition(), TwoDimensional.Bias.Backward).getMajor() + 1;
				var end = lastDocumentSnapshot.offsetToPosition(change.getRemovalEnd(), TwoDimensional.Bias.Backward).getMajor() + 1;
				onLinesRemoved(start, end);
			}
			if (lineInserted) {
				var start = editor.getCodeArea().offsetToPosition(change.getPosition(), TwoDimensional.Bias.Backward).getMajor() + 1;
				var end = editor.getCodeArea().offsetToPosition(change.getInsertionEnd(), TwoDimensional.Bias.Backward).getMajor();
				onLinesInserted(start, end);
			}
		} catch (Throwable t) {
			logger.error("Error updating fold regions on text change", t);
		}
	}

	/**
	 * Shifts or grows regions to account for inserted lines.
	 *
	 * @param startLine
	 * 		Starting range of lines inserted, inclusive.
	 * @param endLine
	 * 		Ending range of lines inserted, inclusive.
	 */
	private void onLinesInserted(int startLine, int endLine) {
		var shift = 1 + endLine - startLine;
		remapRegions(region -> {
			if (region.endLine() < startLine)
				return region;
			if (region.startLine() >= startLine)
				return region.shifted(shift);

			// Insertion within the body -> grow.
			return region.extended(shift);
		});
	}

	/**
	 * Shifts or shrinks regions to account for removed lines.
	 *
	 * @param startLine
	 * 		Starting range of lines removed, inclusive.
	 * @param endLine
	 * 		Ending range of lines removed, exclusive.
	 */
	private void onLinesRemoved(int startLine, int endLine) {
		var shift = endLine - startLine;
		remapRegions(region -> {
			if (region.endLine() < startLine)
				return region;
			if (region.startLine() > endLine)
				return region.shifted(-shift);

			// Removal within the body -> shrink
			if (region.startLine() < startLine && region.endLine() >= endLine) {
				var shrunk = region.extended(-shift);
				return shrunk.endLine() > shrunk.startLine() ? shrunk : null;
			}

			// Removal crosses a boundary -> drop.
			return null;
		});
	}

	/**
	 * @param mapper
	 * 		Mapping function.
	 */
	private void remapRegions(@Nonnull UnaryOperator<FoldRegion> mapper) {
		synchronized (regions) {
			var updated = new TreeMap<Integer, FoldRegion>();
			for (var region : regions.values()) {
				var mapped = mapper.apply(region);
				if (mapped != null && mapped.endLine() > mapped.startLine())
					updated.merge(mapped.startLine(), mapped, (a, b) -> a.endLine() >= b.endLine() ? a : b);
			}
			regions.clear();
			regions.putAll(updated);
		}
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		clear();
		editor.getTextChangeEventStream().addObserver(this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
		clear();
		editor.getTextChangeEventStream().removeObserver(this);
	}
}
