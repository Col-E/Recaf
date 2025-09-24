package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.RecafBuildConfig;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.BoundHyperlink;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.dnd.DragAndDrop;
import software.coley.recaf.ui.dnd.FileDropListener;
import software.coley.recaf.ui.dnd.WorkspaceLoadingDropListener;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.PathLoadingManager;

import java.awt.Toolkit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane displayed when first opening Recaf.
 *
 * @author Matt Coley
 */
@Dependent
public class WelcomePane extends BorderPane implements Navigable {
	private static final Logger logger = Logging.get(WelcomePane.class);

	@Inject
	public WelcomePane(@Nonnull RecentFilesConfig recentFiles,
	                   @Nonnull PathLoadingManager pathLoadingManager,
	                   @Nonnull WorkspaceLoadingDropListener listener) {
		FileDropListener disablingListener = (region, event, files) -> {
			// Disable when content is dragged over this panel while the workspace is loading.
			// There's not a great feedback mechanism without a goofy timeout so this silly hack works for now.
			setDisable(true);
			FxThreadUtil.delayedRun(5000, () -> setDisable(false));

			// Delegate to the original listener.
			listener.onDragDrop(region, event, files);
		};
		DragAndDrop.installFileSupport(this, disablingListener);

		int hyperlinkCount;
		BorderPane wrapper = new BorderPane();
		VBox versionPane = new VBox();
		VBox linksPane = new VBox();
		VBox recentsPane = new VBox();
		VBox dndPane = new VBox();
		linksPane.setPadding(new Insets(10));
		recentsPane.setPadding(new Insets(10));
		dndPane.setPadding(new Insets(10));
		{
			Label title = new Label("Recaf " + RecafBuildConfig.VERSION);
			Label subtitle = new Label("Build " + RecafBuildConfig.GIT_REVISION + " - " + RecafBuildConfig.GIT_DATE);
			Label outdated = new Label(Duration.ofMillis(System.currentTimeMillis() - RecafBuildConfig.BUILD_UNIX_TIME).toDays() + " " + Lang.get("welcome.dayssince"));
			title.getStyleClass().add(Styles.TITLE_2);
			outdated.getStyleClass().add(Styles.TEXT_SUBTLE);
			versionPane.setAlignment(Pos.CENTER);
			versionPane.getChildren().addAll(title, subtitle, outdated);
		}
		{
			Label title = new BoundLabel(Lang.getBinding("welcome.links"));
			Hyperlink home = new BoundHyperlink(Lang.getBinding("welcome.links.home"), Icons.getIconView(Icons.LOGO), "https://recaf.coley.software/home.html");
			Hyperlink docUser = new BoundHyperlink(Lang.getBinding("welcome.links.docs-user"), new FontIconView(CarbonIcons.PEDESTRIAN_CHILD), "https://recaf.coley.software/user/index.html");
			Hyperlink docDev = new BoundHyperlink(Lang.getBinding("welcome.links.docs-dev"), new FontIconView(CarbonIcons.PEDESTRIAN), "https://recaf.coley.software/dev/index.html");
			Hyperlink git = new BoundHyperlink(Lang.getBinding("welcome.links.github"), new FontIconView(CarbonIcons.LOGO_GITHUB), "https://github.com/Col-E/Recaf");
			Hyperlink discord = new BoundHyperlink(Lang.getBinding("welcome.links.discord"), Icons.getIconView(Icons.DISCORD), "https://discord.gg/Bya5HaA");
			Hyperlink jvms = new BoundHyperlink(Lang.getBinding("welcome.links.jvms"), Icons.getIconView(Icons.DUKE_THUMBS), "https://docs.oracle.com/javase/specs/jvms/se25/html/index.html");
			Hyperlink jvmsClass = new BoundHyperlink(Lang.getBinding("welcome.links.jvms.class"), Icons.getIconView(Icons.DUKE_TUMBLE), "https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html");
			Hyperlink jvmsInsns = new BoundHyperlink(Lang.getBinding("welcome.links.jvms.instructions"), Icons.getIconView(Icons.DUKE_THINK), "https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-6.html");

			title.getStyleClass().add(Styles.TITLE_2);
			docUser.setPadding(new Insets(0, 0, 0, 20));
			docDev.setPadding(new Insets(0, 0, 0, 20));
			git.setPadding(new Insets(0, 0, 0, 20));
			discord.setPadding(new Insets(0, 0, 0, 20));
			jvmsClass.setPadding(new Insets(0, 0, 0, 20));
			jvmsInsns.setPadding(new Insets(0, 0, 0, 20));
			linksPane.setAlignment(Pos.TOP_LEFT);
			linksPane.getChildren().addAll(title, home, docUser, docDev, git, discord, jvms, jvmsClass, jvmsInsns);
			linksPane.setSpacing(4);
			hyperlinkCount = (int) linksPane.getChildren().stream().filter(n -> n instanceof Hyperlink).count();
		}
		{
			Label title = new BoundLabel(Lang.getBinding("menu.file.recent"));
			title.getStyleClass().add(Styles.TITLE_2);

			recentsPane.setAlignment(Pos.TOP_LEFT);
			recentsPane.getChildren().addAll(title);
			recentsPane.setSpacing(4);

			var models = recentFiles.getRecentWorkspaces().getValue().stream()
					.filter(RecentFilesConfig.WorkspaceModel::canLoadWorkspace)
					.limit(hyperlinkCount)
					.toList();
			if (models.isEmpty())
				recentsPane.getChildren().add(new BoundLabel(Lang.getBinding("welcome.norecent")));
			for (RecentFilesConfig.WorkspaceModel model : models) {
				String extension = IOUtil.getExtension(model.primary().path());
				Node graphic = Icons.getIconView(Icons.getIconPathForFileExtension(extension));

				Hyperlink entry = new Hyperlink(model.primary().getSimpleName(), graphic);
				entry.setOnAction(e -> load(pathLoadingManager, model));
				recentsPane.getChildren().add(entry);
			}
		}
		{
			BoundLabel label = new BoundLabel(getBinding("welcome.dnd"));
			label.getStyleClass().add(Styles.TEXT_SUBTLE);
			dndPane.getChildren().addAll(label);
			dndPane.setAlignment(Pos.CENTER);
		}
		BorderPane middleWrapper = new BorderPane();
		middleWrapper.setLeft(linksPane);
		middleWrapper.setRight(recentsPane);
		wrapper.setTop(versionPane);
		wrapper.setCenter(middleWrapper);
		wrapper.setBottom(dndPane);


		VBox content = new VBox(new Group(wrapper));
		content.setFillWidth(true);
		content.setAlignment(Pos.CENTER);
		ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		scroll.setFitToHeight(true);
		scroll.maxHeightProperty().bind(heightProperty().subtract(5));
		setCenter(scroll);
		getStyleClass().add(Styles.BG_SUBTLE);
	}

	private void load(@Nonnull PathLoadingManager pathLoadingManager, @Nonnull RecentFilesConfig.WorkspaceModel model) {
		// Disable further interaction while loading
		setDisable(true);

		// Get paths from model
		Path primaryPath = Paths.get(model.primary().path());
		List<Path> supportingPaths = model.libraries().stream()
				.map(resource -> Paths.get(resource.path()))
				.toList();

		// Pass to loader
		pathLoadingManager.asyncNewWorkspace(primaryPath, supportingPaths, ex -> {
			setDisable(false);
			Toolkit.getDefaultToolkit().beep();
			logger.error("Failed to open recent workspace for '{}'", model.primary().getSimpleName(), ex);
			ErrorDialogs.show(
					getBinding("dialog.error.loadworkspace.title"),
					getBinding("dialog.error.loadworkspace.header"),
					getBinding("dialog.error.loadworkspace.content"),
					ex
			);
		});
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return PathNodes.unique("welcome");
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// no-op
	}
}
