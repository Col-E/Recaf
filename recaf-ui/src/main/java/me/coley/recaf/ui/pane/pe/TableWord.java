package me.coley.recaf.ui.panel.pe;

public class TableWord extends TableGeneric {

    public TableWord(String member, String value, String meaning) {
        super(member, value, meaning);
    }

    public TableWord(String member, String value) {
        super(member, value, "");
    }

    public TableWord(String member, int value, String meaning) {
        this(member, "", meaning);

        byte byte1 = (byte)value;
        byte byte2 = (byte)(value >> 8);
        this.setValue(String.format("%02X%02X", byte2, byte1));
    }

    public TableWord(String member, int value) {
        this(member, value, "");
    }

}
