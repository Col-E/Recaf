package me.coley.recaf.ui.controls.text;

import me.coley.recaf.util.struct.Pair;
import org.w3c.css.sac.CSSParseException;

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
	public CssErrorHandling(CssThemeEditorPane textPane) {
		super(textPane);
	}

	@Override
	protected void handleCodeChangeError(Throwable ex) {
		if (ex == null)
			// Clear displayed errors
			updateProblem(null);
		else if (ex instanceof CSSParseException) {
			// Handle displaying errors
			updateProblem((CSSParseException)ex);
		}
	}

	/**
	 * Update problem.
	 */
	private void updateProblem(CSSParseException ex) {
		// No problem.
		if (ex == null) {
			setProblems(Collections.emptyList());
			return;
		}
		// Convert problem to <Line:Message> format
		//  - Yeah, line needs a -2 offset for some reason.
		setProblems(Collections.singletonList(new Pair<>(ex.getLineNumber() - 2, ex.getMessage())));
	}
}
