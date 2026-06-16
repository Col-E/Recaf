package software.coley.recaf.ui.control.richtext.folding;

import jakarta.annotation.Nonnull;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;

/**
 * Graphic factory that adds collapse/expand arrows to lines that begin a {@link FoldRegion}.
 *
 * @author SooStrator
 */
public class FoldGutterGraphicFactory extends AbstractLineGraphicFactory {
	private static final double DEFAULT_FONT_SIZE = 12;
	private static final double ICON_SCALE = 16.0 / DEFAULT_FONT_SIZE;
	private static final double GUTTER_SCALE = 14.0 / 16.0;

	private Editor editor;

	private double cachedFontSize = -1;

	public FoldGutterGraphicFactory() {
		super(LineGraphicFactory.P_LINE_FOLDING);
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		cachedFontSize = -1;
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
		cachedFontSize = -1;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		FoldTracking foldTracking = (FoldTracking) editor.getComponent(FoldTracking.COMPONENT_KEY);
		if (foldTracking == null)
			return;

		boolean collapsedHeader = editor.isParagraphFolded(paragraph + 1) && !editor.isParagraphFolded(paragraph);
		if (foldTracking.isEmpty() && !collapsedHeader)
			return;

		FoldRegion region = foldTracking.getRegionStartingAt(paragraph + 1);

		// Scale the arrow and gutter with the editor font size.
		int iconSize = resolveIconSize();
		double gutterWidth = resolveGutterWidth(iconSize);

		HBox box = new HBox();
		box.setAlignment(Pos.CENTER_LEFT);
		box.setMinWidth(gutterWidth);
		box.setMaxWidth(gutterWidth);
		box.setPickOnBounds(true);
		if (collapsedHeader) {
			FontIconView icon = new FontIconView(CarbonIcons.CHEVRON_DOWN, iconSize);
			icon.setRotate(-90);
			icon.setOpacity(0.75);
			box.getChildren().add(icon);
			box.setCursor(Cursor.HAND);
			box.setOnMousePressed(e -> {
				if (e.getButton() != MouseButton.PRIMARY)
					return;

				e.consume();
				editor.unfoldParagraphs(paragraph);
				editor.redrawParagraphGraphics();
			});
		} else if (region != null) {
			FontIconView icon = new FontIconView(CarbonIcons.CHEVRON_DOWN, iconSize);
			icon.setOpacity(0.3);
			box.getChildren().add(icon);
			box.setCursor(Cursor.HAND);
			box.setOnMousePressed(e -> {
				if (e.getButton() != MouseButton.PRIMARY)
					return;

				e.consume();
				editor.foldParagraphs(region.startLine() - 1, region.endLine() - 1);
				editor.redrawParagraphGraphics();
			});
		}
		container.addHorizontal(box);
	}

	private int resolveIconSize() {
		return (int) Math.round(resolveFontSize() * ICON_SCALE);
	}

	private double resolveGutterWidth(int iconSize) {
		return Math.ceil(iconSize * GUTTER_SCALE);
	}

	private double resolveFontSize() {
		if (cachedFontSize > 0)
			return cachedFontSize;

		if (editor != null) {
			for (Node node : editor.getCodeArea().lookupAll(".text")) {
				if (node instanceof Text text) {
					var size = text.getFont().getSize();
					if (size > 0) {
						cachedFontSize = size;
						return size;
					}
				}
			}
		}

		return DEFAULT_FONT_SIZE;
	}
}
