package me.coley.recaf.ui.pane.pe;


import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.util.logging.Logging;
import me.martinez.pe.ImagePeHeaders;
import me.martinez.pe.ImageSectionHeader;
import me.martinez.pe.io.CadesBufferStream;
import me.martinez.pe.io.LittleEndianReader;
import org.slf4j.Logger;

/**
 * A panel that displays information about an image's PE header.
 *
 * @author Wolfie / win32kbase
 */
public class PEExplorerPane extends SplitPane implements FileRepresentation {
	private static final Logger logger = Logging.get(PEExplorerPane.class);

	private static final DosTableDisplayMode DOS_MODE = new DosTableDisplayMode();
	private static final FileTableDisplayMode FILE_MODE = new FileTableDisplayMode();
	private static final OptionalTableDisplayMode OPT_MODE = new OptionalTableDisplayMode();
	private static final SectionTableDisplayMode SECTION_MODE = new SectionTableDisplayMode();

	private final TreeView<String> primaryTreeView = new TreeView<>();
	private final SizedDataTypeTable primaryTableView = new SizedDataTypeTable();

	private final TreeItem<String> itemDosHeader = new TreeItem<>("DOS header");
	private final TreeItem<String> itemNtHeaders = new TreeItem<>("NT headers");
	private final TreeItem<String> itemFileHeaders = new TreeItem<>("File header");
	private final TreeItem<String> itemOptionalHeaders = new TreeItem<>("Optional header");
	private final TreeItem<String> itemSectionHeaders = new TreeItem<>("Section headers");

	private FileInfo fileInfo;
	private ImagePeHeaders pe;

	/**
	 * Create and setup the PE explorer panel.
	 */
	public PEExplorerPane()   {
		setupPrimaryTree();
		setupPrimaryTable();
		// Setup panel
		getItems().addAll(primaryTreeView, primaryTableView);
		setDividerPositions(0.3);
		// Select initial view
		primaryTreeView.getSelectionModel().select(itemDosHeader);
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return fileInfo;
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		this.fileInfo = newValue;
		CadesBufferStream stream = new CadesBufferStream(newValue.getValue());
		LittleEndianReader reader = new LittleEndianReader(stream);
		pe = ImagePeHeaders.read(reader);
		// Reset section headers
		ImageSectionHeader[] sectionTable = pe.sectionHeaders;
		itemSectionHeaders.getChildren().clear();
		for (ImageSectionHeader header : sectionTable) {
			TreeItem<String> sectionItem = new TreeItem<>(header.getName());
			itemSectionHeaders.getChildren().add(sectionItem);
		}
	}

	/**
	 * Sets up the primary tree.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTree() {
		TreeItem<String> dummyRoot = new TreeItem<>();
		dummyRoot.getChildren().addAll(itemDosHeader, itemNtHeaders);

		primaryTreeView.setMinSize(getMaxWidth(), getMaxHeight());
		primaryTreeView.setRoot(dummyRoot);
		primaryTreeView.setShowRoot(false);
		primaryTreeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
	}


	/**
	 * Sets up the primary table.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTable() {
		itemNtHeaders.getChildren().addAll(itemFileHeaders, itemOptionalHeaders, itemSectionHeaders);
		itemNtHeaders.setExpanded(true);
		primaryTableView.setSortPolicy(param -> false);
	}

	/**
	 * This function listens in for a new selected item on the primary tree view.
	 * When a different item in the tree view is selected, this function
	 * will set up the table view to have the correct contents.
	 *
	 * @param observable
	 * 		Wrapper.
	 * @param oldValue
	 * 		Old selected item.
	 * @param newValue
	 * 		New selected item.
	 */
	@SuppressWarnings("unchecked")
	private void onSelectionChange(ObservableValue<? extends TreeItem<String>> observable,
								   TreeItem<String> oldValue,
								   TreeItem<String> newValue) {
		if (newValue == null || pe == null)
			return;

		if (primaryTableView.getColumns().isEmpty()) {
			TableColumn<TableGeneric, String> member = new TableColumn<>("Member");
			TableColumn<TableGeneric, String> value = new TableColumn<>("Value");
			TableColumn<TableGeneric, String> meaning = new TableColumn<>("Meaning");
			member.setCellValueFactory(new PropertyValueFactory<>("member"));
			value.setCellValueFactory(new PropertyValueFactory<>("value"));
			meaning.setCellValueFactory(new PropertyValueFactory<>("meaning"));
			primaryTableView.getColumns().addAll(member, value, meaning);
		}

		logger.debug("Selected item: {}", newValue.getValue());

		if (newValue == itemDosHeader) {
			primaryTableView.getItems().clear();
			DOS_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemFileHeaders) {
			primaryTableView.getItems().clear();
			FILE_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemOptionalHeaders) {
			primaryTableView.getItems().clear();
			OPT_MODE.apply(pe, primaryTableView);
		} else {
			ObservableList<TreeItem<String>> children = itemSectionHeaders.getChildren();

			for (int i = 0; i < children.size(); i++) {
				if (children.get(i) == newValue) {
					primaryTableView.getItems().clear();
					ImageSectionHeader sectionHeader = pe.sectionHeaders[i];
					SECTION_MODE.apply(sectionHeader, primaryTableView);
					break;
				}
			}
		}
	}
}
