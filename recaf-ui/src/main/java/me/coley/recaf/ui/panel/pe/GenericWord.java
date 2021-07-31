package me.coley.recaf.ui.panel.pe;

import me.coley.recaf.ui.util.HexUtils;

public class GenericWord {
    private String member = null;
    private String value = null;
    private String meaning = null;

    public GenericWord() {
    }

    public GenericWord(String member, int value) { this(member, value, "", true); }
    public GenericWord(String member, int value, boolean hex) { this(member, value, "", hex); }
    public GenericWord(String member, int value, String meaning) {
        this(member, value, meaning,true);
    }

    public GenericWord(String member, int value, String meaning, boolean hex) {
        this.member = member;
        this.value = hex ? HexUtils.ToHex(value) : Integer.toString(value);
        this.meaning = meaning;
    }

    public GenericWord(String member, String value, String meaning) {
        this.member = member;
        this.value = value;
        this.meaning = meaning;
    }

    public GenericWord(String member, String value) {
        this.member = member;
        this.value = value;
    }

    public String getMember() {
        return this.member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMeaning() { return this.meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
}