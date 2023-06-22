package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic implementation of an annotation array reference in an element.
 *
 * @author Matt Coley
 */
public class BasicAnnotationArrayReference implements AnnotationArrayReference {
	private final List<Object> values;

	/**
	 * @param values
	 * 		Array values.
	 */
	public BasicAnnotationArrayReference(List<Object> values) {
		this.values = values;
	}

	@Nonnull
	public List<Object> getValues() {
		return values;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicAnnotationArrayReference that = (BasicAnnotationArrayReference) o;

		return values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}

	@Override
	public String toString() {
		return "[" + values.stream()
				.map(Object::toString)
				.collect(Collectors.joining(", ")) + "]";
	}
}
