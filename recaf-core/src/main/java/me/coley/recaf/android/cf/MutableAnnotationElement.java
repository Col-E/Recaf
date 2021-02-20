package me.coley.recaf.android.cf;

import org.jf.dexlib2.base.BaseAnnotationElement;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;

/**
 * Mutable implementation of {@link org.jf.dexlib2.iface.Annotation}.
 *
 * @author Matt Coley
 */
public class MutableAnnotationElement extends BaseAnnotationElement {
	private String name;
	private EncodedValue value;

	/**
	 * @param original
	 * 		Instance to copy.
	 */
	public MutableAnnotationElement(AnnotationElement original) {
		this(original.getName(), original.getValue());
	}

	/**
	 * @param name
	 * 		Name of element.
	 * @param value
	 * 		Const value of element.
	 */
	public MutableAnnotationElement(String name, EncodedValue value) {
		this.name = name;
		this.value = value;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * 		New element name.
	 */
	public void setName(@Nonnull String name) {
		this.name = name;
	}

	@Nonnull
	@Override
	public EncodedValue getValue() {
		return value;
	}

	/**
	 * @param value
	 * 		New const value.
	 */
	public void setValue(@Nonnull EncodedValue value) {
		this.value = value;
	}
}
