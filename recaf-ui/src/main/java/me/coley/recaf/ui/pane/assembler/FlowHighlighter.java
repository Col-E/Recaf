package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.ui.control.code.IndicatorApplier;
import me.coley.recaf.ui.control.code.IndicatorFactory;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Updates the indicator graphics to show which lines are sources/destinations of a flow control instruction
 * that the current line <i>(caret position)</i> references.
 *
 * @author Matt Coley
 */
public class FlowHighlighter implements IndicatorApplier {
	private final Set<FlowControl> sources = new HashSet<>();
	private final Set<String> destinations = new HashSet<>();
	private final List<Integer> linesToRefresh = new ArrayList<>();
	private final ContextualPipeline pipeline;
	private final AssemblerArea assemblerArea;

	/**
	 * @param pipeline
	 * 		Assembler pipeline.
	 * @param assemblerArea
	 * 		Target assembler area.
	 */
	public FlowHighlighter(ContextualPipeline pipeline, AssemblerArea assemblerArea) {
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
	 * Add selected line listener so that we can be notified of when new selections contain flow refs.
	 *
	 * @param selectedCaretPosition
	 * 		Observable caret position.
	 */
	public void addCaretPositionListener(ObservableValue<Integer> selectedCaretPosition) {
		selectedCaretPosition.addListener((observable, oldValue, newValue) -> {
			// Skip if a drag selection is being made
			if (assemblerArea.getSelection().getLength() > 1)
				return;
			// Compute the flow initial point at the caret position, and collect destinations
			sources.clear();
			destinations.clear();
			int line = assemblerArea.getCaretLine();
			int col = assemblerArea.getCaretColumn();
			Element elementOnLine = pipeline.getCodeElementAt(line, col);
			if (!pipeline.isCurrentMethod())
				return;
			Code code = pipeline.getCurrentMethod().getCode();
			if (elementOnLine instanceof FlowControl) {
				try {
					Map<String, Label> labelMap = code.getLabels();
					List<String> labelNames = ((FlowControl) elementOnLine)
							.getTargets(labelMap)
							.stream()
							.map(Label::getName)
							.collect(Collectors.toList());
					destinations.addAll(labelNames);
				} catch (IllegalAstException ex) {
					// ignored
				}
			} else if (elementOnLine instanceof Label) {
				Map<String, Label> labelMap = code.getLabels();
				List<FlowControl> matched = code.getChildrenOfType(FlowControl.class).stream()
						.filter(flow -> {
							try {
								return flow.getTargets(labelMap).contains(elementOnLine);
							} catch (Throwable t) {
								return false;
							}
						}).collect(Collectors.toList());
				sources.addAll(matched);
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
		Code code = pipeline.getCurrentMethod().getCode();
		// Need to refresh flow control instructions
		for (FlowControl flow : code.getChildrenOfType(FlowControl.class)) {
			int line = flow.getLine();
			if (line > 0)
				linesToRefresh.add(line);
		}
		// Also need to refresh labels themselves
		for (Label label : code.getLabels().values()) {
			int line = label.getLine();
			if (line > 0)
				linesToRefresh.add(line);
		}
		assemblerArea.regenerateLineGraphics(linesToRefresh);
	}

	@Override
	public boolean checkModified(int lineNo, Polygon poly) {
		List<Element> elementsOnLine = pipeline.getCodeElementsAt(lineNo);
		for (Element elementOnLine : elementsOnLine) {
			if (elementOnLine instanceof Label) {
				String labelId = ((Label) elementOnLine).getName();
				if (destinations.contains(labelId)) {
					poly.setFill(Color.GREEN);
					return true;
				}
			} else if (elementOnLine instanceof FlowControl) {
				if (sources.contains(elementOnLine)) {
					poly.setFill(Color.LIMEGREEN);
					return true;
				}
			}
		}
		return false;
	}
}
