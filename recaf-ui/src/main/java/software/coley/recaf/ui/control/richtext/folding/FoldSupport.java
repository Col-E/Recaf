package software.coley.recaf.ui.control.richtext.folding;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;
import software.coley.recaf.ui.control.richtext.Editor;

import java.util.List;

/**
 * Shared wiring for {@link Editor} components that source {@link FoldRegion fold regions} from a parsed model.
 * Centralizes {@link FoldTracking}/{@link FoldGutterGraphicFactory} install/uninstall and offset-range to
 * line-region conversion.
 *
 * @author Matt Coley
 * @see FoldTracking
 */
public final class FoldSupport {
	private FoldSupport() {}

	/**
	 * Installs fold tracking and gutter graphics into the given editor.
	 *
	 * @param editor
	 * 		Editor to install into.
	 * @param tracking
	 * 		Fold tracking to register.
	 * @param gutter
	 * 		Gutter graphic factory to register.
	 */
	public static void install(@Nonnull Editor editor, @Nonnull FoldTracking tracking, @Nonnull FoldGutterGraphicFactory gutter) {
		tracking.install(editor);
		editor.setComponent(FoldTracking.COMPONENT_KEY, tracking);
		editor.getRootLineGraphicFactory().addLineGraphicFactory(gutter);
	}

	/**
	 * Uninstalls fold tracking and gutter graphics from the given editor.
	 *
	 * @param editor
	 * 		Editor to uninstall from.
	 * @param tracking
	 * 		Fold tracking to unregister.
	 * @param gutter
	 * 		Gutter graphic factory to unregister.
	 */
	public static void uninstall(@Nonnull Editor editor, @Nonnull FoldTracking tracking, @Nonnull FoldGutterGraphicFactory gutter) {
		tracking.uninstall(editor);
		editor.getRootLineGraphicFactory().removeLineGraphicFactory(gutter);
		editor.setComponent(FoldTracking.COMPONENT_KEY, null);
	}

	/**
	 * Adds a fold region for the given text offset range to the collection, if the resulting region
	 * spans multiple lines.
	 *
	 * @param regions
	 * 		Collection to add to.
	 * @param area
	 * 		Code area to resolve text offsets against.
	 * @param beginOffset
	 * 		Region start offset in the text.
	 * @param endOffset
	 * 		Region end offset in the text.
	 * @param endLineAdjustment
	 * 		End line adjustment, for when the end offset points at content that should stay visible
	 * 		<i>(a closing brace, or the next block's start)</i>.
	 */
	public static void addRegion(@Nonnull List<FoldRegion> regions, @Nonnull CodeArea area,
	                             int beginOffset, int endOffset, int endLineAdjustment) {
		if (beginOffset < 0 || endOffset <= beginOffset)
			return;
		var startLine = 1 + area.offsetToPosition(beginOffset, TwoDimensional.Bias.Forward).getMajor();
		var endLine = 1 + area.offsetToPosition(endOffset, TwoDimensional.Bias.Backward).getMajor() + endLineAdjustment;
		if (endLine > startLine)
			regions.add(new FoldRegion(startLine, endLine));
	}
}
