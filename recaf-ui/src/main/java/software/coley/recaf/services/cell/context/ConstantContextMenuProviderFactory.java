package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;

/**
 * Provides context menus for standalone constant values.
 *
 * @author Matt Coley
 */
public interface ConstantContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param value
	 * 		Constant value to create a menu for.
	 *
	 * @return Context menu provider for the constant value.
	 */
	@Nonnull
	default ContextMenuProvider getConstantContextMenuProvider(@Nonnull Object value) {
		return emptyProvider();
	}
}
