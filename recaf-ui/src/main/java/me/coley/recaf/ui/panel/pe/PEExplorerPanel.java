package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.COFFHeader;
import com.kichik.pecoff4j.DOSHeader;
import com.kichik.pecoff4j.OptionalHeader;
import com.kichik.pecoff4j.io.PEParser;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.tree.item.FileItem;
import com.kichik.pecoff4j.PE;
import me.coley.recaf.ui.util.Pair;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;
import sun.net.www.content.text.Generic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * A panel that displays information about an image's PE header.
 *
 * @author Wolfie / win32kbase
 */

public class PEExplorerPanel extends SplitPane {
    private static final Logger logger = Logging.get(PEExplorerPanel.class);
    private PE pe;

    // You can't have two roots in a TreeView, so you have to create an invisible parent item
    private final TreeItem dummyRoot = new TreeItem();
    private final TreeView primaryTreeView = new TreeView();
    private final TableView primaryTableView = new TableView();

    private final TreeItem DOSHeaderTree = new TreeItem("DOS header");

    private final TreeItem NTHeadersTree = new TreeItem("NT headers");
    private final TreeItem FileHeaderTree = new TreeItem("File header");
    private final TreeItem OptionalHeaderTree = new TreeItem("Optional header");

    final List<Pair<Integer, String>> Characteristics = Arrays.asList(
            new Pair<>(0x0001, "IMAGE_FILE_RELOCS_STRIPPED"),
            new Pair<>(0x0002, "IMAGE_FILE_EXECUTABLE_IMAGE"),
            new Pair<>(0x0004, "IMAGE_FILE_LINE_NUMS_STRIPPED"),
            new Pair<>(0x0008, "IMAGE_FILE_LOCAL_SYMS_STRIPPED"),
            new Pair<>(0x0010, "IMAGE_FILE_AGGRESSIVE_WS_TRIM"),
            new Pair<>(0x0020, "IMAGE_FILE_LARGE_ADDRESS_AWARE"),
            new Pair<>(0x0040, "IMAGE_FILE_RESERVED"),
            new Pair<>(0x0080, "IMAGE_FILE_BYTES_REVERSED_LO"),
            new Pair<>(0x0100, "IMAGE_FILE_32BIT_MACHINE"),
            new Pair<>(0x0200, "IMAGE_FILE_DEBUG_STRIPPED"),
            new Pair<>(0x0400, "IMAGE_FILE_REMOVABLE_RUN_FROM_SWAP"),
            new Pair<>(0x0800, "IMAGE_FILE_NET_RUN_FROM_SWAP"),
            new Pair<>(0x1000, "IMAGE_FILE_SYSTEM"),
            new Pair<>(0x2000, "IMAGE_FILE_DLL"),
            new Pair<>(0x4000, "IMAGE_FILE_UP_SYSTEM_ONLY"),
            new Pair<>(0x8000, "IMAGE_FILE_BYTES_REVERSED_HI")
    );

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

