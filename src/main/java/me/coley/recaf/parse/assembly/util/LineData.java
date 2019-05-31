package me.coley.recaf.parse.assembly.util;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.bytecode.OpcodeUtil;

/**
 * Wrapper for opcode information of a line of text.
 *
 * @author Matt
 */
public class LineData {
	private static final Pattern P_OPCODE = new Pattern("^\\w+(?=\\s*)");
	public final String optext;
	public final int opcode;
	public final int type;

	private LineData(String optext, int opcode, int type) {
		this.optext = optext;
		this.opcode = opcode;
		this.type = type;
	}

	public static LineData from(String lineText) {
		Matcher m = P_OPCODE.matcher(lineText);
		m.find();
		String opMatch = m.group(0);
		// Line must be empty
		if(opMatch == null)
			return null;
		String opText = opMatch.toUpperCase();
		// Get opcode / opcode-type
		int opcode;
		try {
			opcode = OpcodeUtil.nameToOpcode(opText);
		} catch(Exception e) {
			throw new IllegalStateException("Unknown opcode: " + opText);
		}
		int optype;
		try {
			optype = OpcodeUtil.opcodeToType(opcode);
		} catch(Exception e) {
			throw new IllegalStateException("Unknown group for opcode: " + opText);
		}
		return new LineData(opText, opcode, optype);
	}
}