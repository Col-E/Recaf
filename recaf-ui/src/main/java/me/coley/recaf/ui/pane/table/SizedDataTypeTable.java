package me.coley.recaf.ui.pane.table;

import javafx.scene.control.TableView;
import me.martinez.pe.PeImage;
import net.fornwall.jelf.ElfFile;

/**
 * A {@link TableGeneric} oriented {@link TableView} with handy utility calls.
 *
 * @author Matt Coley
 */
public class SizedDataTypeTable extends TableView<TableGeneric> {
	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addByte(String memberName, int value, String meaning) {
		getItems().add(new TableByte(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addByte(String memberName, String value, String meaning) {
		getItems().add(new TableByte(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addWord(String memberName, int value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addWord(String memberName, String value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addDword(String memberName, int value, String meaning) {
		getItems().add(new TableDword(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addDword(String memberName, String value, String meaning) {
		getItems().add(new TableDword(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addQword(String memberName, long value, String meaning) {
		getItems().add(new TableQword(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 */
	public void addQword(String memberName, String value, String meaning) {
		getItems().add(new TableQword(memberName, value, meaning));
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 * @param pe
	 * 		PE image to pull 64-bit flag from.
	 */
	public void addAddress(String memberName, long value, String meaning, PeImage pe) {
		if (pe.is64bit()) {
			getItems().add(new TableQword(memberName, value, meaning));
		} else {
			getItems().add(new TableDword(memberName, (int) value, meaning));
		}
	}

	/**
	 * Adds a table entry.
	 *
	 * @param memberName
	 * 		Entry member name.
	 * @param value
	 * 		Entry value.
	 * @param meaning
	 * 		Entry description.
	 * @param elf
	 * 		ELF image to pull 64-bit flag from.
	 */
	public void addAddress(String memberName, long value, String meaning, ElfFile elf) {
		// ei_class 1 means 32-bit
		if (elf.ei_class == 1) {
			getItems().add(new TableDword(memberName, (int) value, meaning));
		} else {
			getItems().add(new TableQword(memberName, value, meaning));
		}
	}
}
