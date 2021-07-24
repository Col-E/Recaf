package me.coley.recaf.search.result;

/**
 * Result containing some matched member reference.
 *
 * @author Matt Coley
 */
public class ReferenceResult extends Result {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param builder
	 * 		Builder containing information about the result.
	 * @param owner
	 * 		The class defining the referenced member.
	 * @param name
	 * 		The name of the referenced member.
	 * @param desc
	 * 		The type descriptor of the referenced member.
	 */
	public ReferenceResult(ResultBuilder builder, String owner, String name, String desc) {
		super(builder);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Class defining the referenced member.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Name of the referenced member.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Type descriptor of the referenced member.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	protected Object getValue() {
		if (desc.charAt(0) == '(') {
			return owner + "." + name + desc;
		} else {
			return owner + "." + name + " " + desc;
		}
	}
}