        getItems().addAll(primaryTreeView, primaryTableView);
        setDividerPositions(0.3);
    }

    /**
     * Sets up the primary tree.
     */
    void SetupPrimaryTree() {
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
        NTHeadersTree.getChildren().addAll(FileHeaderTree, OptionalHeaderTree);
        primaryTableView.setSortPolicy((Callback<TableView, Boolean>) param -> false);
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
        primaryTableView.getItems().clear();

        if (primaryTableView.getColumns().isEmpty()) {
            TableColumn<TableGeneric, String> member = new TableColumn("Member");
            member.setCellValueFactory(new PropertyValueFactory<>("member"));
            TableColumn<TableGeneric, String> value = new TableColumn("Value");
            value.setCellValueFactory(new PropertyValueFactory<>("value"));
            TableColumn<TableGeneric, String> meaning = new TableColumn("Meaning");
            meaning.setCellValueFactory(new PropertyValueFactory<>("meaning"));
            primaryTableView.getColumns().addAll(member, value, meaning);
        }

        logger.debug("Selected item: {}", selectedItem.getValue());

        if (selectedItem == DOSHeaderTree) {
            PopulateDOSHeader();
        }
        else if (selectedItem == FileHeaderTree) {
            PopulateFileHeader();
        }
        else if (selectedItem == OptionalHeaderTree) {
            PopulateOptionalHeader();
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
        int[] rw = dos.getReserved();
        int[] rw2 = dos.getReserved2();

        primaryTableView.getItems().addAll(
                new TableWord("e_magic", dos.getMagic(), "Magic number"),
                new TableWord("e_cblp", dos.getUsedBytesInLastPage(), "Bytes on last page of file"),
                new TableWord("e_cp", dos.getFileSizeInPages(), "Pages in file"),
                new TableWord("e_crlc", dos.getNumRelocationItems(), "Relocation count"),
                new TableWord("e_cparhdr", dos.getHeaderSizeInParagraphs(), "Size of header in paragraphs"),
                new TableWord("e_minalloc", dos.getMinExtraParagraphs(), "Minimum extra paragraphs needed"),
                new TableWord("e_maxalloc", dos.getMaxExtraParagraphs(), "Maximum extra paragraphs needed"),
                new TableWord("e_ss", dos.getInitialSS(), "Initial relative SS value"),
                new TableWord("e_sp", dos.getInitialSP(), "Initial SP value"),
                new TableWord("e_csum", dos.getChecksum(), "Checksum"),
                new TableWord("e_ip", dos.getInitialIP(), "Initial IP value"),
                new TableWord("e_cs", dos.getInitialRelativeCS(), "Initial relative CS value"),
                new TableWord("e_lfalc", dos.getAddressOfRelocationTable(), "File address of relocation table"),
                new TableWord("e_ovno", dos.getOverlayNumber(), "Overlay number"),
                new TableWord("e_res", String.format("%d, %d, %d, %d", rw[0], rw[1], rw[2], rw[3]), "Reserved words (4)"),
                new TableWord("e_oemid", dos.getOemId(), "OEM identifier"),
                new TableWord("e_oeminfo", dos.getOemInfo(), "OEM information"),
                new TableWord("e_res2", String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d", rw2[0], rw2[1], rw2[2], rw2[3], rw2[4], rw2[5], rw2[6], rw2[7], rw2[8], rw2[9]), "Reserved words (10)"),
                new TableWord("e_lfanew", dos.getAddressOfNewExeHeader(), "File address of new exe header")
        );
    }

    String GetMachineType(int machine) {
        switch (machine) {
            case 0x0    : return "Any machine type";
            case 0x1d3  : return "Matsushita AM33";
            case 0x8664 : return "x64";
            case 0x1c0  : return "ARM little endian";
            case 0xaa64 : return "ARM64 little endian";
            case 0x1c4  : return "ARM Thumb-2 little endian";
            case 0xebc  : return "EFI byte code";
            case 0x14c  : return "Intel 386 or later processors and compatible processors";
            case 0x200  : return "Intel Itanium processor family";
            case 0x9041 : return "Mitsubishi M32R little endian";
            case 0x266  : return "MIPS16";
            case 0x366  : return "MIPS with FPU";
            case 0x466  : return "MIPS16 with FPU";
            case 0x1f0  : return "Power PC little endian";
            case 0x1f1  : return "Power PC with floating point support";
            case 0x166  : return "MIPS little endian";
            case 0x5032 : return "RISC-V 32-bit address space";
            case 0x5064 : return "RISC-V 64-bit address space";
            case 0x5128 : return "RISC-V 128-bit address space";
            case 0x1a2  : return "Hitachi SH3";
            case 0x1a3  : return "Hitachi SH3 DSP";
            case 0x1a6  : return "Hitachi SH4";
            case 0x1a8  : return "Hitachi SH5";
            case 0x1c2  : return "Thumb";
            case 0x169  : return "MIPS little-endian WCE v2";
            default     : return "Unknown";
        }
    }

    List<Pair<Integer, String>> GetCharacteristics(int characteristics) {
        List<Pair<Integer, String>> out = new ArrayList<>();

        Characteristics.forEach((characteristic) -> {
            if ((characteristics & characteristic.getLeft()) > 0) {
                out.add(characteristic);
            }
        });

        return out;
    }

    void PopulateFileHeader() {
        COFFHeader fileHeader = pe.getCoffHeader();

        primaryTableView.getItems().addAll(
                new TableWord("Machine", fileHeader.getMachine(), GetMachineType(fileHeader.getMachine())),
                new TableWord("NumberOfSections", fileHeader.getNumberOfSections(), "Number of sections"),
                new TableDword("TimeDateStamp", fileHeader.getTimeDateStamp(), Instant.ofEpochSecond(fileHeader.getTimeDateStamp()).toString()),
                new TableDword("PointerToSymbolTable", fileHeader.getPointerToSymbolTable(), "Pointer to symbol table"),
                new TableDword("NumberOfSymbols", fileHeader.getNumberOfSymbols(), "Number of symbols"),
                new TableWord("SizeOfOptionalHeader", fileHeader.getSizeOfOptionalHeader(), "Size of optional header"),
                new TableWord("Characteristics", fileHeader.getCharacteristics(), "PE characteristics")
        );

        List<Pair<Integer, String>> characteristics = GetCharacteristics(fileHeader.getCharacteristics());
        characteristics.forEach((characteristic) -> {
            primaryTableView.getItems().add(new TableWord("", characteristic.getLeft(), characteristic.getRight()));
        });
    }

    void PopulateOptionalHeader() {
        OptionalHeader optionalHeader = pe.getOptionalHeader();

        primaryTableView.getItems().addAll(
                new TableWord("Magic", optionalHeader.getMagic(), "Magic number"),
                new TableByte("MajorLinkerVersion", optionalHeader.getMajorLinkerVersion(), "Major linker version"),
                new TableByte("MinorLinkerVersion", optionalHeader.getMinorLinkerVersion(), "Minor linker version"),
                new TableDword("SizeOfCode", optionalHeader.getSizeOfCode(), "Size of code"),
                new TableDword("SizeOfInitializedData", optionalHeader.getSizeOfInitializedData(), "Size of initialized data"),
                new TableDword("SizeOfUninitializedData", optionalHeader.getSizeOfUninitializedData(), "Size of uninitialized data"),
                new TableDword("AddressOfEntryPoint", optionalHeader.getAddressOfEntryPoint(), "Address of entry point (.unkn)")
                //new TableDword("BaseOfCode")


        );
    }
}
