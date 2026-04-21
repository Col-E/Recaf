package software.coley.recaf.ui.pane.editing.binary;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import software.coley.recaf.info.ArscFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.ImageFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.android.AndroidRes;
import software.coley.recaf.util.android.AndroidRes.ResourceEntry;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the contents of a {@link ArscFileInfo}. These resource bundles are generally glorified KV pairs, but they
 * can also point to other files in the workspace. In such cases, we will display them here when selected.
 *
 * @author Matt Coley
 */
@Dependent
public class ArscPane extends BorderPane implements FileNavigable, UpdatableNavigable {
	private final Instance<DecodingXmlPane> xmlPaneProvider;
	private FilePathNode path;

	@Inject
	public ArscPane(@Nonnull Instance<DecodingXmlPane> xmlPaneProvider) {
		this.xmlPaneProvider = xmlPaneProvider;
	}

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			FileInfo info = filePath.getValue();
			if (info instanceof ArscFileInfo arscInfo)
				refresh(arscInfo);
		}
	}

	private void refresh(@Nonnull ArscFileInfo arscInfo) {
		AndroidRes resources = arscInfo.getResourceInfo();

		// Group entries by type and fill a placeholder if there are no resources.
		Map<String, List<ResourceEntry>> entriesByType = resources.getEntriesByType();
		if (entriesByType.isEmpty()) {
			setCenter(new BoundLabel(Lang.getBinding("arscviewer.no-resources-placeholder")));
			return;
		}

		// Create a tab for each resource type.
		Workspace workspace = path.getValueOfType(Workspace.class);
		TabPane tabs = new TabPane();
		entriesByType.forEach((type, entries) -> {
			Tab tab = new Tab(type);
			tab.setClosable(false);
			tab.setContent(createTypeContent(workspace, resources, entries));
			tabs.getTabs().add(tab);
		});
		setCenter(tabs);
	}

	@Nonnull
	private Node createTypeContent(@Nullable Workspace workspace,
	                               @Nonnull AndroidRes resources,
	                               @Nonnull List<ResourceEntry> entries) {
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.VERTICAL);

		// Base table view for the resource entries.
		// We'll use its selection model to drive the image and XML previews below.
		TableView<ResourceEntry> table = createEntryTable(workspace, resources, entries);

		// Try making image/xml preview panes. If there are no entries of that type, these will be null.
		Node imageGrid = createImageGrid(workspace, table);
		Node xmlPreview = createXmlPreview(workspace, table);

		// Add the panes in a layout that is appropriate.
		// A triple stack looks weird, so for both (seen with drawables) we'll split them horizontally.
		if (imageGrid != null && xmlPreview != null) {
			split.getItems().add(new SplitPane(imageGrid, xmlPreview));
		} else {
			if (imageGrid != null)
				split.getItems().add(imageGrid);
			if (xmlPreview != null)
				split.getItems().add(xmlPreview);
		}

		// Optional complex value table. These shouldn't appear in resource types that would result in a triple split
		// like mentioned above. If this does ever happen, I'm sorry.
		BorderPane content = new BorderPane(table);
		if (entries.stream().anyMatch(ResourceEntry::isComplex)) {
			TableView<ComplexValue> complexTable = createComplexValueTable(resources);
			table.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
				if (cur == null || !cur.isComplex())
					complexTable.getItems().clear();
				else
					complexTable.setItems(FXCollections.observableArrayList(toComplexValues(cur)));
			});
			content.setBottom(complexTable);
		}
		split.getItems().add(content);
		return split;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private TableView<ResourceEntry> createEntryTable(@Nullable Workspace workspace,
	                                                  @Nonnull AndroidRes resources,
	                                                  @Nonnull List<ResourceEntry> entries) {
		TableView<ResourceEntry> table = new TableView<>(FXCollections.observableArrayList(entries));
		TableColumn<ResourceEntry, String> nameColumn = new TableColumn<>("Name");
		TableColumn<ResourceEntry, String> hexIdColumn = new TableColumn<>("ID");
		TableColumn<ResourceEntry, Number> decimalIdColumn = new TableColumn<>("Decimal ID");
		TableColumn<ResourceEntry, String> valueColumn = new TableColumn<>("Value");

		nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name()));
		hexIdColumn.setCellValueFactory(param -> new SimpleStringProperty(formatId(param.getValue().id())));
		decimalIdColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().id()));
		valueColumn.setCellValueFactory(param -> new SimpleStringProperty(formatValue(resources, param.getValue())));

		nameColumn.setPrefWidth(180);
		hexIdColumn.setPrefWidth(100);
		decimalIdColumn.setPrefWidth(100);
		valueColumn.setPrefWidth(360);

		table.getColumns().addAll(nameColumn, hexIdColumn, decimalIdColumn, valueColumn);
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		return table;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private static TableView<ComplexValue> createComplexValueTable(@Nonnull AndroidRes resources) {
		TableView<ComplexValue> table = new TableView<>();
		TableColumn<ComplexValue, String> keyColumn = new TableColumn<>("Key");
		TableColumn<ComplexValue, String> valueColumn = new TableColumn<>("Value");

		keyColumn.setCellValueFactory(param -> new SimpleStringProperty(formatComplexKey(resources, param.getValue().key())));
		valueColumn.setCellValueFactory(param -> new SimpleStringProperty(formatBinaryValue(resources, param.getValue().value())));

		keyColumn.setPrefWidth(260);
		valueColumn.setPrefWidth(520);

		table.getColumns().addAll(keyColumn, valueColumn);
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPlaceholder(new BoundLabel(Lang.getBinding("arscviewer.no-complex-values-placeholder")));
		table.setPrefHeight(160);
		return table;
	}

	@Nullable
	private Node createImageGrid(@Nullable Workspace workspace, @Nonnull TableView<ResourceEntry> table) {
		List<ResolvedEntry> imageEntries = table.getItems().stream()
				.map(entry -> resolve(workspace, entry))
				.filter(resolved -> resolved.path() != null && resolved.path().getValue() instanceof ImageFileInfo)
				.toList();
		if (imageEntries.isEmpty())
			return null;

		FlowPane grid = new FlowPane();
		grid.setHgap(12);
		grid.setVgap(12);
		grid.setPadding(new Insets(12));

		Map<FileInfo, Node> tileMap = new HashMap<>();
		for (ResolvedEntry resolved : imageEntries) {
			Node tile = createImageTile(resolved);
			tileMap.put(resolved.path().getValue(), tile);
			grid.getChildren().add(tile);
		}

		ScrollPane scroll = new ScrollPane(grid);
		table.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			FilePathNode resolvedPath = resolve(workspace, cur).path();
			if (resolvedPath != null
					&& resolvedPath.getValue() instanceof ImageFileInfo image
					&& tileMap.get(image) instanceof Node n) {
				centerNodeInScrollPane(scroll, n);
				Animations.animateNotice(n, 250);
			}
		});

		scroll.setFitToWidth(true);
		scroll.setPrefHeight(260);
		return scroll;
	}

	private static void centerNodeInScrollPane(@Nonnull ScrollPane scroll, @Nonnull Node node) {
		double ch = scroll.getContent().getBoundsInLocal().getHeight();
		double y = node.getBoundsInParent().getMinY() + node.getBoundsInParent().getHeight() / 2;
		double vh = scroll.getViewportBounds().getHeight();
		scroll.setVvalue(Math.max(0, Math.min(1, (y - vh / 2) / (ch - vh))));
	}

	@Nonnull
	private static Node createImageTile(@Nonnull ResolvedEntry resolved) {
		ResourceEntry entry = resolved.entry();
		FileInfo fileInfo = resolved.path().getValue();
		Image image = new Image(new ByteArrayInputStream(fileInfo.getRawContent()), 96, 96, true, true);
		ImageView imageView = new ImageView(image);
		imageView.setFitWidth(96);
		imageView.setFitHeight(96);
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);

		Label name = new Label(entry.name());
		Label id = new Label(formatId(entry.id()));
		id.getStyleClass().add(Styles.TEXT_SUBTLE);

		VBox tile = new VBox(6, imageView, name, id);
		tile.setAlignment(Pos.CENTER);
		tile.setPadding(new Insets(8));
		tile.setPrefWidth(180);
		tile.getStyleClass().addAll(Styles.BORDER_DEFAULT);
		return tile;
	}

	@Nullable
	private Node createXmlPreview(@Nullable Workspace workspace, @Nonnull TableView<ResourceEntry> table) {
		// Skip if there are no XML entries.
		List<ResolvedEntry> xmlEntries = table.getItems().stream()
				.map(entry -> resolve(workspace, entry))
				.filter(resolved -> resolved.path() != null && resolved.path().getValue() instanceof BinaryXmlFileInfo)
				.toList();
		if (xmlEntries.isEmpty())
			return null;

		// Set placeholder text until an XML entry is selected.
		DecodingXmlPane xmlPane = xmlPaneProvider.get();
		String placeholder = Lang.get("arscviewer.xml-placeholder");
		xmlPane.setText(placeholder);
		table.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			FilePathNode resolvedPath = resolve(workspace, cur).path();
			if (resolvedPath != null && resolvedPath.getValue() instanceof BinaryXmlFileInfo)
				xmlPane.onUpdatePath(resolvedPath);
			else {
				xmlPane.setText(placeholder);
			}
		});
		return new BorderPane(xmlPane);
	}

	@Nonnull
	private static List<ComplexValue> toComplexValues(@Nonnull ResourceEntry entry) {
		List<ComplexValue> values = new ArrayList<>();
		entry.complexValues().forEach((key, value) -> values.add(new ComplexValue(key, value)));
		return values;
	}

	@Nonnull
	private static String formatValue(@Nonnull AndroidRes resources, @Nonnull ResourceEntry entry) {
		if (entry.isComplex())
			return entry.complexValues().size() + " " + Lang.get("arscviewer.complex-entries-suffix");
		BinaryResourceValue value = entry.simpleValue();
		if (value == null)
			return "";
		String stringValue = entry.stringValue();
		if (stringValue != null)
			return stringValue;
		String path = entry.resourcePath();
		if (path != null)
			return path;
		return formatBinaryValue(resources, value);
	}

	@Nonnull
	private static String formatBinaryValue(@Nonnull AndroidRes resources, @Nonnull BinaryResourceValue value) {
		return switch (value.type()) {
			case REFERENCE, DYNAMIC_REFERENCE -> {
				String name = resources.getResName(value.data());
				yield name == null ? "@" + formatId(value.data()) : "@" + name + " (" + formatId(value.data()) + ")";
			}
			case ATTRIBUTE, DYNAMIC_ATTRIBUTE -> {
				String name = resources.getResName(value.data());
				yield name == null ? "?" + formatId(value.data()) : "?" + name + " (" + formatId(value.data()) + ")";
			}
			case INT_BOOLEAN -> value.data() == 0 ? "false" : "true";
			case INT_HEX -> formatId(value.data());
			case INT_COLOR_ARGB8, INT_COLOR_RGB8, INT_COLOR_ARGB4, INT_COLOR_RGB4 ->
					"#" + Integer.toHexString(value.data());
			default -> value.type() + ": " + value.data();
		};
	}

	@Nonnull
	private static String formatComplexKey(@Nonnull AndroidRes resources, int key) {
		String name = resources.getResName(key);
		return name == null ? formatId(key) : name + " (" + formatId(key) + ")";
	}

	@Nonnull
	private static String formatPath(@Nullable Workspace workspace, @Nonnull ResourceEntry entry) {
		String resourcePath = entry.resourcePath();
		if (resourcePath == null)
			return "";
		return resolve(workspace, entry).path() == null ?
				resourcePath + " " + Lang.get("arscviewer.unknown-resource") : resourcePath;
	}

	@Nonnull
	private static String formatId(int id) {
		return "0x%08X".formatted(id);
	}

	@Nonnull
	private static ResolvedEntry resolve(@Nullable Workspace workspace, @Nonnull ResourceEntry entry) {
		// These should point to files in the workspace.
		// If they're not there, oh well...
		String resourcePath = entry.resourcePath();
		if (workspace == null || resourcePath == null)
			return new ResolvedEntry(entry, null);
		return new ResolvedEntry(entry, workspace.findFile(resourcePath));
	}

	/**
	 * Complex KV pair.
	 *
	 * @param key
	 * 		Complex value key.
	 * @param value
	 * 		Complex value.
	 */
	private record ComplexValue(int key, @Nonnull BinaryResourceValue value) {}

	/**
	 * Resolved {@link ResourceEntry}.
	 *
	 * @param entry
	 * 		Resource entry.
	 * @param path
	 * 		Resolved file path in the workspace for the entry's content, if it could be found.
	 */
	private record ResolvedEntry(@Nonnull ResourceEntry entry, @Nullable FilePathNode path) {}
}
