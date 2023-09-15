package software.coley.recaf.info;

import software.coley.recaf.info.member.ClassMember;

import static java.lang.reflect.Modifier.*;

/**
 * Outline of a class or member with access modifiers.
 *
 * @author Matt Coley
 * @see ClassMember
 * @see ClassInfo
 */
public interface Accessed {
	/**
	 * @return Access modifiers.
	 */
	int getAccess();

	/**
	 * @return {@code true} when this item's access modifiers contains {@code public}.
	 */
	default boolean hasPublicModifier() {
		return hasModifierMask(PUBLIC);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code protected}.
	 */
	default boolean hasProtectedModifier() {
		return hasModifierMask(PROTECTED);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code private}.
	 */
	default boolean hasPrivateModifier() {
		return hasModifierMask(PRIVATE);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains none of:
	 * {@code public}, {@code protected}, or {@code private}.
	 */
	default boolean hasPackagePrivateModifier() {
		return hasNoneOfMask(PRIVATE | PROTECTED | PUBLIC);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code static}.
	 */
	default boolean hasStaticModifier() {
		return hasModifierMask(STATIC);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code final}.
	 */
	default boolean hasFinalModifier() {
		return hasModifierMask(FINAL);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code synchronized}.
	 */
	default boolean hasSynchronizedModifier() {
		return hasModifierMask(SYNCHRONIZED);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code volatile}.
	 */
	default boolean hasVolatileModifier() {
		return hasModifierMask(VOLATILE);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code transient}.
	 */
	default boolean hasTransientModifier() {
		return hasModifierMask(TRANSIENT);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code native}.
	 */
	default boolean hasNativeModifier() {
		return hasModifierMask(NATIVE);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code enum}.
	 */
	default boolean hasEnumModifier() {
		return hasModifierMask(0x00004000);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code annotation}.
	 */
	default boolean hasAnnotationModifier() {
		return hasModifierMask(0x00002000);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code interface}.
	 */
	default boolean hasInterfaceModifier() {
		return hasModifierMask(INTERFACE);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code module}.
	 */
	default boolean hasModuleModifier() {
		return hasModifierMask(0x8000);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code abstract}.
	 */
	default boolean hasAbstractModifier() {
		return hasModifierMask(ABSTRACT);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code strictfp}.
	 */
	default boolean hasStrictFpModifier() {
		return hasModifierMask(STRICT);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code varargs}.
	 */
	default boolean hasVarargsModifier() {
		return hasModifierMask(0x00000080);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code bridge}.
	 */
	default boolean hasBridgeModifier() {
		return hasModifierMask(0x00000040);
	}

	/**
	 * @return {@code true} when this item's access modifiers contains {@code synthetic}.
	 */
	default boolean hasSyntheticModifier() {
		return hasModifierMask(0x00001000);
	}

	/**
	 * @return {@code true} when {@link #hasSyntheticModifier()} or {@link #hasBridgeModifier()} are {@code true}.
	 */
	default boolean isCompilerGenerated() {
		return hasBridgeModifier() || hasSyntheticModifier();
	}

	/**
	 * @param mask
	 * 		Mask to check.
	 *
	 * @return {@code true} if the access modifiers of this item match the given mask.
	 */
	default boolean hasModifierMask(int mask) {
		return (getAccess() & mask) == mask;
	}

	/**
	 * @param modifiers
	 * 		Modifiers to check.
	 *
	 * @return {@code true} if the access modifiers of this item contain all the given modifiers.
	 */
	default boolean hasAllModifiers(int... modifiers) {
		for (int modifier : modifiers)
			if (!hasModifierMask(modifier))
				return false;
		return true;
	}

	/**
	 * @param modifiers
	 * 		Modifiers to check.
	 *
	 * @return {@code true} if the access modifiers of this item contain any the given modifiers.
	 */
	default boolean hasAnyModifiers(int... modifiers) {
		for (int modifier : modifiers)
			if (hasModifierMask(modifier))
				return true;
		return false;
	}

	/**
	 * @param mask
	 * 		Mask to check.
	 *
	 * @return {@code true} if the access modifiers of this item do not match the mask.
	 */
	default boolean hasNoneOfMask(int mask) {
		return (getAccess() & mask) == 0;
	}

	/**
	 * @param modifiers
	 * 		Modifiers to check.
	 *
	 * @return {@code true} if the access modifiers of this item contain none the given modifiers.
	 */
	default boolean hasNoneOfModifiers(int... modifiers) {
		for (int modifier : modifiers)
			if (!hasNoneOfMask(modifier))
				return false;
		return true;
	}
}
