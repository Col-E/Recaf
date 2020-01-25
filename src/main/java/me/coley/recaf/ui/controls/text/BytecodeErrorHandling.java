package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AssemblerException;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.Errorable;
import me.coley.recaf.util.struct.Pair;
import java.util.*;

/**
 * Bytecode-focused error handling.
 *
 * @author Matt
 */
public class BytecodeErrorHandling extends ErrorHandling<AssemblerException> {
	private static final int UPDATE_DELAY = 700;

	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public BytecodeErrorHandling(BytecodePane textPane) {
		super(textPane);
	}

	// Update JavaParser problems
	@Override
	public void onCodeChange(String unused, Errorable<AssemblerException> errorable) {
		// Because we need to clear old handlers for hover-messages
		clearOldEvents();
		// Check if new update thread needs to be spawned
		if(updateThread == null || updateThread.isDone()) {
			updateThread = new DelayableAction(UPDATE_DELAY, () -> {
				Platform.runLater(this::refreshProblemGraphics);
				try {
					// Attempt to parse
					errorable.run();
					updateProblems(null);
				} catch(AssemblerException ex) {
					// Handle displaying errors
					updateProblems(ex);
					addProblem(ex);
				}
				Platform.runLater(this::refreshProblemGraphics);
			});
		}
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
	private void updateProblems(AssemblerException ex) {
		// Convert problem to <Line:Message> format
		if(ex == null)
			setProblems(Collections.emptyList());
		else {
			int line = ex.getLine();
			String msg = ex.getMessage();
			if (line == -1) {
				Throwable exx = ex;
				while(exx.getCause() != null && !(exx instanceof ASTParseException))
					exx = exx.getCause();
				if(exx instanceof ASTParseException)
					line = ((ASTParseException) exx).getLine();
				msg = exx.getMessage();
			}
			setProblems(Collections.singletonList(new Pair<>(line - 1, msg)));
		}
	}

	/**
	 * @param ex
	 * 		Assembler problem.
	 */
	private void addProblem(AssemblerException ex) {
		// TODO: Error mark the line's text
		// markProblem(line, start, end, literalStart, ex.getMessage());
	}
}
