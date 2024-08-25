package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Basic implementation of inner class info.
 *
 * @author Matt Coley
 */
public class BasicInnerClassInfo implements InnerClassInfo {
	private final String outerDeclaringClassName; // Recaf specific, not modeling class spec
	private final String innerClassName;
	private final String outerClassName;
	private final String innerName;
	private final int access;
	private String simpleName;

	/**
	 * @param outerDeclaringClassName
	 * 		Declaring class name,
	 * @param innerClassName
	 * 		Inner name.
	 * @param outerClassName
	 * 		Outer name.
	 * @param innerName
	 * 		Local inner name.
	 * @param access
	 * 		Inner class flags originally declared.
	 */
	public BasicInnerClassInfo(String outerDeclaringClassName, String innerClassName,
	                           String outerClassName, String innerName, int access) {
		this.outerDeclaringClassName = outerDeclaringClassName;
		this.innerClassName = innerClassName;
		this.outerClassName = outerClassName;
		this.innerName = innerName;
		this.access = access;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Nonnull
	@Override
	public String getOuterDeclaringClassName() {
		return outerDeclaringClassName;
	}

	@Nonnull
	@Override
	public String getInnerClassName() {
		return innerClassName;
	}

	@Override
	public String getOuterClassName() {
		return outerClassName;
	}

	@Override
	public String getInnerName() {
		return innerName;
	}

	@Nonnull
	@Override
	public String getSimpleName() {
		// Cache simple name computation
		if (simpleName == null) simpleName = InnerClassInfo.super.getSimpleName();
		return simpleName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;

		if (o instanceof InnerClassInfo inner) {
			if (access != inner.getAccess()) return false;
			if (!innerClassName.equals(inner.getInnerClassName())) return false;
			if (!Objects.equals(outerClassName, inner.getOuterClassName())) return false;
			return Objects.equals(innerName, inner.getInnerName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = innerClassName.hashCode();
		result = 31 * result + (outerClassName != null ? outerClassName.hashCode() : 0);
		result = 31 * result + (innerName != null ? innerName.hashCode() : 0);
		result = 31 * result + access;
		return result;
	}

	@Override
	public String toString() {
		return "Inner class: " + getSimpleName();
	}
}
