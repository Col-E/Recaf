package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Outline of an Android class.
 *
 * @author Matt Coley
 */
public interface AndroidClassInfo extends ClassInfo {
	/**
	 * @return {@code true} when {@link #asJvmClass()} can act as a mapping operation.
	 * {@code false} when mapping to JVM classes is unsupported.
	 */
	default boolean canMapToJvmClass() {
		return false;
	}

	/**
	 * @return New builder wrapping this class information.
	 */
	@Nonnull
	default AndroidClassInfoBuilder toAndroidBuilder() {
		return new AndroidClassInfoBuilder(this);
	}

	@Override
	default void acceptIfJvmClass(Consumer<JvmClassInfo> action) {
		// no-op
	}

	@Override
	default void acceptIfAndroidClass(Consumer<AndroidClassInfo> action) {
		action.accept(this);
	}

	@Override
	default boolean testIfJvmClass(Predicate<JvmClassInfo> predicate) {
		return false;
	}

	@Override
	default boolean testIfAndroidClass(Predicate<AndroidClassInfo> predicate) {
		return predicate.test(this);
	}

	@Nonnull
	@Override
	default JvmClassInfo asJvmClass() {
		throw new IllegalStateException("Android class cannot be cast to JVM class");
	}

	@Nonnull
	@Override
	default AndroidClassInfo asAndroidClass() {
		return this;
	}

	@Override
	default boolean isJvmClass() {
		return false;
	}

	@Override
	default boolean isAndroidClass() {
		return true;
	}
}
