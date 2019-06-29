package me.coley.recaf.ui.component;

import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.function.IntConsumer;

import org.controlsfx.control.GridCell;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Misc;

/**
 * Control for visualizing access flags.
 * 
 * @author Matt
 */
public class AccessButton extends Button {
	/**
	 * Context used to determine which set of modifiers to display.
	 */
	private final AccessFlag.Type context;
	/**
	 * Access currently displayed.
	 */
	private int access;
	/**
	 * Popover to show modifiers in.
	 */
	private PopOver popover;
	/**
	 * Task executed when {@link #setAccess(int)} is called.
	 */
	private IntConsumer updateTask;

	public AccessButton(AccessFlag.Type context) {
		this(context, 0);
	}

	public AccessButton(AccessFlag.Type context, int access) {
		this.context = context;
		setupPopover();
		setAccess(access);
	}

	/**
	 * Open editor window.
	 */
	private void setupPopover() {
		// Create grid of access modifiers
		GridView<AccessFlag> grid = new GridView<>();
		grid.setCellHeight(25);
		grid.setCellWidth(110);
		grid.setCellFactory(gridView -> new GridCell<AccessFlag>() {
			@Override
			protected void updateItem(AccessFlag flag, boolean empty) {
				super.updateItem(flag, empty);
				if (empty || flag == null) {
					setGraphic(null);
				} else {
					setGraphic(new AccessCheck(flag));
				}
			}
		});
		for (AccessFlag flag : AccessFlag.getApplicableFlags(context)) {
			grid.getItems().add(flag);
		}
		grid.setMinWidth(140);
		popover = new PopOver(grid);
		popover.setAnimated(false);
		popover.setTitle(Lang.get("misc.access"));
		setOnAction(e -> {
			Point m = MouseInfo.getPointerInfo().getLocation();
			popover.show(this);
			popover.setX(m.getX() - 5);
			popover.setY(m.getY() - 35);
		});

	}

	/**
	 * @return Access flags.
	 */
	public int getAccess() {
		return access;
	}

	/**
	 * Set access.
	 */
	public void setAccess(int access) {
		this.access = access;
		setText(AccessFlag.sortAndToString(context, access));
		setGraphic(null);
		if (context == AccessFlag.Type.CLASS) {
			setGraphic(Icons.getClass(access));
		} else {
			setGraphic(Icons.getMember(access, context == AccessFlag.Type.METHOD));
		}
		if (updateTask != null) {
			updateTask.accept(access);
		}
	}

	/**
	 * Set access of given type.
	 * 
	 * @param flag
	 *            Type of access.
	 * @param additive
	 *            If the type should be added or removed.
	 */
	private void setAccess(AccessFlag flag, boolean additive) {
		boolean has = flag.has(this.access);
		if (has && !additive) {
			setAccess(flag.clear(this.access));
		} else if (!has && additive) {
			setAccess(flag.set(this.access));
		}
	}

	/**
	 * Set action for when access is updated.
	 */
	public void setUpdateTask(IntConsumer updateTask) {
		this.updateTask = updateTask;
	}

	/**
	 * Checkbox for modifying access of button.
	 * 
	 * @author Matt
	 */
	private final class AccessCheck extends CheckBox {
		public AccessCheck(AccessFlag flag) {
			setSelected(flag.has(getAccess()));
			setText(Misc.fixCase(flag.getName()));
			setGraphic(Icons.getAccess(flag, context == AccessFlag.Type.METHOD));
			// !isSelected() may seem counter-intuitive, but it seems to fire
			// before the property is changed. So ! becoems the expected value.
			setOnMousePressed(e -> setAccess(flag, !isSelected()));
		}
	}
}
