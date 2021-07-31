package me.coley.recaf.ui.panel.pe;

public class TableDword extends TableGeneric {

    public TableDword(String member, String value, String meaning) {
        super(member, value, meaning);
    }

    public TableDword(String member, String value) {
        super(member, value, "");
    }

    public TableDword(String member, int value, String meaning) {
        this(member, "", meaning);
        this.setValue(Integer.toHexString(value));
    }

    public TableDword(String member, int value) {
        this(member, (short)value, "");
    }

}
