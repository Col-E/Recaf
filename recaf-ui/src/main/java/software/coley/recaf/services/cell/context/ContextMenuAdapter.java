package software.coley.recaf.services.cell.context;

import javafx.scene.control.ContextMenu;
import software.coley.recaf.behavior.PrioritySortable;

/**
 * Adapts an existing {@link ContextMenu}. Used to customize content provided by {@link ContextMenuProvider} instances.
 *
 * @author Matt Coley
 */
public interface ContextMenuAdapter extends PrioritySortable {}
