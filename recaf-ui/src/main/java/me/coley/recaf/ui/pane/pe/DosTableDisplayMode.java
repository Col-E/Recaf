package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.DOSHeader;
import com.kichik.pecoff4j.PE;

/**
 * Table display for DOS headers.
 *
 * @author Wolfie / win32kbase
 */
public class DosTableDisplayMode implements TableDisplayMode {
	@Override
	public void apply(PE pe, SizedDataTypeTable table) {
		DOSHeader dos = pe.getDosHeader();
		int[] rw = dos.getReserved();
		int[] rw2 = dos.getReserved2();
		table.addWord("e_magic", dos.getMagic(), "Magic number");
		table.addWord("e_cblp", dos.getUsedBytesInLastPage(), "Bytes on last page of file");
		table.addWord("e_cp", dos.getFileSizeInPages(), "Pages in file");
		table.addWord("e_crlc", dos.getNumRelocationItems(), "Relocation count");
		table.addWord("e_cparhdr", dos.getHeaderSizeInParagraphs(), "Size of header in paragraphs");
		table.addWord("e_minalloc", dos.getMinExtraParagraphs(), "Minimum extra paragraphs needed");
		table.addWord("e_maxalloc", dos.getMaxExtraParagraphs(), "Maximum extra paragraphs needed");
		table.addWord("e_ss", dos.getInitialSS(), "Initial relative SS value");
		table.addWord("e_sp", dos.getInitialSP(), "Initial SP value");
		table.addWord("e_csum", dos.getChecksum(), "Checksum");
		table.addWord("e_ip", dos.getInitialIP(), "Initial IP value");
		table.addWord("e_cs", dos.getInitialRelativeCS(), "Initial relative CS value");
		table.addWord("e_lfalc", dos.getAddressOfRelocationTable(), "File address of relocation table");
		table.addWord("e_ovno", dos.getOverlayNumber(), "Overlay number");
		table.addWord("e_res", String.format("%d, %d, %d, %d", rw[0], rw[1], rw[2], rw[3]), "Reserved words (4)");
		table.addWord("e_oemid", dos.getOemId(), "OEM identifier");
		table.addWord("e_oeminfo", dos.getOemInfo(), "OEM information");
		table.addWord("e_res2", String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d", rw2[0], rw2[1], rw2[2], rw2[3], rw2[4], rw2[5], rw2[6], rw2[7], rw2[8], rw2[9]), "Reserved words (10)");
		table.addWord("e_lfanew", dos.getAddressOfNewExeHeader(), "File address of new exe header");
	}
}
