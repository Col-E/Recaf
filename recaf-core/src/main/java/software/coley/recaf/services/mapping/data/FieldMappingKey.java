package software.coley.recaf.services.mapping.data;

import java.util.Objects;

/**
 * Mapping key for fields.
 *
 * @author xDark
 */
public class FieldMappingKey extends AbstractMappingKey {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param owner
	 * 		Class name.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 */
	public FieldMappingKey(String owner, String name, String desc) {
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
	 * @return Field name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Field descriptor.
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
		if (!(o instanceof FieldMappingKey)) return false;

		FieldMappingKey that = (FieldMappingKey) o;

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
