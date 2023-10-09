package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.martinez.pe.PeImage;
import me.martinez.pe.headers.ImageDosHeader;

/**
 * Table display for DOS headers.
 *
 * @author Wolfie / win32kbase
 */
public class DosTableDisplayMode implements TableDisplayMode<PeImage> {
	@Override
	public void apply(PeImage pe, SizedDataTypeTable table) {
		ImageDosHeader dos = pe.dosHeader;
		int[] rw = dos.res;
		int[] rw2 = dos.res2;
		table.addWord("e_magic", dos.magic, "Magic number");
		table.addWord("e_cblp", dos.cblp, "Bytes on last page of file");
		table.addWord("e_cp", dos.cp, "Pages in file");
		table.addWord("e_crlc", dos.crlc, "Relocation count");
		table.addWord("e_cparhdr", dos.cparhdr, "Size of header in paragraphs");
		table.addWord("e_minalloc", dos.minalloc, "Minimum extra paragraphs needed");
		table.addWord("e_maxalloc", dos.maxalloc, "Maximum extra paragraphs needed");
		table.addWord("e_ss", dos.ss, "Initial relative SS value");
		table.addWord("e_sp", dos.sp, "Initial SP value");
		table.addWord("e_csum", dos.csum, "Checksum");
		table.addWord("e_ip", dos.ip, "Initial IP value");
		table.addWord("e_cs", dos.cs, "Initial relative CS value");
		table.addWord("e_lfalc", dos.lfarlc, "File address of relocation table");
		table.addWord("e_ovno", dos.ovno, "Overlay number");
		table.addWord("e_res", String.format("%d, %d, %d, %d", rw[0], rw[1], rw[2], rw[3]), "Reserved words (4)");
		table.addWord("e_oemid", dos.oemid, "OEM identifier");
		table.addWord("e_oeminfo", dos.oeminfo, "OEM information");
		table.addWord("e_res2", String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d", rw2[0], rw2[1], rw2[2], rw2[3], rw2[4], rw2[5], rw2[6], rw2[7], rw2[8], rw2[9]), "Reserved words (10)");
		table.addWord("e_lfanew", (int) dos.lfanew, "File address of new exe header");
	}
}
