package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.window.DeobfuscationWindow;

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
	private final WindowManager windowManager;
	private final Instance<DeobfuscationWindow> deobfuscationWindowProvider;

	@Inject
	public AnalysisMenu(@Nonnull WorkspaceManager workspaceManager,
	                    @Nonnull DockingManager dockingManager,
	                    @Nonnull WindowManager windowManager,
	                    @Nonnull Instance<DeobfuscationWindow> deobfuscationWindowProvider,
	                    @Nonnull Actions actions) {
		super(workspaceManager);

		this.windowManager = windowManager;
		this.deobfuscationWindowProvider = deobfuscationWindowProvider;

		disableProperty().bind(hasWorkspace.not());
		textProperty().bind(getBinding("menu.analysis"));
		setGraphic(new FontIconView(CarbonIcons.CHART_CUSTOM));

		MenuItem itemViewSummary = action("menu.analysis.summary", CarbonIcons.INFORMATION, actions::openSummary);
		itemViewSummary.disableProperty().bind(hasWorkspace.not());
		getItems().add(itemViewSummary);

		ActionMenuItem itemDeobfuscation = action("menu.analysis.deobfuscation", CarbonIcons.DEVELOPMENT, this::openDeobfuscation);
		itemDeobfuscation.disableProperty().bind(hasWorkspace.or(hasAgentWorkspace).not());
		getItems().add(itemDeobfuscation);

		ActionMenuItem itemListComments = action("menu.analysis.list-comments", CarbonIcons.CHAT, actions::openCommentList);
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
		deobfuscationWindow.setOnCloseRequest(e -> deobfuscationWindowProvider.destroy(deobfuscationWindow));
		windowManager.register("deobfuscation-" + UUID.randomUUID(), deobfuscationWindow);
	}
}
