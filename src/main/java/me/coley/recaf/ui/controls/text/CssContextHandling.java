package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

/**
 * Context menu handler for {@link CssThemeEditorPane}.
 *
 * @author Matt
 */
public class CssContextHandling extends ContextHandling {
	/**
	 * @param controller
	 * 		Controller to use.
	 * @param codeArea
	 * 		Controller to pull info from.
	 */
	public CssContextHandling(GuiController controller, CodeArea codeArea) {
		super(controller, codeArea);
	}

	@Override
	protected Object getSelection(TwoDimensional.Position pos) {
		return null;
	}

	@Override
	protected Object getCurrentSelection() {
		return null;
	}
}
