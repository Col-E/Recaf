package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.util.StringUtil;

import java.util.Comparator;
import java.util.Objects;

/**
 * Outline of a type that can be identified by name.
 *
 * @author Matt Coley
 */
public interface Named {
	/**
	 * Comparator for {@link String} items.
	 * First compares case-insensitively with natural ordering, then falls back to case-sensitive comparison.
	 */
	Comparator<String> STRING_COMPARATOR = (a, b) -> {
		// Fallback to natural string comparison, first case-insensitive but then case-sensitive to differentiate.
		int cmp = CaseInsensitiveSimpleNaturalComparator.getInstance().compare(a, b);
		if (cmp == 0)
			cmp = a.compareTo(b);
		return cmp;
	};

	/**
	 * Comparator for {@link Named} items.
	 * First compares case-insensitively with natural ordering, then falls back to case-sensitive comparison.
	 *
	 * @see #STRING_COMPARATOR
	 */
	Comparator<Named> NAMED_COMPARATOR = (o1, o2) -> {
		String a = o1.getName();
		String b = o2.getName();
		return STRING_COMPARATOR.compare(a, b);
	};

	/**
	 * Comparator for {@link String} items whose content represent file paths.
	 */
	@SuppressWarnings("StringEquality")
	Comparator<String> STRING_PATH_COMPARATOR = (a, b) -> {
		// Get parent directory path for each item.
		String directoryPathA = StringUtil.cutOffAtLast(a, '/');
		String directoryPathB = StringUtil.cutOffAtLast(b, '/');
		if (!Objects.equals(directoryPathA, directoryPathB)) {
			// The directory path is the input path (same reference) if there is no '/'.
			// We always want root paths to be shown first since we group them in a container directory anyways.
			if (directoryPathA == a && directoryPathB != b)
				return -1;
			if (directoryPathA != a && directoryPathB == b)
				return 1;

			// We want subdirectories to be shown first over files in the directory.
			// The top-level directory being an empty string is an edge case.
			if (directoryPathA.isEmpty())
				return -1;
			else if (directoryPathB.isEmpty())
				return 1;

			// If both paths have the same number of separators, then we can do a normal string comparison on the directory paths.
			// If they have different numbers of separators, then we want to check if one is a parent of the other (or vice versa).
			int sectionCountA = StringUtil.count('/', directoryPathA);
			int sectionCountB = StringUtil.count('/', directoryPathB);
			if (sectionCountA == sectionCountB) {
				// Compare directories as paths.
				int cmp = STRING_COMPARATOR.compare(directoryPathA, directoryPathB);
				if (cmp != 0)
					return cmp;
			} else {
				// Check for parent-child relationship between the directory paths. The parent directory should be shown first.
				if (directoryPathB.startsWith(directoryPathA))
					return 1;
				else if (directoryPathA.startsWith(directoryPathB))
					return -1;
			}
		}

		return STRING_COMPARATOR.compare(a, b);
	};

	/**
	 * Comparator for {@link Named} items whose names represent file paths.
	 *
	 * @see #STRING_PATH_COMPARATOR
	 */
	Comparator<Named> NAMED_PATH_COMPARATOR = (o1, o2) -> {
		String a = o1.getName();
		String b = o2.getName();
		return STRING_PATH_COMPARATOR.compare(a, b);
	};

	/**
	 * @return Identifying name.
	 */
	@Nonnull
	String getName();
}
