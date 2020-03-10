package me.coley.recaf.ui.controls.text.selection;

/**
 * Wrapper for selected classes.
 *
 * @author Matt
 */
public class ClassSelection {
	public final String name;
	public final boolean dec;

	/**
	 * @param name
	 * 		Internal class name.
	 * @param dec
	 * 		Is the name given as a declaration or reference.
	 */
	public ClassSelection(String name, boolean dec) {
		this.name = name;
		this.dec = dec;
	}
}