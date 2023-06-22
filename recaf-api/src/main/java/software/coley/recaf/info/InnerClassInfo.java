package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Outline of an inner class for a declaring {@link ClassInfo}.
 * <br>
 * <b>Important note:</b> Oracle's Java Virtual Machine implementation
 * does not check the consistency of an InnerClasses attribute against a class
 * file representing a class or interface referenced by the attribute.
 *
 * @author Matt Coley
 * @author Amejonah
 */
public interface InnerClassInfo extends Accessed, Named {
	@Nonnull
	@Override
	default String getName() {
		return getInnerClassName();
	}

	/**
	 * @return The name of the outer declaring class for this inner class.
	 */
	@Nonnull
	String getOuterDeclaringClassName();

	/**
	 * Given the following example, the inner name is {@code Apple$Worm}.
	 * <pre>
	 * class Apple {
	 *     class Worm {}
	 * }
	 * </pre>
	 *
	 * @return The internal name of an inner class.
	 */
	@Nonnull
	String getInnerClassName();

	/**
	 * Given the following example, the inner name is {@code Apple}.
	 * <pre>
	 * class Apple {
	 *     class Worm {}
	 * }
	 * </pre>
	 *
	 * @return The internal name of the class to which the inner class belongs.
	 * May be {@code null} for anonymous classes.
	 */
	@Nullable
	String getOuterClassName();

	/**
	 * Given the following example, the inner name is {@code Worm}.
	 * <pre>
	 * class Apple {
	 *     class Worm {}
	 * }
	 * </pre>
	 *
	 * @return The <i>(simple)</i> name of the inner class inside its enclosing class.
	 * May be {@code null} for anonymous inner classes.
	 */
	@Nullable
	String getInnerName();

	/**
	 * There are some wierd cases where there can be inner-class entries of classes defined by other classes.
	 * You can use this to filter those cases out.
	 *
	 * @return {@code true} when this inner-class entry denotes an inner-class
	 * reference to a class defined in another class.
	 */
	default boolean isExternalReference() {
		return !getInnerClassName().startsWith(getOuterDeclaringClassName());
	}

	/**
	 * @return The access modifiers of the inner class as originally declared in the enclosing class.
	 */
	default int getInnerAccess() {
		return getAccess();
	}

	/**
	 * @return Either {@link #getInnerName()} if not {@code null},
	 * otherwise the last "part" (after last $ or /) of {@link #getOuterClassName()}.
	 */
	@Nonnull
	default String getSimpleName() {
		// Check for inner name
		String innerName = getInnerName();
		if (innerName != null) return innerName;

		// Substring from outer class prefix
		String outerDeclaringClass = getOuterDeclaringClassName();
		String outerName = getOuterClassName();
		if (outerName != null) {
			int outerDeclaringLength = outerDeclaringClass.length();
			int lastIndex = 0;
			int endIndex = Math.min(outerDeclaringLength, outerName.length());
			for (; lastIndex < endIndex; lastIndex++) {
				if (outerDeclaringClass.charAt(lastIndex) != outerName.charAt(lastIndex)) break;
			}

			// Edge case handling with outer name
			if (lastIndex == 0)
				return outerName;
			else if (outerName.startsWith("$", lastIndex))
				lastIndex++;
			return outerName.substring(lastIndex);
		}

		// Class entry is for anonymous class.
		String innerClassName = getInnerClassName();
		return "Anonymous '" + innerClassName.substring(innerClassName.lastIndexOf('$') + 1) + "'";
	}
}
