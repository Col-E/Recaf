package me.coley.recaf.bytecode;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.util.*;
import java.util.stream.Collectors;

public enum AccessFlag {
	// @formatter:off
	ACC_PUBLIC      (0x0001, "public",          true,  Type.CLASS, Type.INNER_CLASS, Type.METHOD, Type.FIELD),
	ACC_PRIVATE     (0x0002, "private",         true,  Type.INNER_CLASS, Type.METHOD, Type.FIELD),
	ACC_PROTECTED   (0x0004, "protected",       true,  Type.INNER_CLASS, Type.METHOD, Type.FIELD),
	ACC_STATIC      (0x0008, "static",          true,  Type.INNER_CLASS, Type.METHOD, Type.FIELD),
	ACC_FINAL       (0x0010, "final",           true,  Type.CLASS, Type.INNER_CLASS, Type.METHOD, Type.FIELD, Type.PARAM),
	ACC_SYNCHRONIZED(0x0020, "synchronized",    true,  Type.METHOD),
	ACC_SUPER       (0x0020, "super",           false, Type.CLASS),
	ACC_BRIDGE      (0x0040, "bridge",          false, Type.METHOD),
	ACC_VOLATILE    (0x0040, "volatile",        true,  Type.FIELD),
	ACC_VARARGS     (0x0080, "varargs",         false, Type.METHOD),
	ACC_TRANSIENT   (0x0080, "transient",       true,  Type.FIELD),
	ACC_NATIVE      (0x0100, "native",          true,  Type.METHOD),
	ACC_INTERFACE   (0x0200, "interface",       true,  Type.CLASS, Type.INNER_CLASS),
	ACC_ABSTRACT    (0x0400, "abstract",        true,  Type.CLASS, Type.INNER_CLASS, Type.METHOD),
	ACC_STRICT      (0x0800, "strictfp",        true,  Type.METHOD),
	ACC_SYNTHETIC   (0x1000, "synthetic",       false, Type.CLASS, Type.INNER_CLASS, Type.METHOD, Type.FIELD, Type.PARAM),
	ACC_ANNOTATION  (0x2000, "annotation",      false, Type.CLASS, Type.INNER_CLASS),
	ACC_ENUM        (0x4000, "enum",            true,  Type.CLASS, Type.INNER_CLASS, Type.FIELD),
	ACC_MODULE      (0x8000, "module",          false, Type.CLASS),
	ACC_MANDATED    (0x8000, "mandated",        false, Type.PARAM);
	// @formatter:on

	private static final Joiner JOINER = Joiner.on(' ').skipNulls();
	private static final SetMultimap<Integer, AccessFlag> maskToFlagsMap;
	private static final Map<String, AccessFlag> nameToFlagMap;
	private static final SetMultimap<Type, AccessFlag> typeToFlagsMap;

	static {
		AccessFlag[] flags = values();
		SetMultimap<Integer, AccessFlag> maskMap = MultimapBuilder.SetMultimapBuilder
				.linkedHashKeys(flags.length)
				.enumSetValues(AccessFlag.class)
				.build();
		Map<String, AccessFlag> nameMap = new LinkedHashMap<>(flags.length);
		SetMultimap<Type, AccessFlag> typeMap = MultimapBuilder.SetMultimapBuilder
				.enumKeys(Type.class)
				.enumSetValues(AccessFlag.class)
				.build();
		for (AccessFlag flag : flags) {
			maskMap.put(flag.mask, flag);
			nameMap.put(flag.name, flag);
			flag.types.forEach(type -> typeMap.put(type, flag));
		}
		maskToFlagsMap = Multimaps.unmodifiableSetMultimap(maskMap);
		nameToFlagMap = Collections.unmodifiableMap(nameMap);
		typeToFlagsMap = Multimaps.unmodifiableSetMultimap(typeMap);
		Type.populateOrder();  // lazy load
	}

	public static Set<AccessFlag> getFlags(int mask) {
		return maskToFlagsMap.get(mask);
	}

	public static Set<AccessFlag> getApplicableFlags(Type type) {
		return typeToFlagsMap.get(type);
	}

	public static Set<AccessFlag> parse(Type type, int acc) {
		Set<AccessFlag> flags = EnumSet.noneOf(AccessFlag.class);
		for (AccessFlag applicableFlag : getApplicableFlags(type)) {
			if (applicableFlag.has(acc)) {
				flags.add(applicableFlag);
			}
		}
		return flags;
	}

	public static List<AccessFlag> sort(Type type, Collection<AccessFlag> flags) {
		List<AccessFlag> list;
		if (flags instanceof List) {
			list = (List<AccessFlag>) flags;
		} else {
			list = new ArrayList<>(flags);
		}
		list.sort(type.recommendOrderComparator);
		return list;
	}

	public static String toString(Iterable<AccessFlag> flags) {
		// Don't include ACC_SUPER, is meaningless
		return JOINER.join(Iterables.filter(flags, flag -> flag != ACC_SUPER));
	}

