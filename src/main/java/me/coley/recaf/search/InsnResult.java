package me.coley.recaf.search;

import java.util.List;
import java.util.Objects;

/**
 * Search result of a matched class.
 *
 * @author Matt
 */
public class InsnResult extends SearchResult {
	private final List<String> lines;

	/**
	 * Constructs a class result.
	 *
	 * @param lines
	 * 		Lines of dissasembled method code.
	 */
	public InsnResult(List<String> lines) {
		this.lines = lines;
	}

	/**
	 * @return Matched Lines of dissasembled method code.
	 */
	public List<String> getLines() {
		return lines;
	}

	@Override
	public String toString() {
		return String.join("\n", lines);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lines);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InsnResult)
			return hashCode() == other.hashCode();
		return false;
	}

	@Override
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if (ret == 0) {
			if (other instanceof InsnResult) {
				InsnResult otherResult = (InsnResult) other;
				List<String> otherLines = otherResult.getLines();
				if(lines.size() == otherLines.size()) {
					int cmp = 0;
					for(int i = 0; i < lines.size() && cmp == 0; i++)
						cmp = lines.get(i).compareTo(otherLines.get(i));
					return cmp;
				}
			}
		}
		return ret;
	}
}
