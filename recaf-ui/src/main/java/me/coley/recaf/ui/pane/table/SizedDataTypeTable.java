package me.coley.recaf.ui.pane.table;

import javafx.scene.control.TableView;
import me.martinez.pe.ImagePeHeaders;
import net.fornwall.jelf.ElfFile;

/**
 * A {@link TableGeneric} oriented {@link TableView} with handy utility calls.
 *
 * @author Matt Coley
 */
public class SizedDataTypeTable extends TableView<TableGeneric> {
	public void addByte(String memberName, int value, String meaning) {
		getItems().add(new TableByte(memberName, value, meaning));
	}

	public void addByte(String memberName, String value, String meaning) {
		getItems().add(new TableByte(memberName, value, meaning));
	}

	public void addWord(String memberName, int value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	public void addWord(String memberName, String value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	public void addDword(String memberName, int value, String meaning) {
		getItems().add(new TableDword(memberName, value, meaning));
	}

	public void addDword(String memberName, String value, String meaning) {
		getItems().add(new TableDword(memberName, value, meaning));
	}

	public void addQword(String memberName, long value, String meaning) {
		getItems().add(new TableQword(memberName, value, meaning));
	}

	public void addQword(String memberName, String value, String meaning) {
		getItems().add(new TableQword(memberName, value, meaning));
	}

	public void addAddress(String memberName, long value, String meaning, ImagePeHeaders pe) {
		if (pe.is64bit()) {
			getItems().add(new TableQword(memberName, value, meaning));
		} else {
			getItems().add(new TableDword(memberName, (int) value, meaning));
		}
	}

	public void addAddress(String memberName, long value, String meaning, ElfFile elf) {
		// ei_class 1 means 32-bit
		if (elf.objectSize == 1) {
			getItems().add(new TableDword(memberName, (int) value, meaning));
		} else {
			getItems().add(new TableQword(memberName, value, meaning));
		}
	}
}
