package me.coley.recaf.ui.controls.text.selection;

/**
 * Wrapper for selected fields/methods.
 *
 * @author Matt
 */
public class MemberSelection {
	public final String owner;
	public final String name;
	public final String desc;
	public final boolean dec;

	/**
	 * @param owner
	 * 		Internal name of defining class.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 * @param dec
	 * 		Is the member declared or a reference.
	 */
	public MemberSelection(String owner, String name, String desc, boolean dec) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.dec = dec;
	}

	/**
	 * @return {@code true} of the {@link #desc} is that of a method.
	 */
	public boolean method() {
		return desc.contains("(");
	}
}