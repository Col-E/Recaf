package me.coley.recaf.ui.controls.text;

import me.coley.recaf.util.struct.Pair;

import java.util.Collections;

/**
 * Css-focused error handling.
 *
 * @author Matt
 */
public class CssErrorHandling extends ErrorHandling {
	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public CssErrorHandling(CssPane textPane) {
		super(textPane);
	}

	@Override
	protected void handleCodeChangeError(Throwable ex) {
		// TODO: Create proper handling
		setProblems(Collections.singletonList(new Pair<>(-1, ex.getMessage())));
	}
}
