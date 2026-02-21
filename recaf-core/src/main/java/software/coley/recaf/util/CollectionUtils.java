package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Collection utilities that don't fit anywhere else.
 * These should ideally get migrated to <a href="https://github.com/Col-E/ExtraCollections">Extra-Collections</a>.
 *
 * @author Matt Coley
 */
public class CollectionUtils {
	/**
	 * @param list
	 * 		List to search through.
	 * @param element
	 * 		Element to search for.
	 * @param <T>
	 * 		Inferred element type.
	 *
	 * @return The index of the first occurrence of {@code element} in {@code list}, or -1 if not found.
	 */
	public static <T> int identityIndexOf(@Nonnull List<? extends T> list, T element) {
		for (int i = list.size(); i != 0; )
			if (element == list.get(--i))
				return i;
		return -1;
	}
}
