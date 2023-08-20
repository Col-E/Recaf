package me.coley.recaf.assemble.ast;

import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

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
	 * @param handle
	 * 		Handle to pull info from.
	 */
	public HandleInfo(Handle handle) {
		this(OpcodeUtil.tagToName(handle.getTag()), handle.getOwner(), handle.getName(), handle.getDesc());
	}

	/**
	 * @return Handle instance.
	 */
	public Handle toHandle() {
		return new Handle(getTagVal(), getOwner(), getName(), getDesc(), (getTagVal() == Opcodes.H_INVOKEINTERFACE));
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
	public String print(PrintContext context) {
			return tag + " " + context.fmtIdentifier(owner + '.' + name) + ' ' + context.fmtIdentifier(desc);
	}
}
