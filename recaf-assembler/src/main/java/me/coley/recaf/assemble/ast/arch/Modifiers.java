package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A collection of {@link Modifier}s to apply to a {@link FieldDefinition} or {@link MethodDefinition}.
 *
 * @author Matt Coley
 */
public class Modifiers extends BaseElement implements Element, Iterable<Modifier> {
	private final List<Modifier> modifiers = new ArrayList<>();

	/**
	 * @return Modifiers masked value.
	 */
	public int value() {
		int value = 0;
		for (Modifier modifier : modifiers)
			value |= modifier.getValue();
		return value;
	}

	/**
	 * @param modifier
	 * 		Modifier to add.
	 */
	public void add(Modifier modifier) {
		modifiers.add(modifier);
	}

	@Override
	public String print() {
		return modifiers.stream()
				.map(m -> '.' + m.getName())
				.collect(Collectors.joining(" "));
	}

	@Override
	public Iterator<Modifier> iterator() {
		return modifiers.listIterator();
	}
}
