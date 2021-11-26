package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.VariableReference;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.ui.control.code.IndicatorApplier;
import me.coley.recaf.ui.control.code.IndicatorFactory;

/**
 * Updates the indicator graphics to show which lines reference the same variable that the current
 * line <i>(caret position)</i> references.
 *
 * @author Matt Coley
 */
public class VariableHighlighter implements IndicatorApplier {
	private final AssemblerArea assemblerArea;
	private String targetId;

	/**
	 * @param assemblerArea
	 * 		Target assembler area.
	 */
	public VariableHighlighter(AssemblerArea assemblerArea) {
		this.assemblerArea = assemblerArea;
	}

	/**
	 * @param indicatorFactory
	 * 		Indicator factory to add this highlighter to.
	 */
	public void addIndicator(IndicatorFactory indicatorFactory) {
		indicatorFactory.addIndicatorApplier(this);
	}

	/**
	 * Add selected line listener so that we can be notified of when new selections contain variable refs.
	 *
	 * @param selectedParagraph
	 * 		Observable paragraph index.
	 */
	public void addSelectedLineListener(ObservableValue<Integer> selectedParagraph) {
		selectedParagraph.addListener((observable, oldValue, newValue) -> {
			// The selected paragraph is 0-based, lines are 1-based
			Element elementOnLine = assemblerArea.getElementOnLine(newValue + 1);
			if (elementOnLine instanceof VariableReference) {
				targetId = ((VariableReference) elementOnLine).getVariableIdentifier();
			} else {
				targetId = null;
			}
			refreshParagraphs();
		});
	}


	/**
	 * Any line with variable indicators must be redrawn.
	 */
	private void refreshParagraphs() {
		Unit unit = assemblerArea.getLastUnit();
		if (unit == null)
			return;
		for (AbstractInstruction instruction : unit.getCode().getInstructions()) {
			if (instruction instanceof VariableReference) {
				// Recreate the graphic on the line.
				// Lines are 1-based, but paragraphs are 0-based.
				int line = instruction.getLine();
				if (line > 0)
					assemblerArea.recreateParagraphGraphic(line - 1);
			}
		}
	}

	@Override
	public boolean apply(int lineNo, Polygon poly) {
		Element elementOnLine = assemblerArea.getElementOnLine(lineNo);
		if (elementOnLine instanceof VariableReference) {
			String lineVarId = ((VariableReference) elementOnLine).getVariableIdentifier();
			if (lineVarId.equals(targetId)) {
				poly.setVisible(true);
				poly.setFill(Color.CORNFLOWERBLUE);
				return true;
			}
		}
		return false;
	}
}
