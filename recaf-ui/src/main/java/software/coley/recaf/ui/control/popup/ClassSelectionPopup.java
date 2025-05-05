package software.coley.recaf.ui.control.popup;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceRootTreeNode;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;

import java.util.function.Consumer;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Popup to show a selection of a class in a workspace.
 *
 * @author Matt Coley
 */
public class ClassSelectionPopup extends RecafStage {
	private final Consumer<ClassPathNode> classPathConsumer;
	private final PathNodeTree tree;

	/**
	 * @param actions
	 * @param configurationService
	 * @param explorerConfig
	 * @param workspace
	 * @param classPathConsumer
	 */
	@SuppressWarnings("all")
	public ClassSelectionPopup(@Nonnull Actions actions, @Nonnull CellConfigurationService configurationService,
	                           @Nonnull WorkspaceExplorerConfig explorerConfig, @Nonnull Workspace workspace,
	                           @Nonnull Consumer<ClassPathNode> classPathConsumer) {
		this.classPathConsumer = classPathConsumer;

		tree = new PathSelectionTree(configurationService, actions, classPathConsumer);
		tree.setShowRoot(false);
		tree.getStyleClass().add("border-muted");

		// Keyboard accept/cancel
		ObservableValue<Boolean> disable = tree.getSelectionModel().selectedItemProperty().map(i -> !(i.getValue() instanceof ClassPathNode));
		tree.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER && !disable.getValue()) {
				accept();
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});

		// Setup tree contents
		WorkspaceRootTreeNode rootItem = new WorkspaceRootTreeNode(explorerConfig, PathNodes.workspacePath(workspace)) {
			@Override
			protected void visitFiles(@Nonnull ResourcePathNode containingResourcePath, @Nonnull FileBundle bundle) {
				// Skip populating files in this tree.
				return;
			}
		};
		rootItem.build();
		TreeItems.recurseOpen(rootItem);
		tree.setRoot(rootItem);

		Button acceptButton = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), this::accept);
		Button cancelButton = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		acceptButton.disableProperty().bind(disable);
		HBox buttons = new HBox(acceptButton, new Spacer(), cancelButton);
		VBox layout = new VBox(tree, buttons);
		layout.setSpacing(10);
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setMinWidth(500);
		setMinHeight(230);
		setMaxHeight(460);
		titleProperty().bind(Lang.getBinding("dialog.title.select-class"));
		setScene(new RecafScene(layout, 500, 350));
	}

	private void accept() {
		TreeItem<PathNode<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
		if (selectedItem != null && selectedItem.getValue() instanceof ClassPathNode classPath)
			classPathConsumer.accept(classPath);
		hide();
	}

	/**
	 * Path node tree with some behavior tweaks to improve its UX as the primary selection element in this popup.
	 * Namely, changing the double click handling of cells to call {@link #accept()}.
	 */
	private class PathSelectionTree extends PathNodeTree {
		public PathSelectionTree(@Nonnull CellConfigurationService configurationService, @Nonnull Actions actions,
		                         @Nonnull Consumer<ClassPathNode> classPathConsumer) {
			super(configurationService, actions);
		}

		@Override
		protected void handleEnter(@Nonnull Actions actions, @Nonnull TreeItem<PathNode<?>> selected) {
			accept();
		}

		@Nonnull
		@Override
		protected WorkspaceTreeCell buildCell(@Nonnull CellConfigurationService configurationService) {
			return new WorkspaceTreeCell(contextSourceObjectPropertyProperty().get(), configurationService) {
				@Override
				protected void populate(@Nonnull PathNode<?> path) {
					// We want to specify the text/graphic of the cell, but not some of the click behaviors.
					// For instance, we don't want context menus or double-click to open classes.
					FxThreadUtil.run(() -> {
						configurationService.configureStyle(this, path);
						setText(configurationService.textOf(path));
						setGraphic(configurationService.graphicOf(path));
						setOnMouseClicked(e -> {
							// We want tree expand/collapse handling, but not "goto-declaration" handling.
							configurationService.clickHandlerOf(this, path, false).handle(e);

							// Double click should fire our class path consumer.
							if (e.getClickCount() == 2
									&& e.getButton() == MouseButton.PRIMARY
									&& path instanceof ClassPathNode classPath)
								accept();
						});
					});
				}
			};
		}
	}
}
