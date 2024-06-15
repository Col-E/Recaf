package software.coley.recaf.ui.control.richtext.linegraphics;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.richtext.CodeArea;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.StringUtil;

/**
 * Graphic factory to draw line numbers.
 *
 * @author Matt Coley
 */
public class LineNumberFactory extends AbstractLineGraphicFactory {
	private CodeArea codeArea;

	/**
	 * New line number factory.
	 */
	public LineNumberFactory() {
		super(P_LINE_NUMBERS);
	}

	@Override
	public void install(@Nonnull Editor editor) {
		codeArea = editor.getCodeArea();
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		codeArea = null;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		if (codeArea == null) return;

		Label label = new Label(format(paragraph + 1, computeDigits(codeArea.getParagraphs().size())));
		label.getStyleClass().add("bg");
		HBox.setHgrow(label, Priority.ALWAYS);
		container.addHorizontal(label);
	}

	@Nonnull
	private static String format(int line, int digits) {
		return String.format(StringUtil.fillLeft(digits, " ", String.valueOf(line)));
	}

	private static int computeDigits(int size) {
		return (int) Math.floor(Math.log10(size)) + 1;
	}
}
