package software.coley.recaf.services.mapping.data;

import java.util.Objects;

/**
 * Mapping key for methods.
 *
 * @author xDark
 */
public class MethodMappingKey extends AbstractMappingKey {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param owner
	 * 		Class name.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	public MethodMappingKey(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Class owner.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Method name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Method descriptor.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	protected String toText() {
		String owner = this.owner;
		String name = this.name;
		String desc = this.desc;
		if (desc == null) {
			return owner + '\t' + name;
		}
		return owner + '\t' + name + '\t' + desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MethodMappingKey)) return false;

		MethodMappingKey that = (MethodMappingKey) o;

		return owner.equals(that.owner) && name.equals(that.name) && Objects.equals(desc, that.desc);
	}

	@Override
	public int hashCode() {
		int result = owner.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + Objects.hashCode(desc);
		return result;
	}
}
