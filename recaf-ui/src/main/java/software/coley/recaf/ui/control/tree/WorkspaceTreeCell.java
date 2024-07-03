package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import javafx.scene.control.TreeCell;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;

import java.util.function.Function;

/**
 * Cell for rendering {@link PathNode} items.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeCell extends TreeCell<PathNode<?>> {
	private final Function<PathNode<?>, ContextSource> sourceFunc;
	private final CellConfigurationService configurationService;

	/**
	 * @param source
	 * 		Context requester source.
	 * @param configurationService
	 * 		Service to configure cell content.
	 */
	public WorkspaceTreeCell(@Nonnull ContextSource source,
	                         @Nonnull CellConfigurationService configurationService) {
		this(path -> source, configurationService);
	}

	/**
	 * @param sourceFunc
	 * 		Context requester source function.
	 * @param configurationService
	 * 		Service to configure cell content.
	 */
	public WorkspaceTreeCell(@Nonnull Function<PathNode<?>, ContextSource> sourceFunc,
	                         @Nonnull CellConfigurationService configurationService) {
		this.sourceFunc = sourceFunc;
		this.configurationService = configurationService;
	}

	@Override
	protected void updateItem(PathNode<?> item, boolean empty) {
		super.updateItem(item, empty);

		// Always reset the cell between item updates.
		configurationService.reset(this);

		// Apply new cell properties if the item is valid.
		if (!empty && item != null) configurationService.configure(this, item, sourceFunc.apply(item));
	}
}
