package software.coley.recaf.ui.control.richtext.suggest.lookups;

import org.objectweb.asm.Opcodes;

import java.util.function.BooleanSupplier;

/**
 * Member access rules shared by completion languages.
 *
 * @author Matt Coley
 */
public final class MemberVisibility {
	private MemberVisibility() {}

	/**
	 * Access level of a member, as declared by its access flags.
	 */
	public enum Access {
		PUBLIC, PROTECTED, PACKAGE, PRIVATE;

		/**
		 * @param access
		 * 		Raw access flags of a field or method.
		 *
		 * @return Access level. {@code ACC_PUBLIC}, {@code ACC_PROTECTED}, and {@code ACC_PRIVATE} are checked in that
		 * order, otherwise the member is package-private.
		 */
		public static Access fromFlags(int access) {
			if ((access & Opcodes.ACC_PUBLIC) != 0)
				return PUBLIC;
			if ((access & Opcodes.ACC_PROTECTED) != 0)
				return PROTECTED;
			if ((access & Opcodes.ACC_PRIVATE) != 0)
				return PRIVATE;
			return PACKAGE;
		}
	}

	/**
	 * @param access
	 * 		Access level of the member.
	 * @param declaredInCurrentClass
	 *        {@code true} if the member is declared by the class being edited.
	 * @param samePackage
	 *        {@code true} if the member's declaring class is in the same package as the edited class.
	 * @param protectedAccessGranted
	 * 		Supplies whether protected access applies. Only evaluated for {@link Access#PROTECTED} members that are
	 * 		not declared in the current class and not in the same package.
	 *
	 * @return {@code true} if the member is visible from the current context.
	 */
	public static boolean isVisible(Access access,
	                                boolean declaredInCurrentClass,
	                                boolean samePackage,
	                                BooleanSupplier protectedAccessGranted) {
		if (access == Access.PUBLIC)
			return true;
		if (declaredInCurrentClass)
			return true;
		if (access == Access.PRIVATE)
			return false;
		if (samePackage)
			return true;
		if (access == Access.PROTECTED)
			return protectedAccessGranted.getAsBoolean();
		return false;
	}
}
