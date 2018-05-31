package me.coley.recaf.ui.component;

import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.function.Consumer;

import org.controlsfx.control.GridCell;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.util.Callback;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Access;
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
	private final AccessContext context;
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
	private Consumer<Integer> updateTask;

	public AccessButton(AccessContext context) {
		this(context, 0);
	}

	public AccessButton(AccessContext context, int access) {
		this.context = context;
		setupPopover();
		setAccess(access);

	}

	/**
	 * Open editor window.
	 */
	private void setupPopover() {
		// Create grid of access modifiers
		GridView<Integer> grid = new GridView<>();
		grid.setCellHeight(25);
		grid.setCellWidth(110);
		grid.setCellFactory(new Callback<GridView<Integer>, GridCell<Integer>>() {
			public GridCell<Integer> call(GridView<Integer> gridView) {
				return new GridCell<Integer>() {
					@Override
					protected void updateItem(Integer flags, boolean empty) {
						super.updateItem(flags, empty);
						if (empty || flags == null) {
							setGraphic(null);
						} else {
							setGraphic(new AccessCheck(flags));
						}
					}
				};
			}
		});
		int[] modifiers = null;
		switch (context) {
		case CLASS:
			modifiers = Access.CLASS_MODIFIERS_ARRAY;
			break;
		case CONSTRUCTOR:
			modifiers = Access.CONSTRUCTOR_MODIFIERS_ARRAY;
			break;
		case FIELD:
			modifiers = Access.FIELD_MODIFIERS_ARRAY;
			break;
		case INTERFACE:
			modifiers = Access.INTERFACE_MODIFIERS_ARRAY;
			break;
		case METHOD:
			modifiers = Access.METHOD_MODIFIERS_ARRAY;
			break;
		case PARAM:
			modifiers = Access.PARAM_MODIFIERS_ARRAY;
			break;
		default:
			Logging.error("Unknown context for access?");
			modifiers = new int[] {};
			break;
		}
		for (int mod : modifiers) {
			grid.getItems().add(mod);
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
	 * 
	 * @param access
	 */
	public void setAccess(int access) {
		this.access = access;
		setGraphic(null);
		if (context == AccessContext.CLASS || context == AccessContext.INTERFACE) {
			setGraphic(Icons.getClassExtended(access));
		} else {
			setGraphic(Icons.getMember(context == AccessContext.METHOD, access));
		}
		if (updateTask != null) {
			updateTask.accept(access);
		}
	}

	/**
	 * Set access of given type.
	 * 
	 * @param access
	 *            Type of access.
	 * @param additive
	 *            If the type should be added or removed.
	 */
	private void setAccess(int access, boolean additive) {
		boolean has = Access.hasAccess(this.access, access);
		if (has && !additive) {
			setAccess(this.access & ~access);
		} else if (!has && additive) {
			setAccess(this.access | access);
		}
	}

	/**
	 * Set action for when access is updated.
	 * 
	 * @param updateTask
	 */
	public void setUpdateTask(Consumer<Integer> updateTask) {
		this.updateTask = updateTask;
	}

	/**
	 * Checkbox for modifying access of button.
	 * 
	 * @author Matt
	 */
	private final class AccessCheck extends CheckBox {
		public AccessCheck(int access) {
			setSelected(Access.hasAccess(getAccess(), access));
			String text = Misc.fixCase(Access.name(access));
			if (context == AccessContext.FIELD && text.equals("Bridge")) {
				text = "Volatile";
			}
			setText(text);
			setGraphic(Icons.getAccess(access, context));
			// !isSelected() may seem counter-intuitive, but it seems to fire
			// before the property is changed. So ! becoems the expected value.
			setOnMousePressed(e -> setAccess(access, !isSelected()));
		}
	}

	/**
	 * Enumeration of types of access groups to display.
	 * 
	 * @author Matt
	 */
	public static enum AccessContext {
		CLASS, INTERFACE, CONSTRUCTOR, METHOD, FIELD, PARAM
	}
}
