package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.ui.control.IconView;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.function.IntFunction;

public class BracketFoldIndicatorFactory implements IntFunction<Node> {
	private static final double SIZE = 10;
	private static final double[] SHAPE = new double[]{
			0, 0,
			SIZE, 0,
			SIZE, SIZE,
			0, SIZE};
	private final SyntaxArea editor;
	private final BracketSupport bracketSupport;

	/**
	 * @param editor
	 * 		The editor context.
	 */
	public BracketFoldIndicatorFactory(SyntaxArea editor) {
		this.editor = editor;
		this.bracketSupport = editor.getBracketSupport();
	}

	@Override
	public Node apply(int lineNo) {
		BracketPair pair = bracketSupport.findBracketOnParagraph(lineNo);
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
		Node shape = create("icons/fold.png");
		shape.setOnMousePressed(e -> {
			BracketPair pair = bracketSupport.findBracketOnParagraph(lineNo);
			if (pair != null) {
				int startParagraph = editor.offsetToPosition(pair.getStart(), TwoDimensional.Bias.Backward).getMajor();
				int endParagraph = editor.offsetToPosition(pair.getEnd(), TwoDimensional.Bias.Backward).getMajor();
				editor.foldParagraphs(startParagraph, endParagraph - 1);
			}
		});
		return shape;
	}

	private Node createUnfold(int startParagraph) {
		Node shape = create("icons/unfold.png");
		shape.setOnMousePressed(e -> editor.unfoldParagraphs(startParagraph));
		return shape;
	}

	private Node create(String path) {
		IconView icon = new IconView(path, 10);
		Label wrap = new Label(null, icon);
		wrap.getStyleClass().add("cursor-pointer");
		wrap.getStyleClass().add("fold-wrapper");
		return wrap;
	}
}
