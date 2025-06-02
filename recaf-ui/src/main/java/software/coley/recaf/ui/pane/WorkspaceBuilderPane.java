package software.coley.recaf.ui.pane;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.dnd.DragAndDrop;
import software.coley.recaf.util.DirectoryChooserBuilder;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.PathLoadingManager;
import software.coley.recaf.workspace.model.Workspace;

import java.awt.Toolkit;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane to facilitate the creation and modification of {@link Workspace} contents.
 *
 * @author Matt Coley
 */
public class WorkspaceBuilderPane extends BorderPane {
	private static final Logger logger = Logging.get(WorkspaceBuilderPane.class);
	private final ObservableList<Path> paths = FXCollections.observableArrayList();
	private final ObjectProperty<Path> primary = new SimpleObjectProperty<>();

	/**
	 * Builder pane for adding content to a given workspace.
	 *
	 * @param pathLoadingManager
	 * 		Loading support.
	 * @param recentFilesConfig
	 * 		File dialog locations.
	 * @param workspace
	 * 		Workspace to append to.
	 * @param onComplete
	 * 		Completion task.
	 */
	public WorkspaceBuilderPane(@Nonnull PathLoadingManager pathLoadingManager,
	                            @Nonnull RecentFilesConfig recentFilesConfig,
	                            @Nonnull Workspace workspace,
	                            @Nonnull Runnable onComplete) {
		// Allow pasting file paths to append to the paths list.
		addEventFilter(KeyEvent.KEY_PRESSED, this::handlePaste);

		// Add dropped files to the paths list.
		DragAndDrop.installFileSupport(this, (region, event, files) -> {
			for (Path path : files) {
				if (!paths.contains(path))
					paths.add(path);
			}
		});

		// Create the vertical list of paths.
		VBox flow = new VBox();
		flow.setPadding(new Insets(15));
		flow.setAlignment(Pos.CENTER);
		flow.setFillWidth(true);
		flow.setSpacing(15);
		ObservableList<Node> flowNodes = flow.getChildren();
		paths.addListener((ListChangeListener<Path>) change -> {
			List<Node> newNodes = new ArrayList<>(flowNodes);
			while (change.next()) {
				for (Path path : change.getRemoved())
					newNodes.removeIf(child -> child instanceof FileEntry childEntry && path.equals(childEntry.path));
				for (Path path : change.getAddedSubList())
					newNodes.add(new FileEntry(path, false));
			}
			setNodes(newNodes, flowNodes);
		});

		// Label to prompt users to drag/drop files here.
		// Only shown when there are no items.
		BoundLabel dropPromptLabel = new BoundLabel(Lang.getBinding("tree.prompt"));
		dropPromptLabel.visibleProperty().bind(primary.isNull());
		dropPromptLabel.getStyleClass().addAll(Styles.TEXT_SUBTLE);
		dropPromptLabel.prefWidthProperty().bind(widthProperty());
		dropPromptLabel.setAlignment(Pos.CENTER);
		dropPromptLabel.setPadding(new Insets(15));

		// Button to load the workspace based on the order of paths given by the user.
		ActionButton appendWorkspaceButton = new ActionButton(CarbonIcons.WORKSPACE_IMPORT, Lang.getBinding("menu.file.addtoworkspace"), () -> {
			setDisable(true);
			pathLoadingManager.asyncAddSupportingResourcesToWorkspace(workspace, paths, ex -> {
				Toolkit.getDefaultToolkit().beep();
				logger.error("Failed to load supporting resources from selected files.", ex);
				ErrorDialogs.show(
						getBinding("dialog.error.loadsupport.title"),
						getBinding("dialog.error.loadsupport.header"),
						getBinding("dialog.error.loadsupport.content"),
						ex
				);
			}).whenComplete((_, error) -> {
				onComplete.run();
			});
		});
		appendWorkspaceButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);