	public static String sortAndToString(Type type, Collection<AccessFlag> flags) {
		List<AccessFlag> list;
		try {
			list = sort(type, flags);
		} catch (UnsupportedOperationException ex) { // Collection is unmodifiable
			list = sort(type, new ArrayList<>(flags));
		}
		return toString(list);
	}

	public static String sortAndToString(Type type, int acc) {
		return sortAndToString(type, parse(type, acc));
	}

	public static int createAccess(AccessFlag... flags) {
		int acc = 0;
		for (AccessFlag flag : flags) flag.set(acc);
		return acc;
	}

	public static boolean hasAll(int acc, AccessFlag... flags) {
		for (AccessFlag flag : flags) {
			if (!flag.has(acc)) return false;
		}
		return true;
	}

	public final int mask;
	public final String name;
	public final boolean isKeyword;
	public final Set<Type> types;

	AccessFlag(int mask, String name, boolean isKeyword, Set<Type> types) {
		this.mask = mask;
		this.name = name;
		this.isKeyword = isKeyword;
		this.types = Collections.unmodifiableSet(types);
	}

	AccessFlag(int mask, String name, boolean isKeyword, Type type) {
		this(mask, name, isKeyword, Collections.singleton(type));
	}

	AccessFlag(int mask, String name, boolean isKeyword, Type firstType, Type... restTypes) {
		this(mask, name, isKeyword, EnumSet.of(firstType, restTypes));
	}

	public boolean has(int access) {
		return (access & mask) != 0;
	}

	public int set(int access) {
		return access | mask;
	}

	public int clear(int access) {
		return access & (~mask);
	}

	public String getName() {
		return name;
	}

	public String getCodeFriendlyName() {
		return isKeyword ? this.name : "/* " + name + " */"; // comment out non-keyword
	}

	@Override
	public String toString() {
		return getCodeFriendlyName();
	}

	// @formatter:off
	public static boolean isPublic(int acc)         { return ACC_PUBLIC.has(acc); }
	public static boolean isPrivate(int acc)        { return ACC_PRIVATE.has(acc); }
	public static boolean isProtected(int acc)      { return ACC_PROTECTED.has(acc); }
	public static boolean isStatic(int acc)         { return ACC_STATIC.has(acc); }
	public static boolean isFinal(int acc)          { return ACC_FINAL.has(acc); }
	public static boolean isSynchronized(int acc)   { return ACC_SYNCHRONIZED.has(acc); }
	public static boolean isSuper(int acc)          { return ACC_SUPER.has(acc); }
	public static boolean isBridge(int acc)         { return ACC_BRIDGE.has(acc); }
	public static boolean isVolatile(int acc)       { return ACC_VOLATILE.has(acc); }
	public static boolean isVarargs(int acc)        { return ACC_VARARGS.has(acc); }
	public static boolean isTransient(int acc)      { return ACC_TRANSIENT.has(acc); }
	public static boolean isNative(int acc)         { return ACC_NATIVE.has(acc); }
	public static boolean isInterface(int acc)      { return ACC_INTERFACE.has(acc); }
	public static boolean isAbstract(int acc)       { return ACC_ABSTRACT.has(acc); }
	public static boolean isStrict(int acc)         { return ACC_STRICT.has(acc); }
	public static boolean isSynthetic(int acc)      { return ACC_SYNTHETIC.has(acc); }
	public static boolean isAnnotation(int acc)     { return ACC_ANNOTATION.has(acc); }
	public static boolean isEnum(int acc)           { return ACC_ENUM.has(acc); }
	public static boolean isModule(int acc)         { return ACC_MODULE.has(acc); }
	public static boolean isMandated(int acc)       { return ACC_MANDATED.has(acc); }
	// @formatter:on

	public enum Type {
		CLASS("public abstract final strictfp"),
		INNER_CLASS("public protected private abstract static final strictfp"),
		METHOD("public protected private abstract static final synchronized native strictfp"),
		FIELD("public protected private static final transient volatile"),
		PARAM("final");

		private static void populateOrder() {  // lazy load
			for (Type type : values()) {
				List<AccessFlag> orderList = type.orderList;
				orderList.clear();
				orderList.addAll(parseModifierOrder(type.order));
			}
		}

		private static List<AccessFlag> parseModifierOrder(String string) {
			if (string == null) return Collections.emptyList();
			return Arrays.stream(string.split(" "))
					.map(nameToFlagMap::get)
					.map(Objects::requireNonNull)
					.collect(Collectors.toList());
		}

		private final String order;
		private final List<AccessFlag> orderList = new ArrayList<>();

		// an unmodifiable view of `orderList`
		public final List<AccessFlag> recommendOrder = Collections.unmodifiableList(orderList);
		public final Comparator<AccessFlag> recommendOrderComparator;

		Type(String recommendOrder) {
			this.order = recommendOrder;
			this.recommendOrderComparator = Comparator.comparingInt(this::index);
		}

		private int index(AccessFlag flag) {
			if (recommendOrder.isEmpty()) return 0; // not intialized yet
			int idx = recommendOrder.indexOf(flag);
			return idx == -1 ? Integer.MAX_VALUE : idx;
		}
	}
}