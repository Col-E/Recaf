package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

/**
 * Context menu handler for {#link BytecodePane}.
 *
 * @author Matt
 */
public class BytecodeContextHandling extends ContextHandling {
	/**
	 * @param controller
	 * 		Controller to use.
	 * @param codeArea
	 * 		Controller to pull info from.
	 */
	public BytecodeContextHandling(GuiController controller, CodeArea codeArea) {
		super(controller, codeArea);
	}

	@Override
	protected Object getSelection(TwoDimensional.Position pos) {
		return null;
	}
}
