package me.coley.recaf.assemble.util;

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
	private static final ReflectiveInheritanceChecker INSTANCE = new ReflectiveInheritanceChecker();
	private static final Map<Class<?>, Set<Class<?>>> PARENTS_LOOKUP = new HashMap<>();
	private static final Map<Class<?>, Set<Class<?>>> ALL_PARENTS_LOOKUP = new HashMap<>();

	private ReflectiveInheritanceChecker() {
		// disallow creation
	}

	/**
	 * @return Singleton instance.
	 */
	public static ReflectiveInheritanceChecker getInstance() {
		return INSTANCE;
	}

	@Override
	public String getCommonType(String class1, String class2) {
		try {
			ClassLoader s = ClassLoader.getSystemClassLoader();
			Class<?> a = Class.forName(class1.replace('/', '.'), false, s);
			Class<?> b = Class.forName(class2.replace('/', '.'), false, s);
			return Type.getType(getCommon(a, b)).getInternalName();
		} catch (Throwable t) {
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
		// ComputeIfAbsent is not allowed here, see: JDK-8071667
		Set<Class<?>> parents = ALL_PARENTS_LOOKUP.get(clazz);
		if (parents == null) {
			parents = getParents(clazz).stream()
					.map(n -> getAllParents(n).stream())
					.reduce(getParents(clazz).stream(), Stream::concat)
					.collect(Collectors.toSet());
			ALL_PARENTS_LOOKUP.put(clazz, parents);
		}
		return parents;
	}

	private static Set<Class<?>> getParents(Class<?> clazz) {
		// ComputeIfAbsent is not allowed here, see: JDK-8071667
		Set<Class<?>> parents = PARENTS_LOOKUP.get(clazz);
		if (parents == null) {
			parents = new HashSet<>();
			if (clazz.getSuperclass() != null)
				parents.add(clazz.getSuperclass());
			parents.addAll(Arrays.asList(clazz.getInterfaces()));
			PARENTS_LOOKUP.put(clazz, parents);
		}
		return parents;
	}
}
