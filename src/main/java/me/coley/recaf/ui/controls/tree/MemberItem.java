package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent members <i>(field or method)</i>.
 *
 * @author Matt
 */
public class MemberItem extends DirectoryItem {
	private final String name;
	private final String desc;
	private final int access;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Local name in tree.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 * @param access
	 * 		Member access modifiers.
	 */
	public MemberItem(JavaResource resource, String local, String name, String desc, int access) {
		super(resource, local);
		this.name = name;
		this.desc = desc;
		this.access = access;
	}

	/**
	 * @return Contained member name.
	 */
	public String getMemberName() {
		return name;
	}

	/**
	 * @return Contained member descriptor.
	 */
	public String getMemberDesc() {
		return desc;
	}

	/**
	 * @return Contained member access modifiers.
	 */
	public int getMemberAccess() {
		return access;
	}

	/**
	 * @return {@code true} if the member represents a method.
	 */
	public boolean isMethod() {
		return desc.indexOf('(') == 0;
	}

	/**
	 * @return {@code true} if the member represents a field.
	 */
	public boolean isField() {
		return !isMethod();
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof MemberItem) {
			MemberItem c = (MemberItem) o;
			if (isField() && c.isMethod())
				return -1;
			else if (isMethod() && c.isField())
				return 1;
			return getLocalName().compareTo(c.getLocalName());
		}
		return 1;
	}
}