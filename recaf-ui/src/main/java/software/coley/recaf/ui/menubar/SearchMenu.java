package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.pane.search.*;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.SceneUtils;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Search menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class SearchMenu extends WorkspaceAwareMenu {
	private final WindowFactory windowFactory;
	private final DockingManager dockingManager;

	@Inject
	public SearchMenu(@Nonnull WindowFactory windowFactory,
					  @Nonnull DockingManager dockingManager,
					  @Nonnull WorkspaceManager workspaceManager,
					  @Nonnull Instance<StringSearchPane> stringSearchPanes,
					  @Nonnull Instance<NumberSearchPane> numberSearchPanes,
					  @Nonnull Instance<ClassReferenceSearchPane> classReferenceSearchPanes,
					  @Nonnull Instance<MemberReferenceSearchPane> memberReferenceSearchPanes) {
		super(workspaceManager);

		this.windowFactory = windowFactory;
		this.dockingManager = dockingManager;

		textProperty().bind(getBinding("menu.search"));
		setGraphic(new FontIconView(CarbonIcons.SEARCH));

		getItems().add(action("menu.search.string", CarbonIcons.QUOTES,
				() -> openSearchPane("menu.search.string", CarbonIcons.QUOTES, stringSearchPanes)));
		getItems().add(action("menu.search.number", CarbonIcons.NUMBER_0,
				() -> openSearchPane("menu.search.number",CarbonIcons.NUMBER_0, numberSearchPanes)));
		getItems().add(action("menu.search.class.type-references", CarbonIcons.CODE_REFERENCE,
				() -> openSearchPane("menu.search.class.type-references",CarbonIcons.CODE_REFERENCE, classReferenceSearchPanes)));
		getItems().add(action("menu.search.class.member-references", CarbonIcons.CODE_REFERENCE,
				() -> openSearchPane("menu.search.class.member-references",CarbonIcons.CODE_REFERENCE, memberReferenceSearchPanes)));

		disableProperty().bind(hasWorkspace.not());
	}

	private void openSearchPane(@Nonnull String titleId, @Nonnull Ikon icon, @Nonnull Instance<? extends Pane> paneProvider) {
		// TODO: Migrate something like this into 'Actions' so people + context menus can easily
		//  do actions.newStringSearch().withSearchText("foo").withMode(mode);
		DockingRegion region = dockingManager.getRegions().stream()
				.filter(r -> r.getDockTabs().stream().anyMatch(t -> t.getContent() instanceof AbstractSearchPane))
				.findFirst().orElse(null);
		Pane content = paneProvider.get();
		if (region != null) {
			DockingTab tab = region.createTab(getBinding(titleId), content);
			tab.setGraphic(new FontIconView(icon));
			tab.select();
			FxThreadUtil.run(() -> SceneUtils.focus(content));
		} else {
			region = dockingManager.newRegion();
			DockingTab tab = region.createTab(getBinding(titleId), content);
			tab.setGraphic(new FontIconView(icon));
			RecafScene scene = new RecafScene((region));
			Stage window = windowFactory.createAnonymousStage(scene, getBinding("menu.search"), 800, 400);
			window.show();
			window.requestFocus();
		}
	}
}
