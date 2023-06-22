package software.coley.recaf.services.mapping.data;

/**
 * Mapping key for classes.
 *
 * @author xDark
 */
public class ClassMappingKey extends AbstractMappingKey {
	private final String name;

	/**
	 * @param name
	 * 		Class name.
	 */
	public ClassMappingKey(String name) {
		this.name = name;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		return name;
	}

	@Override
	protected String toText() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ClassMappingKey)) return false;

		ClassMappingKey that = (ClassMappingKey) o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
