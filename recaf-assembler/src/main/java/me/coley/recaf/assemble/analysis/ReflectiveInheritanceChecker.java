package me.coley.recaf.assemble.analysis;

import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Type checker that pulls info from runtime classes.
 *
 * @author Matt Coley
 */
public class ReflectiveInheritanceChecker implements InheritanceChecker {
	private static final Map<Class<?>, Set<Class<?>>> PARENTS_LOOKUP = new HashMap<>();
	private static final Map<Class<?>, Set<Class<?>>> ALL_PARENTS_LOOKUP = new HashMap<>();

	@Override
	public String getCommonType(String class1, String class2) {
		try {
			ClassLoader s = ClassLoader.getSystemClassLoader();
			Class<?> a = Class.forName(class1.replace('/', '.'), false, s);
			Class<?> b = Class.forName(class2.replace('/', '.'), false, s);
			return Type.getType(getCommon(a, b)).getInternalName();
		} catch (Exception ex) {
			return "java/lang/Object";
		}
	}

	private static Class<?> getCommon(Class<?> first, Class<?> second) {
		Set<Class<?>> firstParents = getAllParents(first);
		firstParents.add(first);
		// Base case
		if (firstParents.contains(second))
			return second;
		// Iterate over second's parents via breadth-first-search
		Queue<Class<?>> queue = new LinkedList<>();
		queue.add(second);
		do {
			// Item to fetch parents of
			Class<?> next = queue.poll();
			if (next == null || next == Object.class)
				break;
			for (Class<?> parent : getParents(next)) {
				// Parent in the set of visited classes? Then its valid.
				if (firstParents.contains(parent))
					return parent;
				// Queue up the parent
				if (parent != Object.class)
					queue.add(parent);
			}
		} while (!queue.isEmpty());
		// Fallback option
		return Object.class;
	}

	private static Set<Class<?>> getAllParents(Class<?> clazz) {
		return ALL_PARENTS_LOOKUP.computeIfAbsent(clazz, c -> getParents(c).stream()
				.map(n -> getAllParents(n).stream())
				.reduce(getParents(c).stream(), Stream::concat)
				.collect(Collectors.toSet()));
	}

	private static Set<Class<?>> getParents(Class<?> clazz) {
		return PARENTS_LOOKUP.computeIfAbsent(clazz, c -> {
			Set<Class<?>> parents = new HashSet<>();
			if (clazz.getSuperclass() != null)
				parents.add(clazz.getSuperclass());
			parents.addAll(Arrays.asList(clazz.getInterfaces()));
			return parents;
		});
	}
}
