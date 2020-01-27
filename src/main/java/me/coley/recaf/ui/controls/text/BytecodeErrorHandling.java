package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AssemblerException;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.*;

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
					if (ex.getSubExceptions().isEmpty())
						updateProblems(Collections.singletonList(ex));
					else
						updateProblems(ex.getSubExceptions());
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
	 * @param exceptions
	 * 		Assembler problems.
	 */
	private void updateProblems(List<LineException> exceptions) {
		// Convert problem to <Line:Message> format
		if(exceptions == null)
			setProblems(Collections.emptyList());
		else {
			List<Pair<Integer, String>> problems = new ArrayList<>(exceptions.size());
			exceptions.forEach(ex -> {
				int line = ex.getLine();
				String msg = ex.getMessage();
				if (line == -1) {
					Throwable exx = (Throwable) ex;
					while(exx.getCause() != null && !(exx instanceof ASTParseException))
						exx = exx.getCause();
					if(exx instanceof ASTParseException)
						line = ((ASTParseException) exx).getLine();
					msg = exx.getMessage();
				}
				addProblem(ex);
				problems.add(new Pair<>(line - 1, msg));
			});
			setProblems(problems);
		}
	}

	/**
	 * @param ex
	 * 		Assembler problem.
	 */
	private void addProblem(LineException ex) {
		int index = ex.getLine() - 1;
		int len = codeArea.getParagraph(index).length();
		int literalStart =  codeArea.getParagraphSelection(index).getStart();
		markProblem(index, 0, len, literalStart, ex.getMessage());
	}
}
