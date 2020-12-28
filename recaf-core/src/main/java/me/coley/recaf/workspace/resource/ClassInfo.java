package me.coley.recaf.workspace.resource;

/**
 * Class info for resource.
 *
 * @author Matt Coley
 */
public class ClassInfo extends ItemInfo {
	/**
	 * @param name
	 * 		Internal class name.
	 * @param value
	 * 		Class bytecode.
	 */
	public ClassInfo(String name, byte[] value) {
		super(name, value);
	}
}
