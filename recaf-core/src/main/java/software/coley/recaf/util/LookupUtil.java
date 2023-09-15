package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

/**
 * Utility class that holds the unrestricted {@link Lookup} instance.
 *
 * @author xDark
 */
public final class LookupUtil {
	private static final Lookup LOOKUP;

	/**
	 * Deny public constructions.
	 */
	private LookupUtil() {
	}

	/**
	 * @return {@link Lookup} instance.
	 */
	@Nonnull
	public static Lookup lookup() {
		return LOOKUP;
	}

	static {
		try {
			Field field = ReflectUtil.getDeclaredField(Lookup.class, "IMPL_LOOKUP");
			LOOKUP = (Lookup) field.get(null);
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
