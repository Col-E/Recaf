package me.coley.recaf.ui.controls.text;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.scene.NodeEventDispatcher;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import me.coley.recaf.util.DelayableAction;
import me.coley.recaf.util.struct.Errorable;
import me.coley.recaf.util.struct.Pair;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Generic error handling for {@link TextPane} content.
 * @param <T> Type of error.
 */
public abstract class ErrorHandling<T extends Throwable> {
	protected final CodeArea codeArea;
	protected DelayableAction updateThread;
	protected List<Pair<Integer, String>> problems = Collections.emptyList();

	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public ErrorHandling(TextPane textPane) {
		this.codeArea = textPane.codeArea;
	}

	/**
	 * @return {@code true} if errors have been found.
	 */
	public boolean hasErrors() {
		return !problems.isEmpty();
	}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return {@code true} if the error has an error.
	 */
	public boolean hasError(int line) {
		for(Pair<Integer, String> problem : problems) {
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
		for(Pair<Integer, String> problem : problems) {
			if(line == problem.getKey())
				return problem.getValue();
		}
		return null;
	}

	/**
	 * Handle AST updates. Waits until the user stops typing <i>(based on a delay)</i> to finally
	 * generate the AST.
	 *
	 * @param unused
	 * 		Unused text of the change. Instead, we lookup this value since it's run on a delay in
	 * 		a new thread.
	 * @param errorable
	 * 		Code change action that can produce errors.
	 */
	public abstract void onCodeChange(String unused, Errorable<T> errorable);

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
	protected void clearProblemLines() {
		for (int p = 0; p < codeArea.getParagraphs().size(); p++)
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
}
