package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import software.coley.recaf.ui.control.richtext.Editor;

import java.util.List;
import java.util.Optional;

import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.TAB;

/**
 * Basic completion popup implementation.
 *
 * @param <T>
 * 		Completion value type.
 *
 * @author Matt Coley
 */
public abstract class CompletionPopup<T> {
	public static final int STANDARD_CELL_SIZE = 20;
	private final Popup popup = new Popup();
	private final ListView<T> listView = new ListView<>();
	private final ScrollPane scrollPane = new ScrollPane(listView);
	private final CompletionValueTextifier<T> textifier;
	private final int maxItemsToShow;
	private final int cellSize;
	private CompletionPopupFocuser completionPopupFocuser;
	private CompletionPopupUpdater<T> completionPopupUpdater;
	private CodeArea area;
	private Bounds lastCaretBounds;
	private int popupSize;
	private T selected;

	/**
	 * @param cellSize
	 * 		Height of cells in the list-view of completion items.
	 * @param maxItemsToShow
	 * 		Number of cells to show at a time.
	 * @param textifier
	 * 		Mapper of {@code T} values to {@code String}.
	 */
	public CompletionPopup(int cellSize, int maxItemsToShow, @Nonnull CompletionValueTextifier<T> textifier) {
		this.maxItemsToShow = maxItemsToShow;
		this.textifier = textifier;

		// Ensure scroll-pane is 'fit-to-height' so there's no empty space wasting virtual scroll space.
		scrollPane.setFitToHeight(true);
		scrollPane.setFitToWidth(true);
		popup.getContent().add(scrollPane);

		// Auto-hide covers most cases that would be hard to otherwise detect
		// and isn't overly aggressive to hide it too often.
		popup.setAutoHide(true);

		// For simplicity of a few operations we're going to ensure all cells are a fixed size.
		listView.setFixedCellSize(this.cellSize = cellSize);
		listView.setPrefWidth(400);

		// Track what is the 'selected' value to complete.
		listView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> selected = newValue);

		// On mouse release should fire after a click event.
		// The selected item should be up-to-date, so triggering a completion request should work out.
		listView.setOnMouseReleased(event -> completeCurrentSelection());

		// The list-view event handling is a bit weird and breaks 'tab' handling.
		// So what we'll do is disable focus traversal so tht it doesn't handle anything
		// unless we specifically tell it to do so.
		listView.setFocusTraversable(false);
		popup.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			KeyCode code = event.getCode();

