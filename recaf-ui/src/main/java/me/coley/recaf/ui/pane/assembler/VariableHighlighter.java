package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.VariableReference;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.ui.control.code.IndicatorApplier;
import me.coley.recaf.ui.control.code.IndicatorFactory;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the indicator graphics to show which lines reference the same variable that the current
 * line <i>(caret position)</i> references.
 *
 * @author Matt Coley
 */
public class VariableHighlighter implements IndicatorApplier {
	private final List<Integer> linesToRefresh = new ArrayList<>();
	private final ContextualPipeline pipeline;
	private final AssemblerArea assemblerArea;
	private String targetId;

	/**
	 * @param pipeline
	 * 		Assembler pipeline.
	 * @param assemblerArea
	 * 		Target assembler area.
	 */
	public VariableHighlighter(ContextualPipeline pipeline, AssemblerArea assemblerArea) {
		this.pipeline = pipeline;
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
	 * @param selectedCaretPosition
	 * 		Observable caret position.
	 */
	public void addCaretPositionListener(ObservableValue<Integer> selectedCaretPosition) {
		selectedCaretPosition.addListener((observable, oldValue, newValue) -> {
			// Skip if a drag selection is being made
			if (assemblerArea.getSelection().getLength() > 1)
				return;
			// Compute target variable identifier at caret position
			int line = assemblerArea.getCaretLine();
			int col = assemblerArea.getCaretColumn();
			Element elementOnLine = pipeline.getCodeElementAt(line, col);
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
		// Clear old matched ines
		if (linesToRefresh.size() > 0) {
			assemblerArea.regenerateLineGraphics(linesToRefresh);
			linesToRefresh.clear();
		}
		// Redraw new matched lines
		if (!pipeline.isCurrentMethod())
			return;
		for (AbstractInstruction instruction : pipeline.getCurrentMethod().getCode().getInstructions()) {
			if (instruction instanceof VariableReference) {
				int line = instruction.getLine();
				if (line > 0)
					linesToRefresh.add(line);
			}
		}
		assemblerArea.regenerateLineGraphics(linesToRefresh);
	}

	@Override
	public boolean checkModified(int lineNo, Polygon poly) {
		List<Element> elementsOnLine = pipeline.getCodeElementsAt(lineNo);
		for (Element elementOnLine : elementsOnLine) {
			if (elementOnLine instanceof VariableReference) {
				String lineVarId = ((VariableReference) elementOnLine).getVariableIdentifier();
				if (lineVarId.equals(targetId)) {
					poly.setFill(Color.CORNFLOWERBLUE);
					return true;
				}
			}
		}
		return false;
	}
}
