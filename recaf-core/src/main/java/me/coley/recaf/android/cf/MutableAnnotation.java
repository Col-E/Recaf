package me.coley.recaf.android.cf;

import org.jf.dexlib2.base.BaseAnnotation;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link Annotation}.
 *
 * @author Matt Coley
 */
public class MutableAnnotation extends BaseAnnotation {
	private String type;
	private Set<MutableAnnotationElement> elements;
	private int visibility;

	/**
	 * @param original
	 * 		Annotation to copy.
	 */
	public MutableAnnotation(Annotation original) {
		this(original.getType(), copyElements(original.getElements()), original.getVisibility());
	}

	/**
	 * @param type
	 * 		Annotation's type.
	 * @param elements
	 * 		Set of name/value pairs.
	 * @param visibility
	 * 		Annotation visibility. See {@link org.jf.dexlib2.AnnotationVisibility}
	 */
	public MutableAnnotation(String type, Set<MutableAnnotationElement> elements, int visibility) {
		this.type = type;
		this.elements = elements;
		this.visibility = visibility;
	}

	@Override
	public int getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility
	 * 		New visibility flags for the annotation.
	 */
	public void setVisibility(int visibility) {
		this.visibility = visibility;
	}

	@Nonnull
	@Override
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * 		New type definition of the annotation.
	 */
	public void setType(@Nonnull String type) {
		this.type = type;
	}

	@Nonnull
	@Override
	public Set<MutableAnnotationElement> getElements() {
		return elements;
	}

	/**
	 * @param elements
	 * 		New name/value pairs of the annotation.
	 */
	public void setElements(@Nonnull Set<MutableAnnotationElement> elements) {
		this.elements = elements;
	}

	/**
	 * @param elements
	 * 		Original element set.
	 *
	 * @return Set of mutable copies.
	 */
	public static Set<MutableAnnotationElement> copyElements(Set<? extends AnnotationElement> elements) {
		Set<MutableAnnotationElement> set = new HashSet<>();
		for (AnnotationElement element : elements) {
			set.add(new MutableAnnotationElement(element));
		}
		return set;
	}

	/**
	 * @param annotations
	 * 		Original annotation set.
	 *
	 * @return Set of mutable copies.
	 */
	public static Set<MutableAnnotation> copyAnnotations(Set<? extends Annotation> annotations) {
		Set<MutableAnnotation> set = new HashSet<>();
		for (Annotation annotation : annotations) {
			set.add(new MutableAnnotation(annotation));
		}
		return set;
	}
}
