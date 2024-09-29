package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.control.richtext.ScrollbarPaddingUtil;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemInvalidationListener;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.PlatformType;

import java.util.Collection;

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
		Button indicator = new Button();
		indicator.setFocusTraversable(false);
		indicator.getStyleClass().addAll(Styles.SMALL, "muted");

		// On-click to show a list of all problems
		indicator.setOnAction(e -> {
			ProblemTracking problemTracking = editor.getProblemTracking();
			if (problemTracking == null) return;

			// Skip if no problems
			Collection<Problem> problems = problemTracking.getAllProblems();
			if (problems.isEmpty()) return;

			// Create vertical list
			VBox content = new VBox();
			ObservableList<Node> children = content.getChildren();
			for (Problem problem : problems) {
				// Map level to graphic
				ProblemLevel level = problem.level();
				Color levelColor = switch (level) {
					case ERROR -> Color.RED;
					case WARN -> Color.YELLOW;
					default -> Color.TURQUOISE;
				};
				Node graphic = switch (level) {
					case ERROR -> new FontIconView(CarbonIcons.ERROR, levelColor);
					case WARN -> new FontIconView(CarbonIcons.WARNING_ALT, levelColor);
					default -> new FontIconView(CarbonIcons.INFORMATION, levelColor);
				};

				// Create 'N: Message' layout
				//  - Exclude line number 'N' when line number is negative
				Label messageLabel = new Label(problem.message());
				messageLabel.setTextFill(levelColor);
				messageLabel.setMaxWidth(Integer.MAX_VALUE);
				int line = problem.line();
				HBox problemBox;
				if (line >= 0) {
					Label lineLabel = new Label(String.valueOf(line), graphic);
					lineLabel.setTextFill(levelColor);
					lineLabel.getStyleClass().add(Styles.TEXT_BOLD);
					problemBox = new HBox(lineLabel, messageLabel);
				} else {
					messageLabel.setGraphic(graphic);
					problemBox = new HBox(messageLabel);
				}
				problemBox.setSpacing(5);
				problemBox.setPadding(new Insets(5));

				// Make on-hover more clearly show which problem is relevant.
				// The changing color on-hover also indicates clickable action.
				problemBox.setOnMouseEntered(me -> problemBox.getStyleClass().add("background"));
				problemBox.setOnMouseExited(me -> problemBox.getStyleClass().remove("background"));

				// When clicked, center the relevant problem.
				problemBox.setOnMousePressed(me -> {
					CodeArea codeArea = editor.getCodeArea();
					codeArea.moveTo(line - 1, 0);
					codeArea.selectLine();
					codeArea.showParagraphAtCenter(codeArea.getCurrentParagraph());
				});
				children.add(problemBox);
			}

			ScrollPane scrollWrapper = new ScrollPane(content);
			scrollWrapper.maxHeightProperty().bind(editor.heightProperty().multiply(0.8));
			scrollWrapper.prefViewportWidthProperty().bind(editor.widthProperty().multiply(0.8));
			Popover popover = new Popover(scrollWrapper);
			popover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
			popover.show(indicator);

			// When mousing away from the problems list make it translucent so the text behind it can be read.
			// Opacity does not seem to work on Linux/Mac, so for now its limited to Windows.
			if (PlatformType.isWindows()) {
				BooleanProperty isMouseOver = new SimpleBooleanProperty(true);
				scrollWrapper.setOnMouseEntered(me -> isMouseOver.set(true));
				scrollWrapper.setOnMouseExited(me -> isMouseOver.set(false));
				scrollWrapper.effectProperty().bind(Bindings.when(isMouseOver.not())
						.then(new BoxBlur(5, 5, 1))
						.otherwise((BoxBlur) null));
				popover.opacityProperty().bind(Bindings.when(isMouseOver)
						.then(1.0)
						.otherwise(0.4));
			}
		});

		// Can recycle the same instance with the indicator graphic
		FontIconView iconGood = new FontIconView(CarbonIcons.CHECKMARK, Color.LAWNGREEN);
		FontIconView iconInfo = new FontIconView(CarbonIcons.INFORMATION, Color.TURQUOISE);
		FontIconView iconWarning = new FontIconView(CarbonIcons.WARNING_ALT, Color.YELLOW);
		FontIconView iconError = new FontIconView(CarbonIcons.ERROR, Color.RED);
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
			int info = tracking.getProblems(p -> p.level().ordinal() <= ProblemLevel.INFO.ordinal()).size();
			int warn = tracking.getProblemsByLevel(ProblemLevel.WARN).size();
			int error = tracking.getProblemsByLevel(ProblemLevel.ERROR).size();
			if (info > 0) wrapper.getChildren().add(new Label(String.valueOf(info), iconInfo));
			if (warn > 0) wrapper.getChildren().add(new Label(String.valueOf(warn), iconWarning));
			if (error > 0) wrapper.getChildren().add(new Label(String.valueOf(error), iconError));
			return wrapper;
		}));
		indicator.tooltipProperty().bind(problemCount.map(size -> {
			if (size.intValue() == 0)
				return new Tooltip(Lang.get("assembler.problem.0"));
			else if (size.intValue() == 1)
				return new Tooltip(Lang.get("assembler.problem.1"));
			return new Tooltip(Lang.get("assembler.problem.N").replace("N", String.valueOf(size)));
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
			if (prevLineWithError == null)
				prevLineWithError = tracking.getProblems().firstKey();
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
			if (nextLineWithError == null)
				nextLineWithError = tracking.getProblems().firstKey();
			if (nextLineWithError != null) {
				codeArea.moveTo(nextLineWithError - 1, 0);
				codeArea.selectLine();
				codeArea.showParagraphAtCenter(codeArea.getCurrentParagraph());
			}
		});
		prev.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.CENTER_PILL, Styles.SMALL, "muted");
		next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.RIGHT_PILL, Styles.SMALL, "muted");
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
			problemCount.set(tracking.getAllProblems().size());

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
			FxThreadUtil.run(() -> problemCount.set(tracking.getAllProblems().size()));
	}
}
