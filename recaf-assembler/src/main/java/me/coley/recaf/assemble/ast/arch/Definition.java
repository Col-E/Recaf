package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.Descriptor;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.List;

/**
 * Outlines a class <i>({@link ClassDefinition})</i> or member <i>({@link FieldDefinition} or {@link MethodDefinition})</i>.
 *
 * @author Matt Coley
 */
public interface Definition extends Element, Descriptor, Named {
	/**
	 * @return {@code true} if the current instance is a class or interface.
	 */
	boolean isClass();

	/**
	 * @return {@code true} if the current instance is a field or method.
	 */
	default boolean isMember() {
		return !isClass() && (isField() || isMethod());
	}

	/**
	 * @return {@code true} if the current instance is a field.
	 */
	boolean isField();

	/**
	 * @return {@code true} if the current instance is a method.
	 */
	boolean isMethod();

	/**
	 * @return Member's modifiers.
	 */
	Modifiers getModifiers();

	/**
	 * @return Member's annotations.
	 */
	List<Annotation> getAnnotations();

	/**
	 * @return Member's signature.
	 */
	Signature getSignature();

}
