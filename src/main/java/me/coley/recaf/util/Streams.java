package me.coley.recaf.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream utilities.
 * 
 * @author Matt
 */
public class Streams {
	private final static int BUFF_SIZE = (int) Math.pow(128, 2);

	/**
	 * Reads the bytes from the InputStream into a byte array.
	 *
	 * @param is
	 *            InputStream to read from.
	 * @return byte array representation of the input stream.
	 * @throws IOException
	 *             Thrown if the given input stream cannot be read from.
	 */
	public static byte[] from(InputStream is) throws IOException {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			int r;
			byte[] data = new byte[BUFF_SIZE];
			while ((r = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, r);
			}
			buffer.flush();
			return buffer.toByteArray();
		}
	}

	/**
	 * Read InputStream into a string.
	 * 
	 * @param input
	 *            Stream to read from.
	 * @return UTF8 string.
	 * @throws IOException
	 */
	public static String toString(final InputStream input) throws IOException {
		return new String(from(input), StandardCharsets.UTF_8);
	}

	/**
	 * Creates a list of sorted java names from a given collection.
	 *
	 * @param names
	 *            Collection of names.
	 * @return The collection, sorted and converted to a list.
	 */
	public static List<String> sortedNameList(Collection<String> names) {
		return sortJavaNames(names.stream()).collect(Collectors.toList());
	}

	/**
	 * Sorts the stream of java names.
	 *
	 * @param stream
	 *            Stream of names.
	 * @return The stream, sorted.
	 */
	private static Stream<String> sortJavaNames(Stream<String> stream) {
		return stream.sorted(new JavaNameSorter());
	}

	/**
	 * Comparator for sorting java names. Ensures packages are given higher
	 * priority than class file names.
	 * 
	 * @author Matt
	 */
	private static class JavaNameSorter implements Comparator<String> {
		@Override
		public int compare(String name1, String name2) {
			// Split name into sections (by package)
			String[] name1Sections = name1.split("/");
			String[] name2Sections = name2.split("/");
			int name1Length = name1Sections.length;
			int name2Length = name2Sections.length;
			int max = Math.min(name1Length, name2Length);
			// Compare packages
			for (int i = 0; i < max; i++) {
				// Ensure packages appear first in sorted order
				if (i == max - 1 && name1Length != name2Length) {
					return (name1Length > name2Length) ? -1 : 1;
				}
				// Compare section values
				String p1 = name1Sections[i];
				String p2 = name2Sections[i];
				int cmp = p1.compareTo(p2);
				if (cmp != 0) {
					return cmp;
				}
			}
			// All sections are equal.
			return 0;
		}
	}
}