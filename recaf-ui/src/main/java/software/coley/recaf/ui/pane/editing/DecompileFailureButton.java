package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

/**
 * Warning icon button to be displayed in the {@link ToolsContainerComponent} when a decompilation fails.
 *
 * @author Matt Coley
 */
public class DecompileFailureButton extends Button {
	private static final int animationCycleMillis = 250;
	private static final int animationCycles = 9; // odd so it when the animation plays it stays visible
	private final FadeTransition fadeIn = new FadeTransition();
	private Popover popover;

	/**
	 * New button.
	 */
	public DecompileFailureButton() {
		// Want to be invisible by default. When placed in a 'Group' it will take no space.
		setVisible(false);
		setOpacity(0);

		// Styles
		setGraphic(new FontIconView(CarbonIcons.WARNING_ALT_FILLED, Color.YELLOW));
		getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);

		// Setup fade to be used when the button is shown.
		fadeIn.setNode(this);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);
		fadeIn.setDuration(Duration.millis(animationCycleMillis));
		fadeIn.setCycleCount(animationCycles);
		fadeIn.setAutoReverse(true);

		// Show message on click.
		setOnMousePressed(e -> {
			if (popover != null && popover.isShowing())
				popover.hide();

			popover = new Popover(new BoundLabel(Lang.getBinding("java.decompile-failure")));
			popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
			popover.setAutoHide(true);
			popover.show(this);
		});
	}

	/**
	 * Animate the button being displayed.
	 */
	public void animate() {
		if (popover != null && popover.isShowing())
			popover.hide();

		// Ensure button is visible and play the fade-in animation
		setVisible(true);
		fadeIn.play();

		// Show a message above the button indicating the class failed to decompile
		popover = new Popover(new BoundLabel(Lang.getBinding("java.decompile-failure.brief")));
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.setAutoHide(true);
		FxThreadUtil.delayedRun(animationCycleMillis, () -> popover.show(this));
		FxThreadUtil.delayedRun(animationCycleMillis * animationCycles, () -> popover.hide());
	}
}
