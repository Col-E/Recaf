package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a single variable.
 *
 * @author Matt Coley
 * @see Variables Consolidated.
 */
public class VariableInfo implements Comparable<VariableInfo> {
	private static final List<Type> UNKNOWN_USAGE = List.of(Types.OBJECT_TYPE);
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
		List<Type> usedTypes = getUsages();
		return usedTypes.get(usedTypes.size() - 1);
	}

	/**
	 * @param checker
	 * 		Inheritance checker to compute common types.
	 *
	 * @return Common type of all usages.
	 */
	public Type getCommonType(InheritanceChecker checker) {
		List<Type> usedTypes = getUsages();
		Type first = usedTypes.get(0);
		if (Types.isPrimitive(first.getDescriptor())) {
			// Primitives just need to be the widest type
			Type widest = first;
			for (Type usage : usedTypes) {
				if (usage.getSort() > widest.getSort())
					widest = usage;
			}
			return widest;
		} else {
			// Object types need a common parent
			String commonName = first.getInternalName();
			int i = 1;
			while (i < usedTypes.size()) {
				commonName = checker.getCommonType(commonName, usedTypes.get(i).getInternalName());
				i++;
			}
			return Type.getObjectType(commonName);
		}
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
	 * Clears usages.
	 */
	public void clearUsages() {
		usages.clear();
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
		// Because we choose to omit 'Object' as a usage when recording things such as:
		//   ACONST_NULL
		//   ASTORE name
		// We will lazily include the 'Object' type here.
		// 'Object' is only included as a usage if the exact type of 'Object' is explicitly used.
		if (usages.isEmpty())
			return UNKNOWN_USAGE;
		return usages;
	}

	@Override
	public int compareTo(VariableInfo other) {
		int cmp = Integer.compare(index, other.index);
		if (cmp == 0) {
			cmp = name.compareTo(other.name);
		}
		return cmp;
	}
}
