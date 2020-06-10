package me.coley.recaf.ui.controls.text.selection;

import java.util.Map;

/**
 * Wrapper for selected switch instruction.
 *
 * @author Matt
 */
public class SwitchSelection {
	public final Map<String, String> mappings;
	public final String dflt;

	/**
	 * @param mappings
	 * 		Map of destinations and their key values.
	 * @param dflt
	 * 		Default destination.
	 */
	public SwitchSelection(Map<String, String> mappings, String dflt) {
		this.mappings = mappings;
		this.dflt = dflt;
	}
}