		// Buttons to add additional paths.
		ActionButton addDir = new ActionButton(CarbonIcons.FOLDER_ADD, Lang.getBinding("dialog.file.open.directory"), () -> {
			File recentOpenDir = recentFilesConfig.getLastWorkspaceOpenDirectory().unboxingMap(File::new);
			DirectoryChooser chooser = new DirectoryChooserBuilder()
					.setInitialDirectory(recentOpenDir)
					.setTitle(Lang.get("dialog.file.open"))
					.build();

			// Show the prompt, update the paths list when complete
			File file = chooser.showDialog(getScene().getWindow());
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);

				Path path = file.toPath();
				if (!paths.contains(path))
					paths.add(path);
			}
		});
		ActionButton addFile = new ActionButton(CarbonIcons.DOCUMENT_ADD, Lang.getBinding("dialog.file.open.file"), () -> {
			File recentOpenDir = recentFilesConfig.getLastWorkspaceOpenDirectory().unboxingMap(File::new);
			FileChooser chooser = new FileChooserBuilder()
					.setInitialDirectory(recentOpenDir)
					.setTitle(Lang.get("dialog.file.open"))
					.build();

			// Show the prompt, update the paths list when complete
			File file = chooser.showOpenDialog(getScene().getWindow());
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);

				Path path = file.toPath();
				if (!paths.contains(path))
					paths.add(path);
			}
		});
		addDir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.LEFT_PILL);
		addFile.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.RIGHT_PILL);

		HBox buttons = new HBox(
				new HBox(addDir, addFile),
				new Spacer(),
				appendWorkspaceButton
		);
		buttons.setSpacing(10);
		buttons.setAlignment(Pos.CENTER);
		buttons.setPadding(new Insets(10));

		ScrollPane flowWrapper = new ScrollPane(flow);
		flowWrapper.setFitToWidth(true);
		flowWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		flow.prefWidthProperty().bind(flowWrapper.widthProperty());
		setTop(new Group(dropPromptLabel));
		setCenter(flowWrapper);
		setBottom(buttons);

		getStyleClass().addAll(Styles.BG_INSET);
		buttons.getStyleClass().addAll(Styles.BG_DEFAULT);
		buttons.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1px 0 0 0;");
	}

	/**
	 * Builder pane for creating a new workspace.
	 *
	 * @param pathLoadingManager
	 * 		Loading support.
	 * @param recentFilesConfig
	 * 		File dialog locations.
	 * @param onComplete
	 * 		Completion task.
	 */
	public WorkspaceBuilderPane(@Nonnull PathLoadingManager pathLoadingManager,
	                            @Nonnull RecentFilesConfig recentFilesConfig,
	                            @Nonnull Runnable onComplete) {
		// Allow pasting file paths to append to the paths list.
		addEventFilter(KeyEvent.KEY_PRESSED, this::handlePaste);

		// Add dropped files to the paths list.
		DragAndDrop.installFileSupport(this, (region, event, files) -> {
			for (Path path : files)
				addPath(path);
		});

		// Create the vertical list of paths.
		VBox flow = new VBox();
		flow.setPadding(new Insets(15));
		flow.setAlignment(Pos.CENTER);
		flow.setFillWidth(true);
		flow.setSpacing(15);
		ObservableList<Node> flowNodes = flow.getChildren();
		primary.addListener((ob, old, cur) -> {
			if (cur != null) {
				// Resort children so the primary path is always on-top.
				setNodes(new ArrayList<>(flowNodes), flowNodes);
			} else if (!paths.isEmpty()) {
				// Select the first path if we lost our selection.
				primary.set(paths.getFirst());
			}
		});
		paths.addListener((ListChangeListener<Path>) change -> {
			List<Node> newNodes = new ArrayList<>(flowNodes);
			while (change.next()) {
				for (Path path : change.getRemoved())
					newNodes.removeIf(child -> child instanceof FileEntry childEntry && path.equals(childEntry.path));
				for (Path path : change.getAddedSubList())
					newNodes.add(new FileEntry(path, true));
			}
			setNodes(newNodes, flowNodes);
		});

		// Label to prompt users to drag/drop files here.
		// Only shown when there are no items.
		BoundLabel dropPromptLabel = new BoundLabel(Lang.getBinding("tree.prompt"));
		dropPromptLabel.visibleProperty().bind(primary.isNull());
		dropPromptLabel.getStyleClass().addAll(Styles.TEXT_SUBTLE);
		dropPromptLabel.prefWidthProperty().bind(widthProperty());
		dropPromptLabel.setAlignment(Pos.CENTER);
		dropPromptLabel.setPadding(new Insets(15));

		// Button to load the workspace based on the order of paths given by the user.
		ActionButton openWorkspaceButton = new ActionButton(CarbonIcons.WORKSPACE_IMPORT, Lang.getBinding("menu.file.openworkspace"), () -> {
			Path primaryPath = primary.get();
			List<Path> supportingPaths = paths.stream().filter(p -> !p.equals(primaryPath)).toList();
			setDisable(true);
			pathLoadingManager.asyncNewWorkspace(primaryPath, supportingPaths, ex -> {
				Toolkit.getDefaultToolkit().beep();
				logger.error("Failed to open workspace for '{}'", primaryPath.getFileName(), ex);
				ErrorDialogs.show(
						getBinding("dialog.error.loadworkspace.title"),
						getBinding("dialog.error.loadworkspace.header"),
						getBinding("dialog.error.loadworkspace.content"),
						ex
				);
			}).whenComplete((workspace, error) -> {
				onComplete.run();
			});
		});
		openWorkspaceButton.disableProperty().bind(primary.isNull());
		openWorkspaceButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);

		// Buttons to add additional paths.
		ActionButton addDir = new ActionButton(CarbonIcons.FOLDER_ADD, Lang.getBinding("dialog.file.open.directory"), () -> {
			File recentOpenDir = recentFilesConfig.getLastWorkspaceOpenDirectory().unboxingMap(File::new);
			DirectoryChooser chooser = new DirectoryChooserBuilder()
					.setInitialDirectory(recentOpenDir)
					.setTitle(Lang.get("dialog.file.open"))
					.build();

			// Show the prompt, update the paths list when complete
			File file = chooser.showDialog(getScene().getWindow());
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);

				Path path = file.toPath();
				if (!paths.contains(path))
					paths.add(path);
			}
		});
		ActionButton addFile = new ActionButton(CarbonIcons.DOCUMENT_ADD, Lang.getBinding("dialog.file.open.file"), () -> {
			File recentOpenDir = recentFilesConfig.getLastWorkspaceOpenDirectory().unboxingMap(File::new);
			FileChooser chooser = new FileChooserBuilder()
					.setInitialDirectory(recentOpenDir)
					.setTitle(Lang.get("dialog.file.open"))
					.build();

			// Show the prompt, update the paths list when complete
			File file = chooser.showOpenDialog(getScene().getWindow());
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);

				Path path = file.toPath();
				if (!paths.contains(path))
					paths.add(path);
			}
		});
		addDir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.LEFT_PILL);
		addFile.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.RIGHT_PILL);

		HBox buttons = new HBox(
				new HBox(addDir, addFile),
				new Spacer(),
				openWorkspaceButton
		);
		buttons.setSpacing(10);
		buttons.setAlignment(Pos.CENTER);
		buttons.setPadding(new Insets(10));

		ScrollPane flowWrapper = new ScrollPane(flow);
		flowWrapper.setFitToWidth(true);
		flowWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		flow.prefWidthProperty().bind(flowWrapper.widthProperty());
		setTop(new Group(dropPromptLabel));
		setCenter(flowWrapper);
		setBottom(buttons);

		getStyleClass().addAll(Styles.BG_INSET);
		buttons.getStyleClass().addAll(Styles.BG_DEFAULT);
		buttons.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1px 0 0 0;");
	}

	/**
	 * Handle adding files via the clipboard when pasting.
	 *
	 * @param e
	 * 		Key press event.
	 */
	private void handlePaste(KeyEvent e) {
		if (e.isControlDown() && e.getCode() == KeyCode.V) {
			Clipboard clipboard = Clipboard.getSystemClipboard();

			// Handle files
			for (File file : clipboard.getFiles())
				addPath(file.toPath());

			// Handle text which may be a path/url to a file
			String url = clipboard.getUrl();
			String string = clipboard.getString();
			if (string != null) {
				// Check if it is a file path
				if (new File(string).exists())
					addPath(Paths.get(string));

				// Check if it is a file uri
				url = string;
			}
			try {
				URI u = URI.create(url);
				if ("file".equals(u.getScheme())) {
					String host = u.getHost();
					if (host != null)
						return;
					String path = u.getPath();
					File filePath = new File(path);
					if (filePath.exists())
						addPath(filePath.toPath());
				}
			} catch (IllegalArgumentException _) {}
		}
	}

	/**
	 * @param path
	 * 		Path to add.
	 */
	private void addPath(@Nonnull Path path) {
		if (paths.isEmpty())
			primary.set(path);
		if (!paths.contains(path))
			paths.add(path);
	}

	/**
	 * Assigns the temporary list, one ordered, to the child list container.
	 *
	 * @param temp
	 * 		Temporary items to sort.
	 * @param children
	 * 		Children list to update with sorted items.
	 */
	private void setNodes(@Nonnull List<Node> temp, @Nonnull ObservableList<Node> children) {
		temp.sort((o1, o2) -> {
			int i1 = o1 instanceof FileEntry e ? e.path == primary.get() ? Integer.MIN_VALUE : paths.indexOf(e.path) : 0;
			int i2 = o2 instanceof FileEntry e ? e.path == primary.get() ? Integer.MIN_VALUE : paths.indexOf(e.path) : 0;
			return Integer.compare(i1, i2);
		});
		temp.forEach(n -> {
			if (n instanceof FileEntry childentry)
				childentry.updateButtons();
		});
		children.setAll(temp);
	}

	private class FileEntry extends HBox {
		private final Path path;
		private final ActionButton up;
		private final ActionButton down;

		FileEntry(@Nonnull Path path, boolean showPrimary) {
			this.path = path;

			String fileName = path.getFileName().toString();
			Node graphic = Files.isDirectory(path) ?
					Icons.getIconView(Icons.FOLDER, 32) :
					Icons.getIconView(Icons.getIconPathForFileExtension(IOUtil.getExtension(path)), 32);
			Label nameLabel = new Label(fileName, graphic);
			nameLabel.setGraphicTextGap(10);

			// Button to remove this path from the inputs.
			ActionButton remove = new ActionButton(CarbonIcons.TRASH_CAN, Lang.getBinding("misc.remove"), () -> {
				paths.remove(path);
				if (primary.get().equals(path))
					primary.set(null);
			});
			remove.setMinWidth(100);
			remove.setPrefWidth(100);

			// Button to mark this path as the primary one.
			ActionButton markPrimary = new ActionButton(CarbonIcons.PIN, Lang.getBinding("dialog.file.primary"), () -> primary.set(path));
			markPrimary.getStyleClass().addAll(Styles.LEFT_PILL);
			markPrimary.visibleProperty().bind(primary.isNotEqualTo(path));
			markPrimary.setMinWidth(100);
			markPrimary.setPrefWidth(100);

			// Up and down arrows to change supporting item order.
			up = new ActionButton(CarbonIcons.ARROW_UP, () -> {
				int i = paths.indexOf(path);
				if (i > 0) {
					int off = paths.get(i - 1) == primary.get() ? 2 : 1;
					paths.remove(i);
					paths.add(i - off, path);
				}
			});
			down = new ActionButton(CarbonIcons.ARROW_DOWN, () -> {
				int i = paths.indexOf(path);
				if (i >= 0) {
					int off = i < paths.size() - 1 && paths.get(i + 1) == primary.get() ? 2 : 1;
					paths.remove(i);
					paths.add(i + off, path);
				}
			});
			up.getStyleClass().add(showPrimary ? Styles.CENTER_PILL : Styles.LEFT_PILL);
			down.getStyleClass().addAll(Styles.RIGHT_PILL);
			up.visibleProperty().bind(primary.isNotEqualTo(path));
			down.visibleProperty().bind(primary.isNotEqualTo(path));
			if (showPrimary) {
				up.prefHeightProperty().bind(markPrimary.heightProperty());
				down.prefHeightProperty().bind(markPrimary.heightProperty());
			}
			updateButtons();

			HBox box = showPrimary ? new HBox(markPrimary, up, down) : new HBox(up, down);
			HBox buttons = new HBox(new Group(box), remove);
			buttons.spacingProperty().bind(markPrimary.visibleProperty().map(v -> v ? 8 : 0));
			buttons.setAlignment(Pos.CENTER);

			Label primaryMarker = new BoundLabel(Lang.getBinding("dialog.file.primary"));
			primaryMarker.setPadding(new Insets(0, 0, 0, 15));
			primaryMarker.setTextFill(Color.YELLOW);
			primaryMarker.maxWidth(Integer.MAX_VALUE);
			primaryMarker.setAlignment(Pos.CENTER);
			primaryMarker.setGraphic(new FontIconView(CarbonIcons.STAR_FILLED, Color.YELLOW));
			primaryMarker.visibleProperty().bind(primary.isEqualTo(path));

			setAlignment(Pos.CENTER);
			setPadding(new Insets(5, 10, 5, 10));
			getChildren().addAll(nameLabel, new Group(primaryMarker), new Spacer(), buttons);
			getStyleClass().addAll(Styles.BG_DEFAULT, Styles.BORDER_DEFAULT);
		}

		/**
		 * Update the up/down arrow enabled states based on this {@link #path} position in {@link #paths}.
		 */
		private void updateButtons() {
			// Ensure we discount the primary path item (as if it does not exist since it is pinned to the top)
			List<Path> filtered = paths.stream().filter(p -> p != primary.get()).toList();
			int i = filtered.indexOf(path);
			up.setDisable(i < 1);
			down.setDisable(i == filtered.size() - 1);
		}
	}
}
