package me.coley.recaf.ui.controls.text;

import com.github.javaparser.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import me.coley.recaf.compiler.VirtualJavaFileObject;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.Errorable;
import me.coley.recaf.util.struct.Pair;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java-focused error handling.
 *
 * @author Matt
 */
public class JavaErrorHandling extends ErrorHandling<SourceCodeException>
		implements DiagnosticListener<VirtualJavaFileObject> {
	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public JavaErrorHandling(JavaPane textPane) {
		super(textPane);
	}

	// Update javac problems
	@Override
	public void report(Diagnostic<? extends VirtualJavaFileObject> diagnostic) {
		if (diagnostic.getKind() != Diagnostic.Kind.ERROR)
			return;
		// Convert the diagnostic to location data
		// 0-index the line number
		int line = (int) diagnostic.getLineNumber() - 1;
		int column = (int) diagnostic.getColumnNumber();
		int literalStart = calculate(Position.pos(line + 1, column));
		int wordLength = codeArea.getText().substring(literalStart).split("[^\\w.]")[0].length();
		int to = column + wordLength;
		String msg = diagnostic.getMessage(Locale.ENGLISH);
		problems.add(new Pair<>(line, msg));
		// Mark problem, 0-indexing the column
		markProblem(line, column - 1, to - 1, literalStart, msg);
		updateProblemLineGraphics();
	}

	// Update JavaParser problems
	@Override
	public void onCodeChange(String unused, Errorable<SourceCodeException> errorable) {
		// Clear old problems
		this.problems = Collections.emptyList();
		clearOldEvents();
		// Check if new update thread needs to be spawned
		if(updateThread == null || updateThread.isDone())
			updateThread = new DelayableAction(700, () -> {
				try {
					// Attempt to parse
					errorable.run();
					updateProblems(Collections.emptyList());
				} catch(SourceCodeException ex) {
					// Handle displaying errors
					updateProblems(ex.getResult().getProblems());
					for(Problem problem : ex.getResult().getProblems())
						problem.getLocation().ifPresent(l -> l.toRange().ifPresent(r -> addProblem(problem, r)));
				}
			});
		// Update the current thread so that
		updateThread.resetDelay();
		if(!updateThread.isAlive())
			updateThread.start();
		else
			updateProblems(Collections.emptyList());
	}

	private void markProblem(int line, int from, int to, int literalFrom, String message) {
		// Highlight the line & add the error indicator next to the line number
		Platform.runLater(() ->
				codeArea.setStyle(line, from, to, Collections.singleton("error"))
		);
		// Add tooltips to highlighted region
		Tooltip popup = new Tooltip(message);
		codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			int charPos = e.getCharacterIndex();
			if(charPos >= literalFrom && charPos <= literalFrom + (to - from)) {
				Point2D pos = e.getScreenPosition();
				popup.show(codeArea, pos.getX(), pos.getY() + 10);
			}
		});
		codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> popup.hide());
	}

	/**
	 * Update problems with JavaParser output.
	 *
	 * @param problems
	 * 		JavaParser problems.
	 */
	private void updateProblems(List<Problem> problems) {
		Platform.runLater(this::updateProblemLineGraphics);
		// Convert problem to <Line:Message> format
		this.problems = problems.stream().map(p -> {
			int key = -1;
			try {
				key = p.getLocation().get().getBegin().getRange().get().begin.line - 1;
			} catch(NoSuchElementException ex) { /* ignored */ }
			String value = p.getMessage();
			return new Pair<>(key, value);
		}).collect(Collectors.toList());
		// If there are problems, make on-hover timing nearly instant
		// - show errors immediately
		// - show non-errors after a delay
		int delay = problems.isEmpty() ? JavaPane.HOVER_DOC_TIME : JavaPane.HOVER_ERR_TIME;
		Platform.runLater(() -> codeArea.setMouseOverTextDelay(Duration.ofMillis(delay)));
	}

	/**
	 * @param problem
	 * 		JavaParser problem.
	 * @param range
	 * 		Range of problem.
	 */
	private void addProblem(Problem problem, Range range) {
		// Convert the JavaParser range to location data
		// - 0-index the line number
		int line = range.begin.line - 1;
		int literalStart = calculate(range.begin);
		int wordLength = codeArea.getText().substring(literalStart).split("[^\\w.]")[0].length();
		int from = range.begin.column - 1;
		int to = from + wordLength;
		//
		markProblem(line, from, to, literalStart, problem.getMessage());
	}

	/**
	 * @param position
	 * 		Javaparser row/column position.
	 *
	 * @return Index of position in entire string.
	 */
	private int calculate(Position position) {
		Scanner reader = new Scanner(codeArea.getText());
		int distance = 0;
		for(int i = 1; i < position.line; i++)
			distance += reader.nextLine().length() + 1;
		distance += position.column - 1;
		return distance;
	}
}
