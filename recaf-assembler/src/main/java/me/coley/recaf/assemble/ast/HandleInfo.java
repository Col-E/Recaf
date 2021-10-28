package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.util.OpcodeUtil;

/**
 * Handle to a member reference.
 *
 * @author Matt Coley
 */
public class HandleInfo extends BaseElement {
	private final int tagVal;
	private final String tag;
	private final String owner;
	private final String name;
	private final String desc;
	private final boolean isMethod;

	/**
	 * @param tag
	 * 		Handle tag name.
	 * @param owner
	 * 		Reference member owner.
	 * @param name
	 * 		Reference member name.
	 * @param desc
	 * 		Reference member descriptor.
	 */
	public HandleInfo(String tag, String owner, String name, String desc) {
		this.tagVal = OpcodeUtil.nameToTag(tag);
		this.tag = tag;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.isMethod = desc.charAt(0) == '(';
	}

	/**
	 * @return Handle tag value.
	 */
	public int getTagVal() {
		return tagVal;
	}

	/**
	 * @return Handle tag name.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @return Reference member owner.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Reference member name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Reference member descriptor.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	public String print() {
		if (isMethod) {
			return String.format("%s %s.%s%s", tag, owner, name, desc);
		} else {
			return String.format("%s %s.%s %s", tag, owner, name, desc);
		}
	}
}
