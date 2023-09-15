package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Basic implementation of annotation elements.
 *
 * @author Matt Coley
 */
public class BasicAnnotationElement implements AnnotationElement {
	private final String name;
	private final Object value;

	/**
	 * @param name
	 * 		Element name.
	 * @param value
	 * 		Element value.
	 */
	public BasicAnnotationElement(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Nonnull
	@Override
	public String getElementName() {
		return name;
	}

	@Nonnull
	@Override
	public Object getElementValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicAnnotationElement element = (BasicAnnotationElement) o;

		if (!name.equals(element.name)) return false;
		return Objects.equals(value, element.value);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "BasicAnnotationElement{" +
				"name='" + name + '\'' +
				", value=" + value +
				'}';
	}

}
