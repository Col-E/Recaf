package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.COFFHeader;
import com.kichik.pecoff4j.DOSHeader;
import com.kichik.pecoff4j.OptionalHeader;
import com.kichik.pecoff4j.io.PEParser;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.tree.item.FileItem;
import com.kichik.pecoff4j.PE;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;
import sun.net.www.content.text.Generic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A panel that displays information about an image's PE header.
 *
 * @author Wolfie / win32kbase
 */

public class PEExplorerPanel extends FlowPane {
    private static final Logger logger = Logging.get(PEExplorerPanel.class);
    private static PE pe;

    // You can't have two roots in a TreeView, so you have to create an invisible parent item
    private static final TreeItem dummyRoot = new TreeItem();
    private static final TreeView primaryTreeView = new TreeView();
    private static final TableView primaryTableView = new TableView();

    private static final TreeItem DOSHeaderTree = new TreeItem("DOS header");

    private static final TreeItem NTHeadersTree = new TreeItem("NT headers");
    private static final TreeItem FileHeaderTree = new TreeItem("File header");
    private static final TreeItem OptionalHeaderTree = new TreeItem("Optional header");

    /**
     * Create and setup the PE explorer panel.
     *
     * @param item The file item that the PE explorer will be showing information about.
     */
    public PEExplorerPanel(FileItem item) throws IOException {
        byte[] image = RecafUI.getController().getWorkspace().getResources().getFile(item.getFileName()).getValue();
        InputStream stream = new ByteArrayInputStream(image);

        pe = PEParser.parse(stream);

        SetupPrimaryTree();
        SetupPrimaryTable();
    }

    /**
     * Sets up the primary tree.
     */
    void SetupPrimaryTree() {
        this.getChildren().add(primaryTreeView);
        primaryTreeView.setMinSize(this.getMaxWidth(), this.getMaxHeight());

        dummyRoot.getChildren().addAll(DOSHeaderTree, NTHeadersTree);

        primaryTreeView.setRoot(dummyRoot);
        primaryTreeView.setShowRoot(false);

        primaryTreeView.getSelectionModel().selectedItemProperty().addListener(this::TreeListener);
    }

    /**
     * Sets up the primary table.
     */
    void SetupPrimaryTable() {
        this.getChildren().add(primaryTableView);

        NTHeadersTree.getChildren().addAll(FileHeaderTree, OptionalHeaderTree);
    }

    /**
     * This function listens in for a new selected item on the primary tree view.
     * When a different item in the tree view is selected, this function
     * will set up the table view to have the correct contents.
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    void TreeListener(ObservableValue observable, Object oldValue, Object newValue) {
        TreeItem selectedItem = (TreeItem)newValue;
        primaryTableView.getColumns().clear();
        primaryTableView.getItems().clear();

        logger.debug("Selected item: {}", selectedItem.getValue());

        if (selectedItem == DOSHeaderTree) {
            PopulateDOSHeader();
        }
        else if (selectedItem == FileHeaderTree) {
            PopulateFileHeader();
        }
        else {
            logger.error("Unimplemented table item was selected");
        }
    }

    /**
     * Fills the primary table view with all of the
     * information contained in the DOS header.
     */
    void PopulateDOSHeader() {
        DOSHeader dos = pe.getDosHeader();

        TableColumn<GenericWord, String> member = new TableColumn("Member");
        member.setCellValueFactory(new PropertyValueFactory<>("member"));
        TableColumn<GenericWord, Integer> value = new TableColumn("Value");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        primaryTableView.getColumns().addAll(member, value);

        int[] rw = dos.getReserved();
        int[] rw2 = dos.getReserved2();
        primaryTableView.getItems().addAll(
                new GenericWord("Magic number", dos.getMagic()),
                new GenericWord("Bytes on last page of file", dos.getUsedBytesInLastPage()),
                new GenericWord("Pages in file", dos.getFileSizeInPages()),
                new GenericWord("Relocation count", dos.getNumRelocationItems()),
                new GenericWord("Size of header in paragraphs", dos.getHeaderSizeInParagraphs()),
                new GenericWord("Minimum extra paragraphs needed", dos.getMinExtraParagraphs()),
                new GenericWord("Maximum extra paragraphs needed", dos.getMaxExtraParagraphs()),
                new GenericWord("Initial SS value", dos.getInitialSS()),
                new GenericWord("Initial SP value", dos.getInitialSP()),
                new GenericWord("Checksum", dos.getChecksum()),
                new GenericWord("Initial IP value", dos.getInitialIP()),
                new GenericWord("Initial relative CS value", dos.getInitialRelativeCS()),
                new GenericWord("File address of relocation table", dos.getAddressOfRelocationTable()),
                new GenericWord("Overlay number", dos.getOverlayNumber()),
                new GenericWord("Reserved words (4)", String.format("%d, %d, %d, %d", rw[0], rw[1], rw[2], rw[3])),
                new GenericWord("OEM identifier", dos.getOemId()),
                new GenericWord("OEM information", dos.getOemInfo()),
                new GenericWord("Reserved words (10)", String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d",
                                rw2[0], rw2[1], rw2[2], rw2[3], rw2[4], rw2[5], rw2[6], rw2[7], rw2[8], rw2[9])),
                new GenericWord("File address of new exe header", dos.getAddressOfNewExeHeader())
        );
    }

    void PopulateFileHeader() {
        COFFHeader FileHeader = pe.getCoffHeader();

        TableColumn<GenericWord, String> member = new TableColumn<>("Member");
        member.setCellValueFactory(new PropertyValueFactory<>("member"));
        TableColumn<GenericWord, Integer> value = new TableColumn<>("Value");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<GenericWord, String> meaning = new TableColumn<>("Meaning");
        value.setCellValueFactory(new PropertyValueFactory<>("meaning"));
        primaryTableView.getColumns().addAll(member, value);

        primaryTableView.getItems().addAll(
                new GenericWord("Machine", FileHeader.getMachine()),
                new GenericWord("Section count", FileHeader.getNumberOfSections(), false),
                new GenericWord("Time date stamp", FileHeader.getTimeDateStamp()),
                new GenericWord("Pointer to symbol table", FileHeader.getPointerToSymbolTable()),
                new GenericWord("Number of symbols", FileHeader.getNumberOfSymbols()),
                new GenericWord("Size of optional header", FileHeader.getSizeOfOptionalHeader()),
                // TODO: Characteristics in detail
                new GenericWord("Characteristics", FileHeader.getCharacteristics(), false)
        );
    }
}
