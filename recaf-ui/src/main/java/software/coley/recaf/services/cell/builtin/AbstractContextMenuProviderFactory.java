package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.cell.ContextMenuProviderFactory;
import software.coley.recaf.services.cell.IconProviderService;
import software.coley.recaf.services.cell.TextProviderService;
import software.coley.recaf.services.navigation.Actions;

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
