package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

/**
 * Basic implementation of an annotation array reference in an element.
 *
 * @author Matt Coley
 */
public class BasicAnnotationEnumReference implements AnnotationEnumReference {
	private final String descriptor;
	private final String value;

	/**
	 * @param descriptor
	 * 		Enum descriptor.
	 * @param value
	 * 		Enum name.
	 */
	public BasicAnnotationEnumReference(String descriptor, String value) {
		this.descriptor = descriptor;
		this.value = value;
	}

	@Nonnull
	@Override
	public String getDescriptor() {
		return descriptor;
	}

	@Nonnull
	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicAnnotationEnumReference enumValue = (BasicAnnotationEnumReference) o;

		if (!descriptor.equals(enumValue.descriptor)) return false;
		return value.equals(enumValue.value);
	}

	@Override
	public int hashCode() {
		int result = descriptor.hashCode();
		result = 31 * result + value.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return descriptor + ":" + value;
	}
}
