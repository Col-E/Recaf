package me.coley.recaf.ui.control;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.Set;

/**
 * A tab pane that supports collapsing the view by clicking on the current open tab's title.
 * This imitates the behavior of the tabs in IntelliJ
 *
 * @author Matt Coley
 */
public class CollapsibleTabPane extends TabPane {
	private static final double MIN_INIT_SIZE = 300;
	private static final double TAB_SIZE = 28;
	private Parent lastClicked;
	private double lastOpenSize = MIN_INIT_SIZE;

	/**
	 * New tab pane.
	 */
	public CollapsibleTabPane() {
		// Prevent auto-expanding when parent resizes.
		SplitPane.setResizableWithParent(this, false);
		getStyleClass().add("collapsible-tab-pane");
	}

	/**
	 * Sets up the initial size of the tab-pane so that it appears as being <i>"closed"</i>.
	 * Then registers mouse listeners to handle toggle behavior.
	 */
	@SuppressWarnings("unchecked")
	public void setup() {
		if (shouldHide()) {
			hide();
		} else if (getSide().isVertical()) {
			setMinWidth(TAB_SIZE);
			setMaxWidth(TAB_SIZE);
		} else {
			setMinHeight(TAB_SIZE);
			setMaxHeight(TAB_SIZE);
		}
		// On a delay so that the headers can be populated when we try to do the lookup
		FxThreadUtil.delayedRun(100, () -> {
			Set<Node> headers = lookupAll(".tab-container");
			headers.forEach(node -> {
				// Change the tab behavior such that the parent bounds are used for actions rather than delegating
				// to children for their own bounds. This makes the behavior much more reliable.
				Parent parent = node.getParent();
				parent.setPickOnBounds(true);
				EventHandler<MouseEvent> parentHandler = (EventHandler<MouseEvent>) parent.getOnMousePressed();
				parent.setOnMousePressed(ev -> {
					// Handle toggling visibility when the user clicks on the tab.
					if (ev.getButton() == MouseButton.PRIMARY) {
						boolean open = isOpen();
						if (parent.equals(lastClicked)) {
							toggleOpen();
							ev.consume();
						} else if (!open) {
							toggleOpen();
							ev.consume();
						}
						lastClicked = parent;
					}
					// Delegate to parent behavior
					if (parentHandler != null)
						parentHandler.handle(ev);
				});
			});
		});
	}

	/**
	 * Mirror for {@link #getWidth()} or {@link #getHeight()} depending on if the pane is vertical or not.
	 *
	 * @return The size of the pane, given its open/closed state.
	 */
	public double getSize() {
		if (getSide().isVertical()) {
			return getWidth();
		} else {
			return getHeight();
		}
	}

	private void setSize(double size) {
		if (getSide().isVertical()) {
			setMaxWidth(Double.MAX_VALUE);
			// Hack to force the parent container to resize (for example, a split-pane)
			setMinWidth(size);
			FxThreadUtil.delayedRun(50, () -> setMinWidth(TAB_SIZE));
		} else {
			setMaxHeight(Double.MAX_VALUE);
			// Hack to force the parent container to resize (for example, a split-pane)
			setMinHeight(size);
			FxThreadUtil.delayedRun(50, () -> setMinHeight(TAB_SIZE));
		}
	}

	/**
	 * Close the pane, hiding it completely.
	 */
	public void hide() {
		if (getSide().isVertical()) {
			setMaxWidth(0);
			setPrefHeight(0);
		} else {
			setMaxHeight(0);
			setPrefHeight(0);
		}
	}

	/**
	 * Close the pane, showing the tab-titles showing.
	 */
	public void minimize() {
		if (getSide().isVertical()) {
			setMaxWidth(TAB_SIZE);
		} else {
			setMaxHeight(TAB_SIZE);
		}
	}

	/**
	 * Restores the pane to its previous opened size.
	 */
	private void restore() {
		setSize(lastOpenSize);
	}

	/**
	 * Toggle the open state of the pane.
	 */
	public void toggleOpen() {
		if (shouldHide()) {
			hide();
		} else if (isOpen()) {
			lastOpenSize = Math.max(MIN_INIT_SIZE, getSize());
			minimize();
		} else {
			restore();
		}
	}


	/**
	 * @return {@code true} if the pane is currently open.
	 */
	public boolean isOpen() {
		return getSize() - 2 > TAB_SIZE;
	}

	private boolean shouldHide() {
		return getTabs().isEmpty();
	}
}
