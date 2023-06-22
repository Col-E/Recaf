package software.coley.recaf.ui.pane.editing;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.control.richtext.ScrollbarPaddingUtil;
import software.coley.recaf.ui.control.richtext.problem.ProblemInvalidationListener;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;

/**
 * Simple problem overlay, showing users how many problems of what type there are in the current {@link Editor}.
 *
 * @author Matt Coley
 */
public class ProblemOverlay extends Group implements EditorComponent, ProblemInvalidationListener {
	private final ChangeListener<Boolean> handleScrollbarVisibility = (ob, old, cur) -> ScrollbarPaddingUtil.handleScrollbarVisibility(this, cur);
	private final IntegerProperty problemCount = new SimpleIntegerProperty(-1);
	private Editor editor;

	/**
	 * New problem overlay.
	 */
	public ProblemOverlay() {
		// Display the number of problems, and their type.
		FontIconView iconGood = new FontIconView(CarbonIcons.CHECKMARK, Color.LAWNGREEN);
		FontIconView iconInfo = new FontIconView(CarbonIcons.INFORMATION, Color.TURQUOISE);
		FontIconView iconWarning = new FontIconView(CarbonIcons.WARNING_ALT, Color.YELLOW);
		FontIconView iconError = new FontIconView(CarbonIcons.ERROR, Color.RED);
		Button indicator = new Button();
		indicator.setFocusTraversable(false);
		indicator.getStyleClass().addAll(Styles.SMALL, "muted");
		indicator.graphicProperty().bind(problemCount.map(size -> {
			// Skip before linked to an editor.
			if (editor == null)
				return null;

			// Skip if tracking is disabled.
			ProblemTracking tracking = editor.getProblemTracking();
			if (tracking == null)
				return null;

			// No problems
			if (size.intValue() <= 0)
				return iconGood;

			// Show the count of each kind of problem
			HBox wrapper = new HBox();
			wrapper.setSpacing(5);
			int info = tracking.getProblems(p -> p.getLevel().ordinal() <= ProblemLevel.INFO.ordinal()).size();
			int warn = tracking.getProblems(p -> p.getLevel() == ProblemLevel.WARN).size();
			int error = tracking.getProblems(p -> p.getLevel() == ProblemLevel.ERROR).size();
			if (info > 0) wrapper.getChildren().add(new Label(String.valueOf(info), iconInfo));
			if (warn > 0) wrapper.getChildren().add(new Label(String.valueOf(warn), iconWarning));
			if (error > 0) wrapper.getChildren().add(new Label(String.valueOf(error), iconError));
			return wrapper;
		}));
		BooleanBinding hasProblems = problemCount.greaterThan(0);
		hasProblems.addListener((ob, had, has) -> {
			// When there are problems, this is the left-most button.
			// When there are no problems, this is the only button.
			// Thus, our left-pill state should be bound to having problems.
			ObservableList<String> styleClass = indicator.getStyleClass();
			if (has)
				styleClass.add(Styles.LEFT_PILL);
			else
				styleClass.remove(Styles.LEFT_PILL);
		});

		// Next/prev buttons
		//  - Actions only available when there are problems.
		Button prev = new ActionButton(CarbonIcons.ARROW_UP, () -> {
			// Skip if tracking is disabled.
			ProblemTracking tracking = editor.getProblemTracking();
			if (tracking == null)
				return;

			// Go to previous line with a problem.
			CodeArea codeArea = editor.getCodeArea();
			int line = codeArea.getCurrentParagraph() + 1;
			Integer prevLineWithError = tracking.getProblems().floorKey(line - 1);
			if (prevLineWithError == null)
				prevLineWithError = tracking.getProblems().floorKey(line);
			if (prevLineWithError != null) {
				codeArea.moveTo(prevLineWithError - 1, 0);
				codeArea.selectLine();
				codeArea.showParagraphAtCenter(codeArea.getCurrentParagraph());
			}
		});
		Button next = new ActionButton(CarbonIcons.ARROW_DOWN, () -> {
			// Skip if tracking is disabled.
			ProblemTracking tracking = editor.getProblemTracking();
			if (tracking == null)
				return;

			// Go to next line with a problem.
			CodeArea codeArea = editor.getCodeArea();
			int line = codeArea.getCurrentParagraph() + 1;
			Integer nextLineWithError = tracking.getProblems().ceilingKey(line + 1);
			if (nextLineWithError == null)
				nextLineWithError = tracking.getProblems().ceilingKey(line);
			if (nextLineWithError != null) {
				codeArea.moveTo(nextLineWithError - 1, 0);
				codeArea.selectLine();
				codeArea.showParagraphAtCenter(codeArea.getCurrentParagraph());
			}
		});
		prev.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.CENTER_PILL, Styles.SMALL, "muted");
		next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.RIGHT_PILL, Styles.SMALL,  "muted");
		prev.setFocusTraversable(false);
		next.setFocusTraversable(false);
		HBox buttons = new HBox(prev, next);
		buttons.visibleProperty().bind(hasProblems);

		// Add to layout
		getChildren().add(new HBox(indicator, new Group(buttons)));
	}

	@Override
	public void install(@Nonnull Editor editor) {
		ProblemTracking tracking = editor.getProblemTracking();
		if (tracking != null) {
			this.editor = editor;

			// Add to editor.
			editor.getPrimaryStack().getChildren().add(this);

			// Track if there are problems
			tracking.addListener(this);

			// Initial value set to trigger a UI refresh.
			problemCount.set(tracking.getProblems().size());

			// Layout tweaks
			StackPane.setAlignment(this, Pos.TOP_RIGHT);
			StackPane.setMargin(this, new Insets(7));
			editor.getVerticalScrollbar().visibleProperty().addListener(handleScrollbarVisibility);
		}
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		if (this.editor == editor) {
			this.editor = null;

			// Remove self as listener.
			ProblemTracking tracking = editor.getProblemTracking();
			if (tracking != null)
				tracking.removeListener(this);

			// Remove from editor.
			editor.getPrimaryStack().getChildren().remove(this);
			editor.getVerticalScrollbar().visibleProperty().removeListener(handleScrollbarVisibility);
		}
	}

	@Override
	public void onProblemInvalidation() {
		ProblemTracking tracking = editor.getProblemTracking();
		if (tracking != null)
			problemCount.set(tracking.getProblems().size());
	}
}
