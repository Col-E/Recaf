package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.ui.pane.editing.tabs.InheritancePane;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;

import java.util.function.Consumer;

/**
 * Injector for adding common {@link SideTabs} content to {@link AbstractContentPane} instances.
 *
 * @author Matt Coley
 */
@Dependent
public class SideTabsInjector {
	private static final Logger logger = Logging.get(SideTabsInjector.class);
	private final Instance<FieldsAndMethodsPane> fieldsAndMethodsPaneProvider;
	private final Instance<InheritancePane> inheritancePaneProvider;

	@Inject
	public SideTabsInjector(@Nonnull Instance<FieldsAndMethodsPane> fieldsAndMethodsPaneProvider,
	                        @Nonnull Instance<InheritancePane> inheritancePaneProvider) {
		this.fieldsAndMethodsPaneProvider = fieldsAndMethodsPaneProvider;
		this.inheritancePaneProvider = inheritancePaneProvider;
	}

	/**
	 * Registers a path update listener within the pane. Once a {@link AbstractContentPane#getPath() path} is
	 * assigned to the pane the side-tabs appropriate for the given content will be added.
	 * <p/>
	 * <b>NOTE:</b> Because path update listeners are handled before paths are passed off to
	 * {@link AbstractContentPane#getNavigableChildren()} we can add the side-tabs in the listener action
	 * and still have them be notified of the path update handled in {@link AbstractContentPane#onUpdatePath(PathNode)}.
	 *
	 * @param pane
	 * 		Pane to inject into.
	 */
	public void injectLater(@Nonnull AbstractContentPane<?> pane) {
		pane.addPathUpdateListener(Unchecked.cast(new TabAdder(pane)));
	}

	/**
	 * Adds the appropriate side-tabs for the pane's current assigned {@link AbstractContentPane#getPath() path}.
	 *
	 * @param pane
	 * 		Pane to inject into.
	 */
	public void injectNow(@Nonnull AbstractContentPane<?> pane) {
		PathNode<?> path = pane.getPath();
		if (path != null) injectInto(pane, path);
		else logger.warn("Attempted to inject side-tabs into content pane with no path registered yet");
	}

	private void injectInto(@Nonnull AbstractContentPane<?> pane, @Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath)
			injectClassTabs(pane);
		else
			logger.warn("Attempted to inject side-tabs into content pane with unsupported path type: {}", path.getClass().getSimpleName());
	}

	/**
	 * Adds class-specific content to the given pane.
	 *
	 * @param pane
	 * 		Pane to inject tabs into for {@link ClassInfo} content.
	 */
	private void injectClassTabs(@Nonnull AbstractContentPane<?> pane) {
		if (pane instanceof ClassNavigable classNavigable) {
			FieldsAndMethodsPane fieldsAndMethodsPane = fieldsAndMethodsPaneProvider.get();
			InheritancePane inheritancePane = inheritancePaneProvider.get();

			// Setup so clicking on items in fields-and-methods pane will synchronize with content in our class pane.
			fieldsAndMethodsPane.setupSelectionNavigationListener(classNavigable);

			// Setup side-tabs
			pane.addSideTab(new BoundTab(Lang.getBinding("fieldsandmethods.title"),
					Icons.getIconView(Icons.FIELD_N_METHOD),
					fieldsAndMethodsPane
			));
			pane.addSideTab(new BoundTab(Lang.getBinding("hierarchy.title"),
					CarbonIcons.FLOW,
					inheritancePane
			));
		} else {
			logger.warn("Called 'injectClassTabs' for non-class navigable content");
		}
	}

	/**
	 * Listener that waits for a non-null input. Once found, the listener removes itself.
	 */
	private class TabAdder implements Consumer<PathNode<?>> {
		private final AbstractContentPane<?> pane;

		private TabAdder(@Nonnull AbstractContentPane<?> pane) {this.pane = pane;}

		@Override
		public void accept(PathNode<?> path) {
			if (path != null) {
				injectNow(pane);
				pane.removePathUpdateListener(Unchecked.cast(this));
			}
		}
	}
}
