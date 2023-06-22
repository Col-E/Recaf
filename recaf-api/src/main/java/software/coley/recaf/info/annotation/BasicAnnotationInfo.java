package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.TypePath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic implementation of annotation info.
 *
 * @author Matt Coley
 */
public class BasicAnnotationInfo implements AnnotationInfo {
	private final Map<String, AnnotationElement> elements = new LinkedHashMap<>(); // preserve order on iter
	private final boolean visible;
	private final String descriptor;

	/**
	 * @param visible
	 * 		Annotation runtime visibility.
	 * @param descriptor
	 * 		Annotation descriptor.
	 */
	public BasicAnnotationInfo(boolean visible, @Nonnull String descriptor) {
		this.visible = visible;
		this.descriptor = descriptor;
	}

	/**
	 * For internal use when populating the model.
	 * Adding an element here does not change the bytecode of the class.
	 *
	 * @param element
	 * 		Element to add.
	 */
	public void addElement(@Nonnull AnnotationElement element) {
		elements.put(element.getElementName(), element);
	}

	/**
	 * @param typeRef
	 * 		Constant denoting where the annotation is applied.
	 * @param typePath
	 * 		Path to a type argument.
	 *
	 * @return Type annotation from this annotation.
	 */
	public BasicTypeAnnotationInfo withTypeInfo(int typeRef, @Nullable TypePath typePath) {
		return new BasicTypeAnnotationInfo(typeRef, typePath, isVisible(), getDescriptor());
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Nonnull
	@Override
	public String getDescriptor() {
		return descriptor;
	}

	@Nonnull
	@Override
	public Map<String, AnnotationElement> getElements() {
		return elements;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicAnnotationInfo annotation = (BasicAnnotationInfo) o;

		if (visible != annotation.visible) return false;
		if (!elements.equals(annotation.elements)) return false;
		return descriptor.equals(annotation.descriptor);
	}

	@Override
	public int hashCode() {
		int result = elements.hashCode();
		result = 31 * result + (visible ? 1 : 0);
		result = 31 * result + descriptor.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "BasicAnnotationInfo{" +
				" visible=" + visible +
				", descriptor='" + descriptor + '\'' +
				", elements=" + elements +
				'}';
	}
}
