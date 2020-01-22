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
		// Clear old problems
		this.problems = Collections.emptyList();
		clearOldEvents();
		// Check if new update thread needs to be spawned
		if(updateThread == null || updateThread.isDone())
			updateThread = new DelayableAction(700, () -> {
				Platform.runLater(this::clearProblemLines);
				try {
					// Attempt to parse
					errorable.run();
					updateProblems(null);
				} catch(AssemblerException ex) {
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
	private void updateProblems(AssemblerException ex) {
		// Convert problem to <Line:Message> format
		if(ex == null)
			this.problems = Collections.emptyList();
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
			this.problems = Arrays.asList(new Pair<>(line - 1, msg));
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
