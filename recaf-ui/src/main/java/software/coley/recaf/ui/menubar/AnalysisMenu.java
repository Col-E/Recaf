package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.pane.CommentListPane;
import software.coley.recaf.ui.window.DeobfuscationWindow;
import software.coley.recaf.ui.pane.DocumentationPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;

import java.util.Optional;
import java.util.UUID;

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
	private final WindowManager windowManager;
	private final Instance<CommentListPane> commentListPaneProvider;
	private final Instance<DeobfuscationWindow> deobfuscationWindowProvider;

	@Inject
	public AnalysisMenu(@Nonnull WorkspaceManager workspaceManager,
	                    @Nonnull DockingManager dockingManager,
	                    @Nonnull WindowManager windowManager,
	                    @Nonnull Instance<CommentListPane> commentListPaneProvider,
	                    @Nonnull Instance<DeobfuscationWindow> deobfuscationWindowProvider) {
		super(workspaceManager);

		this.dockingManager = dockingManager;
		this.windowManager = windowManager;
		this.commentListPaneProvider = commentListPaneProvider;
		this.deobfuscationWindowProvider = deobfuscationWindowProvider;

		disableProperty().bind(hasWorkspace.not());
		textProperty().bind(getBinding("menu.analysis"));
		setGraphic(new FontIconView(CarbonIcons.CHART_CUSTOM));

		ActionMenuItem itemDeobfuscation = action("menu.analysis.deobfuscation", CarbonIcons.DEVELOPMENT, this::openDeobfuscation);
		itemDeobfuscation.disableProperty().bind(hasWorkspace.or(hasAgentWorkspace).not());
		getItems().add(itemDeobfuscation);

		ActionMenuItem itemListComments = action("menu.analysis.list-comments", CarbonIcons.CHAT, this::openCommentList);
		itemListComments.disableProperty().bind(hasWorkspace.or(hasAgentWorkspace).not());
		getItems().add(itemListComments);
	}

	/**
	 * Display the deobfuscation window.
	 */
	private void openDeobfuscation() {
		DeobfuscationWindow deobfuscationWindow = deobfuscationWindowProvider.get();
		deobfuscationWindow.show();
		deobfuscationWindow.requestFocus();
		windowManager.register("deobfuscation-" + UUID.randomUUID(), deobfuscationWindow);
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
			CommentListPane commentList = commentListPaneProvider.get();
			DockingTab tab = region.createTab(getBinding("menu.analysis.list-comments"), commentList);
			tab.setGraphic(new FontIconView(CarbonIcons.CHAT));
			tab.select();
		}
	}
}
