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
import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableGeneric;
import me.coley.recaf.util.logging.Logging;
import me.martinez.pe.ExportEntry;
import me.martinez.pe.LibraryExport;
import me.martinez.pe.LibraryImports;
import me.martinez.pe.PeImage;
import me.martinez.pe.headers.ImageSectionHeader;
import me.martinez.pe.io.CadesBufferStream;
import me.martinez.pe.io.LittleEndianReader;
import me.martinez.pe.util.ParseResult;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
	private static final ImportTableDisplayMode IMPORT_MODE = new ImportTableDisplayMode();
	private static final ExportTableDisplayMode EXPORT_MODE = new ExportTableDisplayMode();

	public static final int MEMBER_COLUMN_INDEX = 0;
	public static final int VALUE_COLUMN_INDEX = 1;
	public static final int MEANING_COLUMN_INDEX = 2;

	private final TreeItem<String> dummyRoot = new TreeItem<>();
	private final TreeView<String> primaryTreeView = new TreeView<>();
	private final SizedDataTypeTable primaryTableView = new SizedDataTypeTable();

	private final TreeItem<String> itemDosHeader = new TreeItem<>("DOS header");
	private final TreeItem<String> itemNtHeaders = new TreeItem<>("NT headers");
	private final TreeItem<String> itemFileHeaders = new TreeItem<>("File header");
	private final TreeItem<String> itemOptionalHeaders = new TreeItem<>("Optional header");
	private final TreeItem<String> itemSectionHeaders = new TreeItem<>("Section headers");
	private final TreeItem<String> itemImportDirectory = new TreeItem<>("Import directory");
	private final TreeItem<String> itemExportDirectory = new TreeItem<>("Export directory");

	private FileInfo fileInfo;
	private PeImage pe;

	/**
	 * Create and setup the PE explorer panel.
	 */
	public PEExplorerPane() {
		setupPrimaryTree();
		setupPrimaryTable();
		// Setup panel
		getItems().addAll(primaryTreeView, primaryTableView);
		setDividerPositions(0.3);
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
		pe = PeImage.read(reader).getOk();

		// Remove the export directory option if the file doesn't have one
		if (dummyRoot.getChildren().contains(itemExportDirectory) && pe.exports == null) {
			dummyRoot.getChildren().remove(itemExportDirectory);
		}

			// Reset section headers
		ArrayList<ParseResult<ImageSectionHeader>> sectionHeaders = pe.sectionHeaders;
		itemSectionHeaders.getChildren().clear();
		for (ParseResult<ImageSectionHeader> headerResult : sectionHeaders) {
			if (headerResult.isOk()) {
				TreeItem<String> sectionItem = new TreeItem<>(headerResult.getOk().getName());
				itemSectionHeaders.getChildren().add(sectionItem);
			}
		}

		// Add libraries to import directory
		if (pe.exports.isOk()) {
			List<LibraryImports> importsList = pe.imports.getOk();
			for (int i = 0; i < importsList.size(); i++) {
				LibraryImports imports = importsList.get(i);
				TreeItem<String> libraryItem = new TreeItem<>(imports.name);
				itemImportDirectory.getChildren().add(libraryItem);
			}
		}


		itemImportDirectory.setExpanded(true);

		// Select initial view
		primaryTreeView.getSelectionModel().select(itemDosHeader);
	}

	/**
	 * Sets up the primary tree.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTree() {
		dummyRoot.getChildren().addAll(itemDosHeader, itemNtHeaders, itemImportDirectory, itemExportDirectory);

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

		primaryTableView.getColumns().clear();
		TableColumn<TableGeneric, String> member = new TableColumn<>("Member");
		TableColumn<TableGeneric, String> value = new TableColumn<>("Value");
		TableColumn<TableGeneric, String> meaning = new TableColumn<>("Meaning");
		member.setCellValueFactory(new PropertyValueFactory<>("member"));
		value.setCellValueFactory(new PropertyValueFactory<>("value"));
		meaning.setCellValueFactory(new PropertyValueFactory<>("meaning"));
		primaryTableView.getColumns().addAll(member, value, meaning);

		primaryTableView.getItems().clear();

		if (newValue == itemDosHeader) {
			DOS_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemFileHeaders) {
			FILE_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemOptionalHeaders) {
			OPT_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemExportDirectory) {
			EXPORT_MODE.apply(pe.exports.getOkOrDefault(null), primaryTableView);
		} else {
			ObservableList<TreeItem<String>> sectionHeadersChildren = itemSectionHeaders.getChildren();
			ObservableList<TreeItem<String>> importDirectoryChildren = itemImportDirectory.getChildren();
			int sectionIndex = sectionHeadersChildren.indexOf(newValue);
			int importIndex = importDirectoryChildren.indexOf(newValue);

			if (sectionIndex != -1) {
				ImageSectionHeader sectionHeader = pe.sectionHeaders.get(sectionIndex).getOk();
				SECTION_MODE.apply(sectionHeader, primaryTableView);
			} else if (importIndex != -1) {
				List<LibraryImports> importsList = pe.imports.getOkOrDefault(null);
				IMPORT_MODE.apply(importsList != null ? importsList.get(importIndex) : null, primaryTableView);
			} else {
				primaryTableView.getColumns().clear();
			}
		}
	}
}
