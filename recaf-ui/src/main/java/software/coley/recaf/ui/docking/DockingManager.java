package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;
import org.slf4j.Logger;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.control.Headers;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.Lang;

import java.util.UUID;
import java.util.function.Supplier;

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

	private final Bento bento = new Bento();

	/**
	 * @param windowManager
	 * 		Window manager to notify of docking created windows.
	 */
	@Inject
	public DockingManager(@Nonnull WindowManager windowManager) {
		// Stages created via our docking framework need to be tracked in the window manager.
		bento.stageBuilding().setStageFactory(originScene -> {
			DragDropStage stage = new DragDropStage(true);
			windowManager.register("dnd-" + UUID.randomUUID(), stage);
			return stage;
		});
		bento.stageBuilding().setSceneFactory((sourceScene, content, width, height) -> {
			content.getStyleClass().add("bg-inset");
			return new RecafScene(content, width, height);
		});

		// Due to how we style the headers, we want the drawing to be clipped.
		bento.controlsBuilding().setHeadersFactory(ClippedHeaders::new);
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
	 * 		The identifier of some {@link DockContainer} to find and replace.
	 * @param supplier
	 * 		Supplier of a {@link DockContainer} to replace the existing container with.
	 *
	 * @return {@code true} when the existing container was found and replaced.
	 * {@code false} when the existing container was not found.
	 */
	public boolean replace(@Nonnull String identifier, @Nonnull Supplier<DockContainer> supplier) {
		return bento.search().replaceContainer(identifier, supplier);
	}

	/**
	 * The primary container is where content in the workspace is displayed when opened.
	 * Opening classes and files should place content in here.
	 *
	 * @return The primary docking container leaf where most content is placed in the UI.
	 */
	@Nonnull
	public DockContainerLeaf getPrimaryDockingContainer() {
		var path = bento.search().container(DockingLayoutManager.ID_CONTAINER_WORKSPACE_PRIMARY);
		if (path != null && path.tailContainer() instanceof DockContainerLeaf leaf)
			return leaf;
		throw new IllegalStateException("Primary docking leaf could not be found");
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
		Dockable dockable = bento.dockBuilding().dockable(translationKey);
		dockable.titleProperty().bind(Lang.getBinding(translationKey));
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		dockable.setClosable(false);
		dockable.setDragGroup(GROUP_TOOLS);
		return dockable;
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
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.setTitle(title);
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		return dockable;
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
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.titleProperty().bind(titleBinding);
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		return dockable;
	}

	/**
	 * Headers impl that configures render clipping.
	 */
	private static class ClippedHeaders extends Headers {
		/**
		 * @param container
		 * 		Parent container.
		 * @param orientation
		 * 		Which axis to layout children on.
		 * @param side
		 * 		Side in the parent container where tabs are displayed.
		 */
		private ClippedHeaders(@Nonnull DockContainerLeaf container, @Nonnull Orientation orientation, @Nonnull Side side) {
			super(container, orientation, side);
			setupClip();
		}
	}
}
