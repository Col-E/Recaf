package software.coley.recaf.services.stub;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.visitors.MethodNoopingVisitor;
import software.coley.recaf.util.visitors.MethodPredicate;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Provides strategies for replacing method bodies with simple return values.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class StubbingService implements Service {
	public static final String SERVICE_ID = "stubbing";
	private final StubbingServiceConfig config;
	private final Map<String, List<StubStrategy>> typeStubbings = new HashMap<>();

	@Inject
	public StubbingService(@Nonnull StubbingServiceConfig config) {
		this.config = config;

		// Void methods only need a bare return.
		add("V", strategy("menu.edit.stub.return", (mv, type) -> mv.visitInsn(Opcodes.RETURN)));

		// Integer-like primitives all use the integer return instructions.
		add("Z", strategy("menu.edit.stub.false", StubbingService::returnFalse));
		add("Z", strategy("menu.edit.stub.true", StubbingService::returnTrue));
		for (String descriptor : List.of("B", "C", "S", "I")) {
			add(descriptor, strategy("menu.edit.stub.zero", StubbingService::returnZeroInt));
			add(descriptor, strategy("menu.edit.stub.one", StubbingService::returnOneInt));
		}
		add("J", strategy("menu.edit.stub.zero", StubbingService::returnZeroLong));
		add("J", strategy("menu.edit.stub.one", StubbingService::returnOneLong));
		add("F", strategy("menu.edit.stub.zero", StubbingService::returnZeroFloat));
		add("F", strategy("menu.edit.stub.one", StubbingService::returnOneFloat));
		add("D", strategy("menu.edit.stub.zero", StubbingService::returnZeroDouble));
		add("D", strategy("menu.edit.stub.one", StubbingService::returnOneDouble));

		// Common collection interfaces and implementations can return useful empty values.
		for (String descriptor : List.of(
				"Ljava/lang/Iterable;",
				"Ljava/util/Collection;",
				"Ljava/util/List;",
				"Ljava/util/Set;",
				"Ljava/util/SortedSet;",
				"Ljava/util/NavigableSet;",
				"Ljava/util/Map;",
				"Ljava/util/SortedMap;",
				"Ljava/util/NavigableMap;",
				"Ljava/util/Queue;",
				"Ljava/util/Deque;",
				"Ljava/util/Iterator;",
				"Ljava/util/ListIterator;",
				"Ljava/util/Enumeration;",
				"Ljava/util/ArrayList;",
				"Ljava/util/ArrayDeque;",
				"Ljava/util/HashMap;",
				"Ljava/util/HashSet;",
				"Ljava/util/LinkedHashMap;",
				"Ljava/util/LinkedHashSet;",
				"Ljava/util/LinkedList;",
				"Ljava/util/PriorityQueue;",
				"Ljava/util/Properties;",
				"Ljava/util/Stack;",
				"Ljava/util/TreeMap;",
				"Ljava/util/TreeSet;",
				"Ljava/util/Vector;",
				"Ljava/util/WeakHashMap;",
				"Ljava/util/concurrent/ConcurrentHashMap;",
				"Ljava/util/concurrent/ConcurrentLinkedDeque;",
				"Ljava/util/concurrent/ConcurrentLinkedQueue;",
				"Ljava/util/concurrent/ConcurrentSkipListMap;",
				"Ljava/util/concurrent/ConcurrentSkipListSet;",
				"Ljava/util/concurrent/CopyOnWriteArrayList;",
				"Ljava/util/concurrent/CopyOnWriteArraySet;",
				"Ljava/util/concurrent/LinkedBlockingDeque;",
				"Ljava/util/concurrent/LinkedBlockingQueue;",
				"Ljava/util/concurrent/LinkedTransferQueue;",
				"Ljava/util/concurrent/PriorityBlockingQueue;")) {
			add(descriptor, strategy("menu.edit.stub.null", StubbingService::returnNull));
			add(descriptor, strategy("menu.edit.stub.empty-collection", StubbingService::returnEmptyCollection));
		}

		// Strings have a useful non-null simple value in addition to the generic null value.
		add("Ljava/lang/String;", strategy("menu.edit.stub.null", StubbingService::returnNull));
		add("Ljava/lang/String;", strategy("menu.edit.stub.empty-string", StubbingService::returnEmptyString));
	}

	/**
	 * @param descriptor
	 * 		Return type descriptor.
	 * @return Strategies available for the return type.
	 */
	@Nonnull
	public List<StubStrategy> getStrategies(@Nonnull String descriptor) {
		List<StubStrategy> strategies = typeStubbings.get(descriptor);
		if (strategies != null)
			return List.copyOf(strategies);

		// Reference descriptors are exact map keys. Populate the fallback lazily so plugin/custom
		// callers can subsequently add strategies for the precise type they requested.
		if (descriptor.startsWith("L"))
			return getOrCreate(descriptor, List.of(strategy("menu.edit.stub.null", StubbingService::returnNull)));
		if (descriptor.startsWith("["))
			return getOrCreate(descriptor, List.of(
					strategy("menu.edit.stub.null", StubbingService::returnNull),
					strategy("menu.edit.stub.empty-array", StubbingService::returnEmptyArray)));
		return List.of();
	}

	/**
	 * Alias for {@link #getStrategies(String)} using the service's stubbing terminology.
	 *
	 * @param descriptor
	 * 		Return type descriptor.
	 * @return Strategies available for the return type.
	 */
	@Nonnull
	public List<StubStrategy> getStubbings(@Nonnull String descriptor) {
		return getStrategies(descriptor);
	}

	/**
	 * Adds a strategy for a return type descriptor.
	 *
	 * @param descriptor
	 * 		Return type descriptor.
	 * @param strategy
	 * 		Strategy to add.
	 */
	public void add(@Nonnull String descriptor, @Nonnull StubStrategy strategy) {
		typeStubbings.computeIfAbsent(descriptor, ignored -> new ArrayList<>()).add(strategy);
	}

	/**
	 * Applies a strategy to the selected methods.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class declaring the methods.
	 * @param methods
	 * 		Methods to stub.
	 * @param strategy
	 * 		Strategy to apply.
	 */
	public void stubMethods(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle,
	                        @Nonnull JvmClassInfo declaringClass,
	                        @Nonnull Collection<MethodMember> methods,
	                        @Nonnull StubStrategy strategy) {
		ClassReader reader = declaringClass.getClassReader();
		ClassWriter writer = new ClassWriter(reader, 0);
		MethodNoopingVisitor visitor = new MethodNoopingVisitor(writer, MethodPredicate.of(methods), strategy.action());
		reader.accept(visitor, declaringClass.getClassReaderFlags());
		bundle.put(declaringClass.toJvmClassBuilder()
				.adaptFrom(writer.toByteArray())
				.build());
	}

	@Nonnull
	private List<StubStrategy> getOrCreate(@Nonnull String descriptor, @Nonnull List<StubStrategy> strategies) {
		return List.copyOf(typeStubbings.computeIfAbsent(descriptor, ignored -> new ArrayList<>(strategies)));
	}

	@Nonnull
	private static StubStrategy strategy(@Nonnull String key, @Nonnull BiConsumer<MethodVisitor, Type> action) {
		return new StubStrategy(() -> key, action);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public StubbingServiceConfig getServiceConfig() {
		return config;
	}

	private static void returnFalse(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitInsn(Opcodes.IRETURN);
	}

	private static void returnTrue(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IRETURN);
	}

	private static void returnZeroInt(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitInsn(Opcodes.IRETURN);
	}

	private static void returnOneInt(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IRETURN);
	}

	private static void returnZeroLong(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.LCONST_0);
		mv.visitInsn(Opcodes.LRETURN);
	}

	private static void returnOneLong(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.LCONST_1);
		mv.visitInsn(Opcodes.LRETURN);
	}

	private static void returnZeroFloat(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.FCONST_0);
		mv.visitInsn(Opcodes.FRETURN);
	}

	private static void returnOneFloat(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.FCONST_1);
		mv.visitInsn(Opcodes.FRETURN);
	}

	private static void returnZeroDouble(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.DCONST_0);
		mv.visitInsn(Opcodes.DRETURN);
	}

	private static void returnOneDouble(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.DCONST_1);
		mv.visitInsn(Opcodes.DRETURN);
	}

	private static void returnNull(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ACONST_NULL);
		mv.visitInsn(Opcodes.ARETURN);
	}

	private static void returnEmptyString(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitLdcInsn("");
		mv.visitInsn(Opcodes.ARETURN);
	}

	private static void returnEmptyCollection(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		String name = type.getInternalName();
		String factory;
		String factoryDescriptor;
		switch (name) {
			case "java/lang/Iterable", "java/util/Collection", "java/util/List" -> {
				factory = "emptyList";
				factoryDescriptor = "()Ljava/util/List;";
			}
			case "java/util/Set" -> {
				factory = "emptySet";
				factoryDescriptor = "()Ljava/util/Set;";
			}
			case "java/util/SortedSet" -> {
				factory = "emptySortedSet";
				factoryDescriptor = "()Ljava/util/SortedSet;";
			}
			case "java/util/NavigableSet" -> {
				factory = "emptyNavigableSet";
				factoryDescriptor = "()Ljava/util/NavigableSet;";
			}
			case "java/util/Map" -> {
				factory = "emptyMap";
				factoryDescriptor = "()Ljava/util/Map;";
			}
			case "java/util/SortedMap" -> {
				factory = "emptySortedMap";
				factoryDescriptor = "()Ljava/util/SortedMap;";
			}
			case "java/util/NavigableMap" -> {
				factory = "emptyNavigableMap";
				factoryDescriptor = "()Ljava/util/NavigableMap;";
			}
			case "java/util/Iterator" -> {
				factory = "emptyIterator";
				factoryDescriptor = "()Ljava/util/Iterator;";
			}
			case "java/util/ListIterator" -> {
				factory = "emptyListIterator";
				factoryDescriptor = "()Ljava/util/ListIterator;";
			}
			case "java/util/Enumeration" -> {
				factory = "emptyEnumeration";
				factoryDescriptor = "()Ljava/util/Enumeration;";
			}
			case "java/util/Queue", "java/util/Deque" -> {
				returnEmptyConstructor(mv, "java/util/ArrayDeque");
				mv.visitInsn(Opcodes.ARETURN);
				return;
			}
			default -> {
				returnEmptyConstructor(mv, name);
				mv.visitInsn(Opcodes.ARETURN);
				return;
			}
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", factory, factoryDescriptor, false);
		mv.visitInsn(Opcodes.ARETURN);
	}

	private static void returnEmptyConstructor(@Nonnull MethodVisitor mv, @Nonnull String owner) {
		mv.visitTypeInsn(Opcodes.NEW, owner);
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "()V", false);
	}

	private static void returnEmptyArray(@Nonnull MethodVisitor mv, @Nonnull Type type) {
		mv.visitInsn(Opcodes.ICONST_0);
		Type elementType = type.getElementType();
		switch (elementType.getSort()) {
			case Type.BOOLEAN -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
			case Type.CHAR -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
			case Type.BYTE -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
			case Type.SHORT -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
			case Type.INT -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			case Type.FLOAT -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
			case Type.LONG -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
			case Type.DOUBLE -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
			default -> mv.visitTypeInsn(Opcodes.ANEWARRAY, elementType.getInternalName());
		}
		mv.visitInsn(Opcodes.ARETURN);
	}
}
