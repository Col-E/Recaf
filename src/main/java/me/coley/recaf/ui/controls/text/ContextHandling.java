package me.coley.recaf.ui.controls.text;

import javafx.scene.input.MouseButton;
import me.coley.recaf.control.gui.GuiController;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.function.Consumer;

/**
 * Generic context handler.
 *
 * @author Matt
 */
public abstract class ContextHandling {
	protected final GuiController controller;
	protected final CodeArea codeArea;
	private Consumer<Object> consumer;

	/**
	 * @param controller
	 * 		Controller to use.
	 * @param codeArea
	 * 		Text editor events originate from.
	 */
	public ContextHandling(GuiController controller, CodeArea codeArea) {
		this.codeArea = codeArea;
		this.controller = controller;
		codeArea.setOnMousePressed(e -> {
			// Only accept right-click presses
			if (e.getButton() != MouseButton.SECONDARY)
				return;
			// Reset
			codeArea.setContextMenu(null);
			// Mouse to location
			CharacterHit hit = codeArea.hit(e.getX(), e.getY());
			int charPos = hit.getInsertionIndex();
			codeArea.getCaretSelectionBind().displaceCaret(charPos);
			TwoDimensional.Position pos = codeArea.offsetToPosition(charPos,
					TwoDimensional.Bias.Backward);
			// Get selection
			Object selection = getSelection(pos);
			if (selection == null)
				return;
			if (consumer != null)
				consumer.accept(selection);
		});
	}

	/**
	 * @param consumer
	 * 		Action to take when the user right-clicks, given some selected content.
	 */
	protected void onContextRequest(Consumer<Object> consumer) {
		this.consumer = consumer;
	}

	/**
	 * @param pos
	 * 		Some position <i>(line/column)</i>
	 *
	 * @return Object at position. May be {@code null}.
	 */
	protected abstract Object getSelection(TwoDimensional.Position pos);
}
