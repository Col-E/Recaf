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
	}

	/**
	 * Sets up the initial size of the tab-pane so that it appears as being <i>"closed"</i>.
	 * Then registers mouse listeners to handle toggle behavior.
	 */
	@SuppressWarnings("unchecked")
	public void setup() {
		if (getSide().isVertical()) {
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

	private double getSize() {
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

	private void minimize() {
		if (getSide().isVertical()) {
			setMaxWidth(TAB_SIZE);
		} else {
			setMaxHeight(TAB_SIZE);
		}
	}

	private void toggleOpen() {
		if (isOpen()) {
			lastOpenSize = Math.max(MIN_INIT_SIZE, getSize());
			minimize();
		} else {
			setSize(lastOpenSize);
		}
	}

	private boolean isOpen() {
		return getSize() - 2 > TAB_SIZE;
	}
}
