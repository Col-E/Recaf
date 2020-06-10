package me.coley.recaf.ui.controls.text;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.struct.*;

import java.util.*;

/**
 * Bytecode-focused error handling.
 *
 * @author Matt
 */
public class BytecodeErrorHandling extends ErrorHandling {
	private final BytecodeEditorPane bytecodePane;

	/**
	 * @param bytecodePane
	 * 		Pane to handle errors for.
	 */
	public BytecodeErrorHandling(BytecodeEditorPane bytecodePane) {
		super(bytecodePane);
		this.bytecodePane = bytecodePane;
	}

	@Override
	protected void handleCodeChangeError(Throwable ex) {
		if (ex == null)
			updateProblems(null);
		else if (ex instanceof LineException) {
			// Handle displaying errors
			AssemblerException aex = (AssemblerException) ex;
			if(aex.getSubExceptions().isEmpty())
				updateProblems(Collections.singletonList(aex));
			else
				updateProblems(aex.getSubExceptions());
		} else {
			updateProblems(Collections.singletonList(new AssemblerException(ex,
					"Uncaught exception in assembler", -1)));
			// Log this because this is something we have to fix, not the user
			Log.error(ex, "Uncaught exception in assembler");
		}
	}

	/**
	 * Update latest problems.
	 *
	 * @param exceptions
	 * 		Assembler problems.
	 */
	private void updateProblems(List<LineException> exceptions) {
		// Notify UI
		bytecodePane.onErrorsReceived(exceptions);
		// Convert problem to <Line:Message> format
		if(exceptions == null)
			setProblems(Collections.emptyList());
		else {
			List<Pair<Integer, String>> problems = new ArrayList<>(exceptions.size());
			exceptions.forEach(ex -> {
				Throwable loggedException = (Throwable) ex;
				// Fetch cause line
				int line = ex.getLine();
				if (line == -1) {
					Throwable tmpException = loggedException;
					while(tmpException.getCause() != null) {
						tmpException = tmpException.getCause();
						if (tmpException instanceof LineException) {
							int exxLine = ((LineException) tmpException).getLine();
							if (exxLine != -1) {
								loggedException = tmpException;
								line = exxLine;
							}
						}
					}
					// Couldn't determine cause line, likely an internal error.
					if (line == -1)  {
						Log.error(loggedException, "Unrecognized exception thrown when assembling method");
					}
				}
				// Fetch root message
				while (loggedException.getCause() instanceof ASTParseException)
					loggedException = loggedException.getCause();
				String msg = loggedException.getMessage();
				markProblem(ex);
				problems.add(new Pair<>(line - 1, msg));
			});
			setProblems(problems);
		}
	}

	/**
	 * @param ex
	 * 		Assembler problem.
	 */
	private void markProblem(LineException ex) {
		int index = ex.getLine() - 1;
		if (index < 0)
			return;
		int len = codeArea.getParagraph(index).length();
		int literalStart =  codeArea.position(index, 0).toOffset();
		markProblem(index, 0, len, literalStart, ex.getMessage());
	}

	@Override
	protected void toggleErrorDisplay() {
		// Do nothing
	}
}
