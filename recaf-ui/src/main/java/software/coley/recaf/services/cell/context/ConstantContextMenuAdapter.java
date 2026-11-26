package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;

/**
 * Adapter for context menus on standalone constant values.
 *
 * @author Matt Coley
 */
public interface ConstantContextMenuAdapter extends ContextMenuAdapter {
	/**
	 * @param menu
	 * 		The menu to adapt.
	 * @param value
	 * 		Constant value the menu is for.
	 */
	void adaptConstantContextMenu(@Nonnull ContextMenu menu, @Nonnull Object value);
}
