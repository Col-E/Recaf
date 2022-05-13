package me.coley.recaf.search.result;

import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
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
	private String comparisonString;
	private String instructionAsString;

	/**
	 * @param builder Builder containing information about the parent result.
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
	public int compareTo(Location o) {
		if (!(o instanceof ClassLocation)) {
			return Location.super.compareTo(o);
		}
		ClassLocation other = (ClassLocation) o;
		int cmp = containingClass.getName().compareTo(other.containingClass.getName());
		if (cmp != 0)
			return cmp;
		MemberInfo thisMember = this.containingMethod;
		if (thisMember == null)
			thisMember = this.containingField;
		if (thisMember == null)
			return -1;
		MemberInfo otherMember = other.containingMethod;
		if (otherMember == null)
			otherMember = other.containingField;
		if (otherMember == null)
			return 1;
		cmp = thisMember.getName().compareTo(otherMember.getName());
		if (cmp != 0)
			return cmp;
		if (instruction == null)
			return -1;
		if (other.instruction == null)
			return 1;
		cmp = getInstructionAsString().compareTo(other.getInstructionAsString());
		if (cmp != 0)
			return cmp;
		String containingAnnotation = this.containingAnnotation;
		String otherAnnotation = other.containingAnnotation;
		return containingAnnotation == null ? 1 : otherAnnotation == null ? -1 : containingAnnotation.compareTo(otherAnnotation);
	}

	@Override
	public String comparableString() {
		String comparisonString = this.comparisonString;
		if (comparisonString == null) {
			StringBuilder sb = new StringBuilder(containingClass.getName());
			if (containingField != null) {
				sb.append(' ').append(containingField.getName());
			} else if (containingMethod != null) {
				sb.append(' ').append(containingMethod.getName());
				if (getInstruction() != null) {
					sb.append(' ').append(instruction);
				}
			}
			if (containingAnnotation != null) {
				sb.append(' ').append(containingAnnotation);
			}
			return this.comparisonString = sb.toString();
		}
		return comparisonString;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClassLocation that = (ClassLocation) o;
		return Objects.equals(containingClass, that.containingClass) &&
			Objects.equals(containingField, that.containingField) &&
			Objects.equals(containingMethod, that.containingMethod) &&
			Objects.equals(containingAnnotation, that.containingAnnotation) &&
			Objects.equals(instruction, that.instruction);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(containingClass);
		result = 31 * result + Objects.hashCode(containingField);
		result = 31 * result + Objects.hashCode(containingMethod);
		result = 31 * result + Objects.hashCode(containingAnnotation);
		result = 31 * result + Objects.hashCode(instruction);
		return result;
	}

	@Override
	public String toString() {
		return comparableString();
	}

	private String getInstructionAsString() {
		String instructionAsString = this.instructionAsString;
		if (instructionAsString == null) {
			AbstractInstruction instruction = this.instruction;
			return instruction == null ? "" : (this.instructionAsString = instruction.toString());
		}
		return instructionAsString;
	}
}
