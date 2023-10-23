package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.model.PlainTextChange;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;

import java.util.List;

/**
 * Outline of tab completion.
 *
 * @author Matt Coley
 */
public interface TabCompleter extends EditorComponent {
	/**
	 * Attempts to fill in the current completion candidate, if one is selected and the tab-completion overlay is active.
	 *
	 * @param event
	 * 		Key event where {@link KeyCode#TAB} or {@link KeyCode#ENTER} was pressed.
	 *
	 * @return {@code true} when a completion is made.
	 */
	boolean requestCompletion(@Nonnull KeyEvent event);

	/**
	 * Called when the linked {@link Editor} has updated its text.
	 * This is called for <i>ANY</i> and <i>ALL</i> changes.
	 * <p/>
	 * Implementations should update any state that is cheap to maintain in here.
	 */
	void onFineTextUpdate(@Nonnull PlainTextChange changes);

	/**
	 * Called when the linked {@link Editor} has updated its text, with reduced successions.
	 * <p/>
	 * Implementations should update any state that is expensive to maintain in here.
	 */
	void onRoughTextUpdate(@Nonnull List<PlainTextChange> changes);
}
