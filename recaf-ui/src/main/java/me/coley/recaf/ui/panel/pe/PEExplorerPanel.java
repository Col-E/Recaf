package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.PE;
import com.kichik.pecoff4j.io.PEParser;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A panel that displays information about an image's PE header.
 *
 * @author Wolfie / win32kbase
 */
public class PEExplorerPanel extends SplitPane {
	private static final Logger logger = Logging.get(PEExplorerPanel.class);

	private static final DosTableDisplayMode DOS_MODE = new DosTableDisplayMode();
	private static final FileTableDisplayMode FILE_MODE = new FileTableDisplayMode();
	private static final OptionalTableDisplayMode OPT_MODE = new OptionalTableDisplayMode();

	private final PE pe;

	private final TreeView<String> primaryTreeView = new TreeView<>();
	private final SizedDataTypeTable primaryTableView = new SizedDataTypeTable();

	private final TreeItem<String> itemDosHeader = new TreeItem<>("DOS header");
	private final TreeItem<String> itemNtHeaders = new TreeItem<>("NT headers");
	private final TreeItem<String> itemFileHeaders = new TreeItem<>("File header");
	private final TreeItem<String> itemOptionalHeaders = new TreeItem<>("Optional header");


	/**
	 * Create and setup the PE explorer panel.
	 *
	 * @param image
	 * 		The raw file image that the PE explorer will be showing information about.
	 */
	public PEExplorerPanel(byte[] image) throws IOException {
		try (InputStream stream = new ByteArrayInputStream(image)) {
			pe = PEParser.parse(stream);
		}
		setupPrimaryTree();
		setupPrimaryTable();
		// Setup panel
		getItems().addAll(primaryTreeView, primaryTableView);
		setDividerPositions(0.3);
		// Select initial view
		primaryTreeView.getSelectionModel().select(itemDosHeader);
	}

	/**
	 * Sets up the primary tree.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTree() {
		TreeItem<String> dummyRoot = new TreeItem<>();
		dummyRoot.getChildren().addAll(itemDosHeader, itemNtHeaders);

		primaryTreeView.setMinSize(this.getMaxWidth(), this.getMaxHeight());
		primaryTreeView.setRoot(dummyRoot);
		primaryTreeView.setShowRoot(false);
		primaryTreeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
	}


	/**
	 * Sets up the primary table.
	 */
	@SuppressWarnings("unchecked")
	private void setupPrimaryTable() {
		itemNtHeaders.getChildren().addAll(itemFileHeaders, itemOptionalHeaders);
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
		primaryTableView.getItems().clear();

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
			DOS_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemFileHeaders) {
			FILE_MODE.apply(pe, primaryTableView);
		} else if (newValue == itemOptionalHeaders) {
			OPT_MODE.apply(pe, primaryTableView);
		}  else if (newValue == itemNtHeaders) {
			// The NT headers item contains sub-items and has no values by itself
			//  - Maybe provide a dummy mode just to let the users know this?
		} else {
			logger.error("Unimplemented table item was selected");
		}
	}
}
