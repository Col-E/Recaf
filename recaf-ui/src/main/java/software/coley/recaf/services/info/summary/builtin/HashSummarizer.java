package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Summarizer that shows input file hash information.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class HashSummarizer implements ResourceSummarizer {
	private final CellConfigurationService configurationService;

	@Inject
	public HashSummarizer(@Nonnull CellConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		List<Node> grids = new ArrayList<>();
		if (resource instanceof WorkspaceFileResource fileResource)
			grids.add(generateHashDisplay(workspace, fileResource));
		for (WorkspaceFileResource embedded : resource.getEmbeddedResources().values())
			grids.add(generateHashDisplay(workspace, embedded));

		// Skip if no file resources were found.
		if (grids.isEmpty())
			return false;

		// Add title then all hash displays.
		BoundLabel title = new BoundLabel(Lang.getBinding("service.analysis.hashing"));
		title.getStyleClass().add(Styles.TITLE_4);
		consumer.appendSummary(title);
		for (Node grid : grids)
			consumer.appendSummary(grid);

		return true;
	}

	@Nonnull
	@SuppressWarnings("deprecation")
	private Node generateHashDisplay(@Nonnull Workspace workspace, @Nonnull WorkspaceFileResource fileResource) {
		FileInfo fileInfo = fileResource.getFileInfo();
		String name = fileInfo.getName();
		byte[] content = fileInfo.getRawContent();
		String md5 = Hashing.md5().hashBytes(content).toString();
		String sha1 = Hashing.sha1().hashBytes(content).toString();
		String sha256 = Hashing.sha256().hashBytes(content).toString();
		String sha512 = Hashing.sha512().hashBytes(content).toString();

		// Generate a TableView of the hash information.
		FilePathNode path = PathNodes.filePath(workspace, fileResource, fileResource.getFileBundle(), fileInfo);
		VBox container = new VBox();
		container.setSpacing(10);

		// Skip file title when the resource is the primary resource of the workspace, as that is already shown in the header.
		if (fileResource != workspace.getPrimaryResource()) {
			HBox fileTitle = new HBox(configurationService.graphicOf(path), new Label(configurationService.textOf(path)));
			fileTitle.setAlignment(Pos.CENTER_LEFT);
			fileTitle.setSpacing(10);
			fileTitle.setPadding(new Insets(8, 8, 0, 0));
			fileTitle.getStyleClass().add(Styles.TEXT_BOLD);
			container.getChildren().add(fileTitle);
		}

		// Create TableView
		TableView<HashEntry> tableView = new TableView<>();
		tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		tableView.setFixedCellSize(40);

		// Create columns
		TableColumn<HashEntry, String> typeColumn = new TableColumn<>();
		TableColumn<HashEntry, String> hashColumn = new TableColumn<>();
		TableColumn<HashEntry, Void> actionColumn = new TableColumn<>();
		typeColumn.setMinWidth(70);
		typeColumn.setMaxWidth(75);
		typeColumn.setReorderable(false);
		typeColumn.setResizable(false);
		typeColumn.textProperty().bind(Lang.getBinding("service.analysis.hashing.type"));
		typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().type()));
		hashColumn.setReorderable(false);
		hashColumn.textProperty().bind(Lang.getBinding("service.analysis.hashing.value"));
		hashColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().value()));
		hashColumn.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					setText(item);
					getStyleClass().add("mono-text");
				}
			}
		});
		actionColumn.setPrefWidth(50);
		actionColumn.setMaxWidth(50);
		actionColumn.setMinWidth(50);
		actionColumn.setReorderable(false);
		actionColumn.setResizable(false);
		actionColumn.setCellFactory(col -> new TableCell<>() {
			private final ActionButton copyButton = new ActionButton(CarbonIcons.COPY, this::runAction);
			private Runnable action;

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
					action = null;
				} else {
					HashEntry hashEntry = getTableView().getItems().get(getIndex());
					action = () -> {
						ClipboardUtil.copyString(hashEntry.value());
						Animations.animateSuccess(this, 500);
					};
					setGraphic(copyButton);
				}
			}

			private void runAction() {
				if (action != null) action.run();
			}
		});
		tableView.getColumns().addAll(typeColumn, hashColumn, actionColumn);

		// Add hash entries
		ObservableList<HashEntry> hashEntries = FXCollections.observableArrayList(
				new HashEntry("MD5", md5),
				new HashEntry("SHA-1", sha1),
				new HashEntry("SHA-256", sha256),
				new HashEntry("SHA-512", sha512)
		);
		tableView.setItems(hashEntries);
		tableView.prefHeightProperty().bind(tableView.fixedCellSizeProperty().multiply(Bindings.size(tableView.getItems()).add(1.05)));
		tableView.minHeightProperty().bind(tableView.prefHeightProperty());
		tableView.maxHeightProperty().bind(tableView.prefHeightProperty());

		container.getChildren().add(tableView);
		return container;
	}

	private record HashEntry(@Nonnull String type, @Nonnull String value) {}
}
