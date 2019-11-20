package me.coley.recaf.search;

import java.util.List;
import java.util.Objects;

/**
 * Search result of a matched class.
 *
 * @author Matt
 */
public class InsnResult extends SearchResult {
	private final int index;
	private final List<String> lines;

	/**
	 * Constructs a insn result.
	 *
	 * @param index
	 * 		Index of first matched item.
	 * @param lines
	 * 		Lines of matched dissasembled method code.
	 */
	public InsnResult(int index, List<String> lines) {
		this.index = index;
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
	@SuppressWarnings("unchecked")
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if (ret == 0) {
			if (other instanceof InsnResult) {
				InsnResult otherResult = (InsnResult) other;
				ret = getContext().getParent().compareTo(otherResult.getContext().getParent());
				// Same method -> sort by index
				if (ret == 0)
					return Integer.compare(index, otherResult.index);
				// Different method
				return ret;
			}
		}
		return ret;
	}
}
