package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.Errorable;
import me.coley.recaf.util.struct.Pair;
import java.util.*;

/**
 * Bytecode-focused error handling.
 *
 * @author Matt
 */
public class BytecodeErrorHandling extends ErrorHandling<LineParseException> {
	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public BytecodeErrorHandling(BytecodePane textPane) {
		super(textPane);
	}

	// Update JavaParser problems
	@Override
	public void onCodeChange(String unused, Errorable<LineParseException> errorable) {
		// Clear old problems
		this.problems = Collections.emptyList();
		clearOldEvents();
		// Check if new update thread needs to be spawned
		if(updateThread == null || updateThread.isDone())
			updateThread = new DelayableAction(700, () -> {
				try {
					// Attempt to parse
					errorable.run();
					updateProblems(null);
				} catch(LineParseException ex) {
					// Handle displaying errors
					updateProblems(ex);
					addProblem(ex);
				}
			});
		// Update the current thread so that
		updateThread.resetDelay();
		if(!updateThread.isAlive())
			updateThread.start();
		else
			updateProblems(null);
	}

	/**
	 * Update latest problems.
	 *
	 * @param ex
	 * 		Assembler problem.
	 */
	private void updateProblems(LineParseException ex) {
		Platform.runLater(this::updateProblemLineGraphics);
		// Convert problem to <Line:Message> format
		if(ex == null)
			this.problems = Collections.emptyList();
		else
			this.problems = Arrays.asList(new Pair<>(ex.getLine(), ex.getText()));
	}

	/**
	 * @param ex
	 * 		Assembler problem.
	 */
	private void addProblem(LineParseException ex) {
		// TODO: Error mark the line's text
		// markProblem(line, start, end, literalStart, ex.getMessage());
	}
}
