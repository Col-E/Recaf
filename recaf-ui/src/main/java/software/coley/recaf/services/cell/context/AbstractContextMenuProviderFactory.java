package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;

/**
 * Common base to context menu provider factories.
 *
 * @author Matt Coley
 */
public abstract class AbstractContextMenuProviderFactory implements ContextMenuProviderFactory {
	protected final TextProviderService textService;
	protected final IconProviderService iconService;
	protected final Actions actions;

	protected AbstractContextMenuProviderFactory(@Nonnull TextProviderService textService,
												 @Nonnull IconProviderService iconService,
												 @Nonnull Actions actions) {
		this.textService = textService;
		this.iconService = iconService;
		this.actions = actions;
	}
}
