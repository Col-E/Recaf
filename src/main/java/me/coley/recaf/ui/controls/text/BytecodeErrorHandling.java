package me.coley.recaf.ui.controls.text;

import com.github.javaparser.utils.Log;
import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AssemblerException;
import me.coley.recaf.util.struct.*;

import java.util.*;

/**
 * Bytecode-focused error handling.
 *
 * @author Matt
 */
public class BytecodeErrorHandling extends ErrorHandling {
	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public BytecodeErrorHandling(BytecodePane textPane) {
		super(textPane);
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
		// Convert problem to <Line:Message> format
		if(exceptions == null)
			setProblems(Collections.emptyList());
		else {
			List<Pair<Integer, String>> problems = new ArrayList<>(exceptions.size());
			exceptions.forEach(ex -> {
				Throwable exx = (Throwable) ex;
				// Fetch cause line
				int line = ex.getLine();
				if (line == -1) {
					Throwable exxCopy = exx;
					while(exxCopy.getCause() != null && !(exxCopy instanceof ASTParseException))
						exxCopy = exxCopy.getCause();
					if(exxCopy instanceof ASTParseException)  {
						exx = exxCopy;
						line = ((ASTParseException) exxCopy).getLine();
					}
				}
				// Fetch root message
				while (exx.getCause() instanceof ASTParseException)
					exx = exx.getCause();
				String msg = exx.getMessage();
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
		int literalStart =  codeArea.getParagraphSelection(index).getStart();
		markProblem(index, 0, len, literalStart, ex.getMessage());
	}
}
