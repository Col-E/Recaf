package me.coley.recaf.config.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.Config;

public class ConfUI extends Config {
	/**
	 * Show confirmation prompt on doing potentially dangerous things.
	 */
	public boolean confirmDeletions = true;
	/**
	 * Show extra jump information.
	 */
	public boolean opcodeShowJumpHelp = true;
	/**
	 * Simplify descriptor displays on the opcode list.
	 */
	public boolean opcodeSimplifyDescriptors = true;
	/**
	 * Show member windows even if class has none.
	 */
	public boolean showEmptyMemberWindows = true;
	/**
	 * Show attributes that should not be messed with or are uncommonly used
	 * such as:
	 * <ul>
	 * <li>Enclosing method</li>
	 * <li>InnerClasses</li>
	 * <li>SourceDebug</li>
	 * </ul>
	 */
	public boolean showUncommonAttributes = true;
	/**
	 * Display variable's signature in the opcode edit window for variable
	 * opcodes. Allows editing of signatures <i>(Generic types)</i> and
	 * significantly increases the edit window size.
	 */
	public boolean showVariableSignatureInTable;
	//@formatter:off
	/**
	 * Order of menu items in the opcode context menu.
	 */
	public List<String> menuOrderOpcodes = Arrays.asList(
			"window.method.opcode.edit",         // edit opcode
			"window.method.opcode.new.before",   // insert opcode before
			"window.method.opcode.new.after",    // insert opcode after
			"window.method.opcode.move.up",      // move opcode up
			"window.method.opcode.move.down",    // move opcode down
			"window.method.opcode.gotodef",      // goto type/member definition
			"window.method.opcode.gotojump",     // goto jump destination
			"window.method.opcode.saveblock",    // save selected opcodes
			"window.method.opcode.insertblock",  // insert saved opcodes
			"window.method.opcode.remove"        // remove opcode
	);     
	/**
	 * Order of menu items in the member context menu.
	 */
	public List<String> menuOrderMember = Arrays.asList(
			"window.member.decompile",   // decompile method
			"window.member.vars",        // show method variables
			"window.member.editopcodes", // edit method code
			"window.member.catch",       // edit method's catch blocks
			"window.member.verify",      // verify method bytecode
			"window.member.define",      // edit method defintion
			"window.member.search",      // search member references
			"window.member.add",         // add new member
			"window.member.remove"       // remove member
	);
	//@formatter:on
	/**
	 * Default action to run when the opcode list is middle-clicked.
	 */
	public String menuOpcodesDefaultAction = "window.method.opcode.edit";
	/**
	 * Default action to run when the method list is middle-clicked.
	 */
	public String menuMethodDefaultAction = "window.member.editopcodes";
	/**
	 * Default action to run when the field list is middle-clicked.
	 */
	public String menuFieldDefaultAction = "window.member.define";
	/**
	 * Max length for text in ldc opcodes to be displayed.
	 */
	public int ldcMaxLength = 125;
	/**
	 * Language to show GUI in.
	 */
	public String language = "en";
	/**
	 * The look and feel to apply to Recaf on launch.
	 */
	private String lookAndFeel = "javax.swing.plaf.nimbus.NimbusLookAndFeel";

	public ConfUI() {
		super("rcinterface");
	}

	/**
	 * @return The current look and feel.
	 */
	public String getLookAndFeel() {
		return lookAndFeel;
	}

	/**
	 * Set the current look and feel.
	 * 
	 * @param lookAndFeel
	 *            Look and feel to set.
	 */
	public void setLookAndFeel(String lookAndFeel) {
		this.lookAndFeel = lookAndFeel;
		// Refresh the UI if already loaded
		if (Recaf.INSTANCE.ui != null) {
			Recaf.INSTANCE.ui.refreshLAF(lookAndFeel);
		}
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (List.class.isAssignableFrom(type)) {
			List<String> list = new ArrayList<>();
			value.asArray().forEach(v -> list.add(v.asString()));
			System.out.println(Arrays.toString(list.toArray(new String[0])));
			return list;
		} else {
			throw new RuntimeException();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		if (List.class.isAssignableFrom(type)) {
			JsonArray arr = new JsonArray();
			List<String> list = (List<String>) value;
			list.forEach(v -> arr.add(v));
			return arr;
		} else {
			throw new RuntimeException();
		}
	}
	
}