			// The event target is initially the popup scene.
			// If the event is not a 'tab' completion we can pass it along.
			// To prevent an infinite loop, we'll change the target value.
			if (event.getTarget() == popup.getScene() && code != TAB && code != ENTER) {
				listView.fireEvent(event.copyFor(popup, listView));

				// Multi-key action like 'Control Z' fail if we consume the event.
				if (!event.isControlDown()) event.consume();
			}
		});
	}

	/**
	 * Installs this popup into the given editor using the given tab-completer.
	 * <br>
	 * Intended to be called from the {@link TabCompleter#install(Editor)} implementation.
	 *
	 * @param area
	 * 		Editor code area component to register events within.
	 * @param completer
	 * 		Tab completer this popup will be associated with.
	 */
	public void install(@Nonnull CodeArea area, @Nonnull TabCompleter<T> completer) {
		setArea(area);

		completionPopupFocuser = new CompletionPopupFocuser(this);
		completionPopupUpdater = new CompletionPopupUpdater<>(completer, this);

		area.addEventFilter(KeyEvent.KEY_RELEASED, completionPopupUpdater);
		area.addEventFilter(KeyEvent.KEY_TYPED, completionPopupFocuser);

		// Clicking off of the popup should hide it.
		// - Clicking on the area is not covered by auto-hide.
		//   - But clicking on other components is covered.
		// - Clicking on the area delays the event until after the caret bounds update
		//   which makes the popup jump around when clicking off the popup.
		//   - Which we can bypass by hooking the events of the area's parent node.
		area.getParent().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> hide());

		// When we scroll the caret bounds change.
		area.caretBoundsProperty().addListener((observable, oldValue, newValue) -> {
			if (isShowing() && updateCaretBoundsIndirect())
				show();
		});
	}

	/**
	 * Uninstalls this popup from the editor provided in {@link #install(CodeArea, TabCompleter)}.
	 * <br>
	 * Intended to be called from the {@link TabCompleter#uninstall(Editor)} implementation.
	 */
	public void uninstall() {
		CodeArea localArea = area;
		if (localArea != null) {
			if (completionPopupUpdater != null)
				localArea.removeEventFilter(KeyEvent.KEY_RELEASED, completionPopupUpdater);
			if (completionPopupFocuser != null)
				localArea.removeEventFilter(KeyEvent.KEY_TYPED, completionPopupFocuser);
		}
		setArea(null);
	}

	/**
	 * Completes the current selection.
	 */
	public abstract void completeCurrentSelection();

	/**
	 * @param area
	 * 		Area the popup should show within.
	 */
	public void setArea(@Nullable CodeArea area) {
		this.area = area;
	}

	/**
	 * @return Current completion candidate to fill in if {@link TabCompleter#requestCompletion(KeyEvent)} is called.
	 */
	@Nullable
	public T getSelected() {
		return selected;
	}

	/**
	 * @return {@link Popup#isShowing()}
	 */
	public boolean isShowing() {
		return popup.isShowing();
	}

	/**
	 * @see Popup#requestFocus()
	 */
	public void requestFocus() {
		popup.requestFocus();
	}

	/**
	 * @see Popup#hide()
	 */
	public void hide() {
		if (isShowing())
			popup.hide();
	}

	/**
	 * Shows the popup at the current caret position in the editor.
	 *
	 * @see Popup#show(Node, double, double)
	 */
	public void show() {
		// If the popup has content to show, show it above where the caret position is.
		if (popupSize > 0)
			popup.show(area, lastCaretBounds.getMinX(), lastCaretBounds.getMinY() - popupSize);
	}

	/**
	 * @return {@code true} when the bounds were updated successfully.
	 */
	public boolean updateCaretBounds() {
		Bounds bounds = area.caretBoundsProperty().getValue().orElse(null);
		if (bounds != null) {
			lastCaretBounds = bounds;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return {@code true} when the bounds were updated successfully.
	 */
	public boolean updateCaretBoundsIndirect() {
		Bounds bounds = lastCaretBounds;
		if (bounds == null)
			return false;

		// The caret bounds can de-sync in some weird cases, so we're better off indirectly computing the current Y position
		// of the caret by looking at the paragraph's bounds the caret resides in.
		Optional<Bounds> paragraphBoundsOnScreen = area.getParagraphBoundsOnScreen(Math.min(area.getCurrentParagraph(), area.lastVisibleParToAllParIndex()));
		if (paragraphBoundsOnScreen.isPresent()) {
			Bounds paragraphBounds = paragraphBoundsOnScreen.get();
			lastCaretBounds = new BoundingBox(bounds.getMinX(), paragraphBounds.getMinY(), bounds.getWidth(), bounds.getHeight());
			return true;
		}

		return false;
	}

	/**
	 * @return {@code true} when the associated text area has selected text.
	 */
	public boolean hasTextSelection() {
		return area.getSelection().getLength() > 0;
	}

	/**
	 * Updates the items to support completion of, and updates the size of the UI
	 * to fit exactly the number of items to show.
	 *
	 * @param items
	 * 		Items to support completion of.
	 */
	public void updateItems(@Nonnull List<T> items) {
		// Compute the size of the popup.
		//  - (Number of cells to show at a time) * (size of cell)
		//  - Needs a bit of padding due to the way borders/scrollbars render
		// The scollpane should be dictating the size since it is the popup content root.
		int itemCount = items.size();
		popupSize = cellSize * (Math.min(itemCount, maxItemsToShow) + 1);
		scrollPane.setPrefHeight(popupSize);
		scrollPane.setMaxHeight(popupSize);

		// Track what was selected beforehand.
		// Needs to be done before we reset the list-view items.
		T localSelected = selected;

		// Update the lists items.
		listView.getItems().setAll(items);

		// Select what was previously selected, or the first item if nothing was selected before.
		if (localSelected != null) {
			int index = items.indexOf(localSelected);
			if (index >= 0 && index < itemCount)
				listView.getSelectionModel().select(index);
			else
				listView.getSelectionModel().select(0);
		} else if (itemCount > 0) {
			listView.getSelectionModel().select(0);
		}
	}

	/**
	 * Completes the current text with the value of {@link #getSelected()}.
	 *
	 * @param partialText
	 * 		Current text to complete.
	 *
	 * @return {@code true} on successful completion.
	 */
	public boolean doComplete(@Nonnull String partialText) {
		// Get the selected value to complete.
		// Must be done before we hide the popup.
		T localSelected = selected;

		// We're done so we can hide the popup.
		hide();

		// Complete the selected item, clearing it now that we're done.
		if (localSelected != null) {
			selected = null;
			String toComplete = textifier.toText(localSelected);
			return complete(partialText, toComplete);
		}

		return false;
	}

	protected boolean complete(@Nonnull String partialText, @Nonnull String fullText) {
		if (!fullText.startsWith(partialText))
			return false;

		// Insert the rest of the text.
		String remainingWordText = fullText.substring(partialText.length());
		area.insertText(area.getCaretPosition(), remainingWordText);

		// Show completion centered on the screen if it is off-screen.
		if (area.getCaretBounds().isEmpty())
			area.showParagraphAtCenter(area.getCurrentParagraph());

		return true;
	}

	/**
	 * Pass along backspace handling when we observe {@link KeyCode#BACK_SPACE} in a {@link KeyEvent}.
	 */
	public void handleBackspace() {
		IndexRange selection = area.getSelection();
		if (selection.getLength() > 1) {
			area.deleteText(selection);
		} else {
			area.deletePreviousChar();
		}
	}
}
