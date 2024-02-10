package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.pane.CommentListPane;
import software.coley.recaf.ui.pane.DocumentationPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.WorkspaceManager;

import java.util.Optional;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Analysis menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class AnalysisMenu extends WorkspaceAwareMenu {
	private final DockingManager dockingManager;
	private final Instance<CommentListPane> commentListProvider;

	@Inject
	public AnalysisMenu(@Nonnull WorkspaceManager workspaceManager,
						@Nonnull DockingManager dockingManager,
						@Nonnull Instance<CommentListPane> commentListProvider) {
		super(workspaceManager);

		this.dockingManager = dockingManager;
		this.commentListProvider = commentListProvider;

		disableProperty().bind(hasWorkspace.not());
		textProperty().bind(getBinding("menu.analysis"));
		setGraphic(new FontIconView(CarbonIcons.CHART_CUSTOM));

		ActionMenuItem itemListComments = action("menu.analysis.list-comments", CarbonIcons.CHAT, this::openCommentList);
		itemListComments.disableProperty().bind(hasWorkspace.or(hasAgentWorkspace).not());
		getItems().add(itemListComments);
	}

	/**
	 * Display the comments list in a new tab.
	 */
	private void openCommentList() {
		// Check for tabs with the panel already open.
		Optional<DockingTab> tabWithListPane = dockingManager.getDockTabs().stream()
				.filter(tab -> tab.getContent() instanceof CommentListPane)
				.findFirst();
		if (tabWithListPane.isPresent()) {
			// It already exists, focus the tab.
			DockingTab dockingTab = tabWithListPane.get();
			dockingTab.select();
			Node content = dockingTab.getContent();
			FxThreadUtil.run(() -> Animations.animateNotice(content, 1000));
			content.requestFocus();
		} else {
			// It does not exist, create a tab and put a new list pane in it.
			Optional<DockingTab> tabWithDocContent = dockingManager.getDockTabs().stream()
					.filter(tab -> tab.getContent() instanceof DocumentationPane)
					.findFirst();
			DockingRegion region = tabWithDocContent.isPresent() ?
					tabWithDocContent.get().getRegion() :
					dockingManager.getPrimaryRegion();
			CommentListPane commentList = commentListProvider.get();
			DockingTab tab = region.createTab(getBinding("menu.analysis.list-comments"), commentList);
			tab.setGraphic(new FontIconView(CarbonIcons.CHAT));
			tab.select();
		}
	}
}
