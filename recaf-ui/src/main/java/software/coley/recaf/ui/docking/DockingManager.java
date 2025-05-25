package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;
import org.slf4j.Logger;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.util.DragDropStage;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.Lang;

import java.util.UUID;

/**
 * Manages open docking regions and tabs.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DockingManager {
	private static final Logger logger = Logging.get(DockingManager.class);

	private static final int GROUP_TOOLS = 1;

	private final Bento bento = Bento.newBento();

	@Inject
	public DockingManager(@Nonnull WindowManager windowManager) {
		// Stages created via our docking framework need to be tracked in the window manager.
		bento.setStageFactory(originScene -> {
			DragDropStage stage = new DragDropStage(true);
			windowManager.register("dnd-" + UUID.randomUUID(), stage);
			return stage;
		});
		bento.setSceneFactory(RecafScene::new);
	}

	@Nonnull
	public Bento getBento() {
		return bento;
	}

	public boolean replace(@Nonnull String identifier, @Nonnull SpaceSupplier supplier) {
		return bento.replaceSpace(identifier, supplier::get);
	}

	public boolean replace(@Nonnull String identifier, @Nonnull LayoutSupplier supplier) {
		return bento.replaceLayout(identifier, supplier::get);
	}

	@Nonnull
	public TabbedDockSpace getPrimaryTabbedSpace() {
		SpacePath path = bento.findSpace(DockingLayoutManager.ID_SPACE_WORKSPACE_PRIMARY);
		if (path != null && path.space() instanceof TabbedDockSpace tc)
			return tc;
		throw new IllegalStateException("Primary TabbedContent space could not be found");
	}

	@Nonnull
	public Dockable newToolDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newToolDockable(translationKey, d -> new FontIconView(icon), content);
	}

	@Nonnull
	public Dockable newToolDockable(@Nonnull String translationKey, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return bento.newDockableBuilder()
				.withTitle(Lang.getBinding(translationKey))
				.withNode(content)
				.withIconFactory(iconFactory)
				.withClosable(false)
				.withDragGroup(GROUP_TOOLS)
				.withIdentifier(translationKey)
				.build();
	}

	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull Ikon icon, @Nonnull Node content) {
		return newDockable(title, d -> new FontIconView(icon), content);
	}

	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return bento.newDockableBuilder()
				.withTitle(title)
				.withNode(content)
				.withIconFactory(iconFactory)
				.build();
	}

	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newTranslatableDockable(translationKey, d -> new FontIconView(icon), content);
	}

	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return newTranslatableDockable(Lang.getBinding(translationKey), iconFactory, content);
	}

	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull ObservableValue<String> titleBinding, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return bento.newDockableBuilder()
				.withTitle(titleBinding)
				.withNode(content)
				.withIconFactory(iconFactory)
				.build();
	}

	public interface SpaceSupplier {
		@Nonnull
		DockSpace get();
	}

	public interface LayoutSupplier {
		@Nonnull
		DockLayout get();
	}
}
