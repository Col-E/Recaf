package software.coley.recaf.test.dummy;

/**
 * Dummy class to test existence of fields with varing degrees of access.
 */
@SuppressWarnings("all")
public class AccessibleFields {
	public static final int CONSTANT_FIELD = 16;
	private final int privateFinalField = 8;
	protected final int protectedField = 4;
	public final int publicField = 2;
	final int packageField = 1;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AccessibleFields other = (AccessibleFields) o;

		if (privateFinalField != other.privateFinalField) return false;
		if (protectedField != other.protectedField) return false;
		if (publicField != other.publicField) return false;
		if (packageField != other.packageField) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = privateFinalField;
		result = 31 * result + protectedField;
		result = 31 * result + publicField;
		result = 31 * result + packageField;
		return result;
	}
}
