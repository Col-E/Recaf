package me.coley.recaf.ui.controls.text;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.scene.NodeEventDispatcher;
import javafx.application.Platform;
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
	protected final TextPane textPane;
	protected final CodeArea codeArea;
	protected DelayableAction updateThread;
	protected List<Pair<Integer, String>> problems = Collections.emptyList();

	/**
	 * @param textPane
	 * 		Pane to handle errors for.
	 */
	public ErrorHandling(TextPane textPane) {
		this.textPane = textPane;
		this.codeArea = textPane.codeArea;
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
	 * Iterate over all problems, redrawing the lines they were once on.
	 */
	protected void updateProblemLineGraphics() {
		List<Pair<Integer, String>> copy = new ArrayList<>(problems);
		Platform.runLater(() -> {
			for(Pair<Integer, String> problem : copy)
				codeArea.recreateParagraphGraphic(problem.getKey());
		});
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
