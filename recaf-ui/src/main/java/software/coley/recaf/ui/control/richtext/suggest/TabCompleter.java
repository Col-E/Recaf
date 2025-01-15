package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.model.PlainTextChange;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;

import java.util.List;

/**
 * Outline of tab completion.
 *
 * @param <T>
 * 		Completion value type.
 *
 * @author Matt Coley
 */
public interface TabCompleter<T> extends EditorComponent {
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
	 * @return List of potential completions based on the current state.
	 */
	@Nonnull
	List<T> computeCurrentCompletions();

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

	/**
	 * Called when the {@link CompletionPopupUpdater} observes a typed non-letter/non-digit {@link KeyCode}.
	 * <p/>
	 * Implementations can check for key-codes not covered by {@link KeyCode#isLetterKey()} and {@link KeyCode#isDigitKey()}
	 * to support completion of additional characters. For example the {@code []} characters are not covered by default
	 * and an implementation would need to explicitly support them via this method.
	 *
	 * @param code Key-code to check for tab-completion support.
	 * @return {@code true} when this completer supports the tab-completion of the given key-code.
	 */
	default boolean isSpecialCompletableKeyCode(@Nullable KeyCode code) {
		return false;
	}
}
