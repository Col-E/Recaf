package software.coley.recaf.info.member;

import java.util.Objects;

/**
 * Basic implementation of a field member.
 *
 * @author Matt Coley
 */
public class BasicFieldMember extends BasicMember implements FieldMember {
	private final Object defaultValue;

	/**
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param signature
	 * 		Field generic signature. May be {@code null}.
	 * @param access
	 * 		Field access modifiers.
	 * @param defaultValue
	 * 		Default value. May be {@code null}.
	 */
	public BasicFieldMember(String name, String desc, String signature, int access, Object defaultValue) {
		super(name, desc, signature, access);
		this.defaultValue = defaultValue;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String toString() {
		return "Field: " + getDescriptor() + " " + getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !FieldMember.class.isAssignableFrom(o.getClass())) return false;

		FieldMember field = (FieldMember) o;

		if (!getName().equals(field.getName())) return false;
		if (!Objects.equals(getSignature(), field.getSignature())) return false;
		if (!Objects.equals(getDefaultValue(), field.getDefaultValue())) return false;
		return getDescriptor().equals(field.getDescriptor());
	}

	@Override
	public int hashCode() {
		int result = getName().hashCode();
		result = 31 * result + getDescriptor().hashCode();
		if (getSignature() != null) result = 31 * result + getSignature().hashCode();
		if (getDefaultValue() != null) result = 31 * result + getDefaultValue().hashCode();
		return result;
	}
}
