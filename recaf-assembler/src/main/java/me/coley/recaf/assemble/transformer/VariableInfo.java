package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.Element;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Information about a single variable.
 *
 * @author Matt Coley
 * @see Variables Consolidated.
 */
public class VariableInfo implements Comparable<VariableInfo> {
	private final List<Type> usages = new ArrayList<>();
	private final List<Element> sources = new ArrayList<>();
	private final int index;
	private boolean usesWide;
	private String name;

	/**
	 * @param index
	 * 		Variable index.
	 */
	public VariableInfo(int index) {
		this.index = index;
	}

	/**
	 * @return Last usage type of the variable.
	 */
	public Type getLastUsedType() {
		return usages.get(usages.size() - 1);
	}

	/**
	 * @param type
	 * 		Type that the variable holds at some point.
	 */
	public void addType(Type type) {
		if (!usages.contains(type))
			usages.add(type);
	}

	/**
	 * @param source
	 * 		AST element source of a variable usage.
	 */
	public void addSource(Element source) {
		if (!sources.contains(source))
			sources.add(source);
	}

	/**
	 * Marks this variable index as being used for wide-type storage.
	 */
	public void markUsesWide() {
		this.usesWide = true;
	}

	/**
	 * @param name
	 * 		Variable identifier / name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Variable identifier / name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Index of the variable.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return {@code true} when this slot <i>at any point/scope</i>
	 * stores a wide type <i>({@code long}/{@code double})</i>.
	 */
	public boolean usesWide() {
		return usesWide;
	}

	/**
	 * @return First contributing element.
	 */
	public Element getFirstSource() {
		if (sources.isEmpty())
			return null;
		return sources.get(0);
	}

	/**
	 * @return Last contributing element.
	 */
	public Element getLastSource() {
		if (sources.isEmpty())
			return null;
		return sources.get(sources.size() - 1);
	}

	/**
	 * @return List of elements that reference the variable.
	 */
	public List<Element> getSources() {
		return sources;
	}

	/**
	 * @return List of direct type usages of the variable.
	 */
	public List<Type> getUsages() {
		return usages;
	}

	@Override
	public int compareTo(@Nonnull VariableInfo other) {
		return Comparator.comparingInt(VariableInfo::getIndex)
				.thenComparing(VariableInfo::getName)
				.compare(this, other);
	}
}
