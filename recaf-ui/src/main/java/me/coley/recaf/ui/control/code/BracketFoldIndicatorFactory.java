package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.control.Label;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.util.Icons;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.function.IntFunction;

/**
 * Decorator factory for building bracket/code-folding indicators.
 *
 * @author Matt Coley
 */
public class BracketFoldIndicatorFactory implements IntFunction<Node> {
	private static final String FOLD_ICON_PATH = "icons/fold.png";
	private static final String UNFOLD_ICON_PATH = "icons/unfold.png";
	private final SyntaxArea editor;
	private final BracketTracking bracketTracking;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public BracketFoldIndicatorFactory(SyntaxArea editor) {
		this.editor = editor;
		this.bracketTracking = editor.getBracketTracking();
	}

	@Override
	public Node apply(int lineNo) {
		BracketPair pair = bracketTracking.findBracketOnParagraph(lineNo);
		if (pair != null) {
			if (pair.getEnd() >= editor.getLength())
				return null;
			int startParagraph = editor.offsetToPosition(pair.getStart(), TwoDimensional.Bias.Backward).getMajor();
			int endParagraph = editor.offsetToPosition(pair.getEnd(), TwoDimensional.Bias.Backward).getMajor();
			boolean folded = editor.isParagraphFolded(lineNo + 1);
			// Skip line is unfolded and the paragraphs are the same or just one line
			if (!folded && endParagraph - startParagraph <= 1)
				return null;
			// Check next line since the fold will show the start paragraph, but not the following ones contained
			// in the bracket pair's range.
			if (folded) {
				// Unfolding only needs to know the line where the fold starts.
				return createUnfold(startParagraph);
			} else {
				// We don't pass the end-paragraph since we will want to recompute that.
				return createFold(lineNo);
			}
		}
		return null;
	}

	private Node createFold(int lineNo) {
		Node shape = create(FOLD_ICON_PATH);
		shape.setOnMousePressed(e -> {
			BracketPair pair = bracketTracking.findBracketOnParagraph(lineNo);
			if (pair != null) {
				int startParagraph = editor.offsetToPosition(pair.getStart(), TwoDimensional.Bias.Backward).getMajor();
				int endParagraph = editor.offsetToPosition(pair.getEnd(), TwoDimensional.Bias.Backward).getMajor();
				// Do end-1 so that we can see the end of the brace we closed.
				editor.foldParagraphs(startParagraph, endParagraph - 1);
			}
		});
		return shape;
	}

	private Node createUnfold(int startParagraph) {
		Node shape = create(UNFOLD_ICON_PATH);
		shape.setOnMousePressed(e -> editor.unfoldParagraphs(startParagraph));
		return shape;
	}

	private Node create(String path) {
		IconView icon = Icons.getIconView(path, 10);
		Label wrap = new Label(null, icon);
		wrap.getStyleClass().add("cursor-pointer");
		wrap.getStyleClass().add("fold-wrapper");
		return wrap;
	}
}
