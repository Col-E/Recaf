package me.coley.recaf.ui.pane.elf;

import javafx.beans.value.ObservableValue;
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
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSectionHeader;
import net.fornwall.jelf.ElfSegment;
import org.slf4j.Logger;

/**
 * A panel that displays information about an image's ELF header.
 *
 * @author Wolfie / win32kbase
 */
public class ElfExplorerPane extends SplitPane implements FileRepresentation {
	private static final Logger logger = Logging.get(ElfExplorerPane.class);

	public static final int MEMBER_COLUMN_INDEX = 0;
	public static final int VALUE_COLUMN_INDEX = 1;
	public static final int MEANING_COLUMN_INDEX = 2;

	private final TreeView<String> primaryTreeView = new TreeView<>();
	private final SizedDataTypeTable primaryTableView = new SizedDataTypeTable();

	private final TreeItem<String> itemElfHeader = new TreeItem<>("ELF header");
	private final TreeItem<String> itemProgramHeaders = new TreeItem<>("Program headers");
	private final TreeItem<String> itemSectionHeaders = new TreeItem<>("Section headers");
	private final TreeItem<String> itemStringTable = new TreeItem<>("String table");
	private final TreeItem<String> itemDynamicStringTable = new TreeItem<>("Dynamic string table");

	private final ElfHeaderDisplayMode elfHeaderDisplayMode = new ElfHeaderDisplayMode();
	private final ProgramHeaderDisplayMode programHeaderDisplayMode = new ProgramHeaderDisplayMode();
	private final SectionHeaderDisplayMode sectionHeaderDisplayMode = new SectionHeaderDisplayMode();
	private final StringTableDisplayMode stringTableDisplayMode = new StringTableDisplayMode();

	private FileInfo fileInfo;
	private ElfFile elfFile;

	/**
	 * Create and setup the ELF explorer panel.
	 */
	public ElfExplorerPane() {
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
		try {
			this.elfFile = ElfFile.from(newValue.getValue());
		} catch (Exception e) {
			logger.error("Failed to parse ELF file: {}", e.getMessage());
		}

		for (int i = 0; i < elfFile.e_phnum; i++) {
			TreeItem<String> programHeaderItem = new TreeItem<>(String.format("Header %d", i));
			itemProgramHeaders.getChildren().add(programHeaderItem);
		}

		for (int i = 1; i < elfFile.e_shnum; i++) {
			ElfSectionHeader sectionHeader = elfFile.getSection(i).header;
			TreeItem<String> sectionHeaderItem = new TreeItem<>(sectionHeader.getName());
			itemSectionHeaders.getChildren().add(sectionHeaderItem);
		}

		primaryTreeView.getSelectionModel().select(itemElfHeader);
		programHeaderDisplayMode.onUpdate(elfFile);
		sectionHeaderDisplayMode.onUpdate(elfFile);
		stringTableDisplayMode.onUpdate(elfFile);
	}

	/**
	 * Sets up the primary tree.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTree() {
		TreeItem<String> dummyRoot = new TreeItem<>();
		dummyRoot.getChildren().addAll(itemElfHeader, itemProgramHeaders, itemSectionHeaders, itemStringTable, itemDynamicStringTable);

		primaryTreeView.setMinSize(getMaxWidth(), getMaxHeight());
		primaryTreeView.setRoot(dummyRoot);
		primaryTreeView.setShowRoot(false);
		primaryTreeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
	}


	/**
	 * Sets up the primary table.
	 */
	private void setupPrimaryTable() {
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
		if (newValue == null || elfFile == null)
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

		if (newValue == itemElfHeader) {
			elfHeaderDisplayMode.apply(elfFile, primaryTableView);
		} else if (newValue == itemStringTable) {
			stringTableDisplayMode.apply(elfFile.getStringTable(), primaryTableView);
		} else if (newValue == itemDynamicStringTable) {
			stringTableDisplayMode.apply(elfFile.getDynamicStringTable(), primaryTableView);
		} else {
			int programHeaderIndex = itemProgramHeaders.getChildren().indexOf(newValue);
			int sectionHeaderIndex = itemSectionHeaders.getChildren().indexOf(newValue);

			if (programHeaderIndex != -1) {
				ElfSegment programHeader = elfFile.getProgramHeader(programHeaderIndex);
				programHeaderDisplayMode.apply(programHeader, primaryTableView);
			} else if (sectionHeaderIndex != -1) {
				// Section header indexes start at 1, so we need to add one or else we'll be off by one.
				ElfSectionHeader sectionHeader = elfFile.getSection(sectionHeaderIndex + 1).header;
				sectionHeaderDisplayMode.apply(sectionHeader, primaryTableView);
			} else {
				primaryTableView.getColumns().clear();
			}
		}
	}
}
