package me.coley.recaf.ui.controls;

import java.util.Map;

/**
 * Common functionality of class editor displays.
 *
 * @author Matt
 */
public interface ClassEditor {
	/**
	 * Compiles the current source code.
	 *
	 * @param name
	 * 		Class name to compile.
	 *
	 * @return Recompiled bytecode of classes <i>(Should there be any inner classes)</i>.
	 */
	Map<String, byte[]> save(String name);

	/**
	 * Select the definition of the given member.
	 *
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	void selectMember(String name, String desc);
}
