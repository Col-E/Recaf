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

			// If neither are the top-level directory then check if one is a subdir of the other.
			if (directoryPathB.startsWith(directoryPathA))
				return 1;
			else if (directoryPathA.startsWith(directoryPathB))
				return -1;
		}

		// Fallback to natural string comparison, first case-insensitive but then case-sensitive to differentiate.
		int cmp = CaseInsensitiveSimpleNaturalComparator.getInstance().compare(a, b);
		if (cmp == 0)
			cmp = a.compareTo(b);
		return cmp;
	};

	/**
	 * Comparator for {@link Named} items whose names represent file paths.
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
