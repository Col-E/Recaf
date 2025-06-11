package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.space.DockSpace;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.pane.CommentListPane;
import software.coley.recaf.ui.pane.DocumentationPane;
import software.coley.recaf.ui.window.DeobfuscationWindow;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;

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
		DockablePath docPanePath = null;
		for (DockablePath path : dockingManager.getBento().getAllDockables()) {
			Dockable dockable = path.dockable();
			Node node = dockable.nodeProperty().get();
			if (node instanceof CommentListPane) {
				path.space().selectDockable(dockable);
				FxThreadUtil.run(() -> {
					node.requestFocus();
					Animations.animateNotice(node, 1000);
				});
				return;
			} else if (node instanceof DocumentationPane) {
				docPanePath = path;
			}
		}

		// Not already open, gotta open a new one.
		DockSpace space = docPanePath != null ? docPanePath.space() : dockingManager.getPrimaryTabbedSpace();
		Dockable dockable = dockingManager.newTranslatableDockable("menu.analysis.list-comments", CarbonIcons.CHAT, commentListPaneProvider.get());
		space.addDockable(dockable);
	}
}
