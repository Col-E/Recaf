package me.coley.recaf.ui.controls.text;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.scene.NodeEventDispatcher;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.Errorable;
import me.coley.recaf.util.struct.Pair;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Error handling for {@link EditorPane} content.
 *
 * @author Matt
 */
public abstract class ErrorHandling {
	private static final int UPDATE_DELAY = 500;
	protected final CodeArea codeArea;
	private DelayableAction updateThread;
	private List<Pair<Integer, String>> oldProblems = Collections.emptyList();
	private List<Pair<Integer, String>> problems = Collections.emptyList();
	private ListView<Pair<Integer, String>> errorList;

	/**
	 * @param editorPane
	 * 		Pane to handle errors for.
	 */
	public ErrorHandling(EditorPane editorPane) {
		this.codeArea = editorPane.codeArea;
	}

	/**
	 * @return {@code true} if errors have been found.
	 */
	public boolean hasErrors() {
		return !getProblems().isEmpty();
	}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return {@code true} if the error has an error.
	 */
	public boolean hasError(int line) {
		for(Pair<Integer, String> problem : getProblems()) {
			if(line == problem.getKey())
				return true;
		}
		return false;
	}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return Error message for line. {@code null} if none.
	 */
	public String getLineComment(int line) {
		for(Pair<Integer, String> problem : getProblems()) {
			if(line == problem.getKey())
				return problem.getValue();
		}
		return null;
	}

	/**
	 * Handle code change events.
	 *
	 * @param errorable
	 * 		Code change action that can produce errors.
	 */
	public void onCodeChange(Errorable<?> errorable) {
		// Because we need to clear old handlers for error-hover-messages
		clearOldEvents();
		// Check if new update thread needs to be spawned
		if (updateThread != null)
			updateThread.resetDelay();
		if(updateThread == null || updateThread.isDone()) {
			updateThread = new DelayableAction(UPDATE_DELAY, () -> {
				Platform.runLater(this::refreshProblemGraphics);
				try {
					// Attempt to run
					errorable.run();
					handleCodeChangeError(null);
				} catch(Throwable ex) {
					handleCodeChangeError(ex);
				}
				Platform.runLater(this::refreshProblemGraphics);
			});
			updateThread.start();
		}
		// Update the current thread so that
		updateThread.resetDelay();
		if(!updateThread.isAlive())
			updateThread.start();
		else
			handleCodeChangeError(null);
	}

	/**
	 * Handle errors thrown by the {@link #onCodeChange(Errorable)}'s passed errorable function.
	 *
	 * @param ex
	 * 		The thrown exception.
	 */
	protected abstract void handleCodeChangeError(Throwable ex);

	/**
	 * Marks the given range with the <i>"error"</i> type.
	 *
	 * @param line
	 * 		Line the problem occured on.
	 * @param from
	 * 		Relative start position on line.
	 * @param to
	 * 		Relative end position on line.
	 * @param literalFrom
	 * 		Literal start position in string.
	 * @param message
	 * 		Message to supply pop-up window with.
	 */
	protected void markProblem(int line, int from, int to, int literalFrom, String message) {
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
	 * Clear paragraphs's error graphics.
	 */
	protected void refreshProblemGraphics() {
		// combine old and new proble mlines
		Set<Integer> numbers = new HashSet<>();
		Set<Integer> problemLines = problems.stream().map(Pair::getKey).collect(Collectors.toSet());
		Set<Integer> oldPoblemLines = oldProblems.stream().map(Pair::getKey).collect(Collectors.toSet());
		numbers.addAll(problemLines);
		numbers.addAll(oldPoblemLines);
		// give some wiggle room for users adding/removing lines
		int range = 2;
		int max = codeArea.getParagraphs().size();
		for(int i : numbers)
			for(int p = i - range; p < i + range; p++)
				if(p >= 0 && p < max)
					codeArea.recreateParagraphGraphic(p);
	}

	/**
	 * This is cancer, but it works flawlessly. Tracking the handlers also sucks.
	 */
	protected void clearOldEvents() {
		try {
			Object handlerManager = ((NodeEventDispatcher)codeArea.getEventDispatcher()).getEventHandlerManager();
			Field handlerMap  = EventHandlerManager.class.getDeclaredField("eventHandlerMap");
			handlerMap.setAccessible(true);
			Map map = (Map) handlerMap.get(handlerManager);
			map.remove(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN);
			map.remove(MouseOverTextEvent.MOUSE_OVER_TEXT_END);
		} catch(Exception exx) {
			exx.printStackTrace();
		}
	}

	/**
	 * @return Last reported problems.
	 */
	public List<Pair<Integer, String>> getProblems() {
		return problems;
	}

	/**
	 * @param problems
	 * 		New reported problems.
	 */
	public void setProblems(List<Pair<Integer, String>> problems) {
		this.oldProblems = this.problems;
		this.problems = problems;
		if (errorList != null)
			Platform.runLater(() -> {
				// Set problems
				errorList.getItems().setAll(problems);
				// Check if there is a UI component to update
				if (errorList.getParent() == null)
					return;
				SplitPane parent = (SplitPane) errorList.getParent().getParent().getParent();
				if (parent == null)
					return;
				// Update the error list display
				if(problems.isEmpty())
					parent.setDividerPositions(1);
				else if(parent.getDividerPositions()[0] > 0.98)
					parent.setDividerPositions(0.84);
			});
	}

	/**
	 * Unbind {@link #errorList UI component} from error handling.
	 */
	public void unbind() {
		this.errorList = null;
	}

	/**
	 * Bind errrors to {@link #errorList UI component} .
	 *
	 * @param errorList
	 * 		UI component to display errors.
	 */
	public void bind(ListView<Pair<Integer, String>> errorList) {
		this.errorList = errorList;
	}
}
