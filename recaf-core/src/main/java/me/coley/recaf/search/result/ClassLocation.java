package me.coley.recaf.search.result;

import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;

import java.util.Objects;

/**
 * Location implementation inside a {@link me.coley.recaf.code.ClassInfo}.
 *
 * @author Matt Coley
 */
public class ClassLocation implements Location {
	private final CommonClassInfo containingClass;
	private final FieldInfo containingField;
	private final MethodInfo containingMethod;
	private final String containingAnnotation;
	private final AbstractInstruction instruction;

	/**
	 * @param builder
	 * 		Builder containing information about the parent result.
	 */
	public ClassLocation(ResultBuilder builder) {
		this.containingClass = builder.getContainingClass();
		this.containingField = builder.getContainingField();
		this.containingMethod = builder.getContainingMethod();
		this.containingAnnotation = builder.getContainingAnnotation();
		this.instruction = builder.getInstruction();
	}

	/**
	 * @return The class the result was found in.
	 */
	public CommonClassInfo getContainingClass() {
		return containingClass;
	}

	/**
	 * @return The field the result was found in.
	 * Or {@code null} when the result was not found inside a field.
	 */
	public FieldInfo getContainingField() {
		return containingField;
	}

	/**
	 * @return The method the result was found in.
	 * Or {@code null} when the result was not found inside a method.
	 */
	public MethodInfo getContainingMethod() {
		return containingMethod;
	}

	/**
	 * @return The internal name of the annotation the result was found in.
	 * Or {@code null} when the result was not found inside an annotation.
	 */
	public String getContainingAnnotation() {
		return containingAnnotation;
	}

	/**
	 * @return The instruction the result was found in.
	 * Or {@code null} when the result was not found inside an instruction.
	 */
	public AbstractInstruction getInstruction() {
		return instruction;
	}

	@Override
	public String comparableString() {
		StringBuilder sb = new StringBuilder(containingClass.getName());
		if (containingField != null) {
			sb.append(" ").append(containingField.getName());
		} else if (containingMethod != null) {
			sb.append(" ").append(containingMethod.getName());
			if (getInstruction() != null) {
				sb.append(" ").append(instruction);
			}
		}
		if (containingAnnotation != null) {
			sb.append(" ").append(containingAnnotation);
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassLocation that = (ClassLocation) o;
		return Objects.equals(containingClass, that.containingClass) &&
				Objects.equals(containingField, that.containingField) &&
				Objects.equals(containingMethod, that.containingMethod) &&
				Objects.equals(containingAnnotation, that.containingAnnotation) &&
				Objects.equals(instruction, that.instruction);
	}

	@Override
	public int hashCode() {
		return Objects.hash(containingClass, containingField, containingMethod, containingAnnotation, instruction);
	}

	@Override
	public String toString() {
		return comparableString();
	}
}
