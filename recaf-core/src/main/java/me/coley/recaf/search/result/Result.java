package me.coley.recaf.search.result;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;

import java.util.Objects;

/**
 * The base result contains location information of the matched value.
 *
 * @author Matt Coley
 */
public abstract class Result implements Comparable<Result> {
	private final CommonClassInfo containingClass;
	private final FieldInfo containingField;
	private final MethodInfo containingMethod;
	private final String containingAnnotation;
	private final int opcode;

	/**
	 * @param builder
	 * 		Builder containing information about the result.
	 */
	public Result(ResultBuilder builder) {
		this.containingClass = builder.getContainingClass();
		this.containingField = builder.getContainingField();
		this.containingMethod = builder.getContainingMethod();
		this.containingAnnotation = builder.getContainingAnnotation();
		this.opcode = builder.getOpcode();
	}

	/**
	 * @return Wrapped value, used internally for {@link #toString()}.
	 */
	protected abstract Object getValue();

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
	 * @return The opcode of the instruction the result was found in.
	 * Or {@code -1} when the result was not found inside an instruction.
	 */
	public int getOpcode() {
		return opcode;
	}

	@Override
	public int compareTo(Result other) {
		return comparableString().compareTo(other.comparableString());
	}

	private String comparableString() {
		StringBuilder sb = new StringBuilder(containingClass.getName());
		if (containingField != null) {
			sb.append(" ").append(containingField.getName());
		} else if (containingMethod != null) {
			sb.append(" ").append(containingMethod.getName());
			if (getOpcode() > 0) {
				sb.append(" ").append(opcode);
			}
		}
		if (containingAnnotation != null) {
			sb.append(" ").append(containingAnnotation);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Result{value=" + getValue() + ", Location=" + comparableString() + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result result = (Result) o;
		return Objects.equals(containingClass, result.containingClass) &&
				Objects.equals(containingField, result.containingField) &&
				Objects.equals(containingMethod, result.containingMethod) &&
				Objects.equals(containingAnnotation, result.containingAnnotation) &&
				opcode == result.opcode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(containingClass, containingField, containingMethod, containingAnnotation, opcode);
	}
}
