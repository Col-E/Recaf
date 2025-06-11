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
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.Lang;

import java.util.UUID;

/**
 * Facilitates creation and inspection of dockable UI content.
 *
 * @author Matt Coley
 * @see DockingLayoutManager
 * @see NavigationManager
 */
@ApplicationScoped
public class DockingManager {
	private static final Logger logger = Logging.get(DockingManager.class);

	private static final int GROUP_TOOLS = 1;

	private final Bento bento = Bento.newBento();

	/**
	 * @param windowManager
	 * 		Window manager to notify of docking created windows.
	 */
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

	/**
	 * @return Backing bento docking instance.
	 */
	@Nonnull
	public Bento getBento() {
		return bento;
	}

	/**
	 * @param identifier
	 * 		The identifier of some {@link DockSpace} to find and replace.
	 * @param supplier
	 * 		Supplier of a {@link DockSpace} to replace the existing space with.
	 *
	 * @return {@code true} when the existing space was found and replaced.
	 */
	public boolean replace(@Nonnull String identifier, @Nonnull SpaceSupplier supplier) {
		return bento.replaceSpace(identifier, supplier::get);
	}

	/**
	 * @param identifier
	 * 		The identifier of some {@link DockLayout} to find and replace.
	 * @param supplier
	 * 		Supplier of a {@link DockLayout} to replace the existing layout with.
	 *
	 * @return {@code true} when the existing layout was found and replaced.
	 */
	public boolean replace(@Nonnull String identifier, @Nonnull LayoutSupplier supplier) {
		return bento.replaceLayout(identifier, supplier::get);
	}

	/**
	 * The primary space is where content in the workspace is displayed when opened.
	 * Opening classes and files should place content in here.
	 *
	 * @return The primary tabbed space where most content is placed in the UI.
	 */
	@Nonnull
	public TabbedDockSpace getPrimaryTabbedSpace() {
		SpacePath path = bento.findSpace(DockingLayoutManager.ID_SPACE_WORKSPACE_PRIMARY);
		if (path != null && path.space() instanceof TabbedDockSpace tc)
			return tc;
		throw new IllegalStateException("Primary TabbedContent space could not be found");
	}

	/**
	 * Creates a {@link Dockable} that is assigned to the {@link #GROUP_TOOLS} group.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newToolDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newToolDockable(translationKey, d -> new FontIconView(icon), content);
	}


	/**
	 * Creates a {@link Dockable} that is assigned to the {@link #GROUP_TOOLS} group.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
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

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param title
	 * 		Dockable title.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull Ikon icon, @Nonnull Node content) {
		return newDockable(title, d -> new FontIconView(icon), content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param title
	 * 		Dockable title.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return bento.newDockableBuilder()
				.withTitle(title)
				.withNode(content)
				.withIconFactory(iconFactory)
				.build();
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newTranslatableDockable(translationKey, d -> new FontIconView(icon), content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return newTranslatableDockable(Lang.getBinding(translationKey), iconFactory, content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param titleBinding
	 * 		Dockable title translation binding.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull ObservableValue<String> titleBinding, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return bento.newDockableBuilder()
				.withTitle(titleBinding)
				.withNode(content)
				.withIconFactory(iconFactory)
				.build();
	}

	/**
	 * Supplier of a {@link DockSpace}.
	 */
	public interface SpaceSupplier {
		@Nonnull
		DockSpace get();
	}

	/**
	 * Supplier of a {@link DockLayout}.
	 */
	public interface LayoutSupplier {
		@Nonnull
		DockLayout get();
	}
}
