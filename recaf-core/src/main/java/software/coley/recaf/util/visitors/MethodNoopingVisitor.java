package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A visitor that replaces method bodies with no-op implementations.
 *
 * @author Matt Coley
 */
public class MethodNoopingVisitor extends ClassVisitor {
	private final MemberPredicate predicate;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param predicate
	 * 		Predicate to match which methods will be noop'd, or {@code null} to noop all methods.
	 */
	public MethodNoopingVisitor(@Nullable ClassVisitor cv, @Nullable MemberPredicate predicate) {
		super(RecafConstants.getAsmVersion(), cv);

		this.predicate = predicate;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

		// Skip if already no code.
		if (AccessFlag.isAbstract(access) || AccessFlag.isNative(access))
			return mv;

		// Only noop matched methods.
		if (predicate == null || predicate.matchMethod(access, name, descriptor, signature, exceptions))
			return new MethodNoopingVisitor.NoopingMethodVisitor(mv, descriptor);

		return mv;
	}

	/**
	 * Method visitor that replaces the contents with a no-op return.
	 */
	public static class NoopingMethodVisitor extends MethodVisitor implements Opcodes {
		private static final int MAX_STACK = 2;
		private static final Map<String, Consumer<MethodVisitor>> OBJECT_DEFAULTS = new HashMap<>();
		private final Type type;

		public NoopingMethodVisitor(@Nullable MethodVisitor mv, @Nonnull String desc) {
			super(RecafConstants.getAsmVersion(), mv);
			type = Type.getMethodType(desc);
		}

		@Override
		public void visitEnd() {
			// Directly pass to the delegate so these do not get no-op'd
			MethodVisitor mv = getDelegate();
			if (mv == null) return;
			Type returnType = type.getReturnType();
			int sort = returnType.getSort();
			switch (sort) {
				case Type.VOID -> {
					// return;
					mv.visitInsn(RETURN);
				}
				case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
					// return false, '\0', 0
					mv.visitInsn(ICONST_0);
					mv.visitInsn(IRETURN);
				}
				case Type.FLOAT -> {
					// return 0.0f
					mv.visitInsn(FCONST_0);
					mv.visitInsn(FRETURN);
				}
				case Type.LONG -> {
					// return 0
					mv.visitInsn(LCONST_0);
					mv.visitInsn(LRETURN);
				}
				case Type.DOUBLE -> {
					// return 0.0
					mv.visitInsn(DCONST_0);
					mv.visitInsn(DRETURN);
				}
				case Type.ARRAY -> {
					// return new T[0]
					mv.visitInsn(ICONST_0);
					Type elementType = returnType.getElementType();
					int elementSort = elementType.getSort();
					switch (elementSort) {
						case Type.BOOLEAN -> mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
						case Type.CHAR -> mv.visitIntInsn(NEWARRAY, T_CHAR);
						case Type.BYTE -> mv.visitIntInsn(NEWARRAY, T_BYTE);
						case Type.SHORT -> mv.visitIntInsn(NEWARRAY, T_SHORT);
						case Type.INT -> mv.visitIntInsn(NEWARRAY, T_INT);
						case Type.FLOAT -> mv.visitIntInsn(NEWARRAY, T_FLOAT);
						case Type.LONG -> mv.visitIntInsn(NEWARRAY, T_LONG);
						case Type.DOUBLE -> mv.visitIntInsn(NEWARRAY, T_DOUBLE);
						default -> mv.visitTypeInsn(ANEWARRAY, elementType.getInternalName());
					}
					mv.visitInsn(ARETURN);
				}
				default -> {
					// See if there's a specific kind of default value better than null.
					String className = returnType.getInternalName();
					Consumer<MethodVisitor> defaultValueProvider = OBJECT_DEFAULTS.get(className);
					if (defaultValueProvider != null) {
						defaultValueProvider.accept(mv);
					} else {
						mv.visitInsn(ACONST_NULL);
					}
					mv.visitInsn(ARETURN);
				}
			}
		}

		@Override
		public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
			// skip
		}

		@Override
		public void visitInsn(int opcode) {
			// skip
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			// skip
		}

		@Override
		public void visitVarInsn(int opcode, int varIndex) {
			// skip
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			// skip
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			// skip
		}

		@SuppressWarnings("all")
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
			// skip
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			// skip
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			// skip
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			// skip
		}

		@Override
		public void visitLabel(Label label) {
			// skip
		}

		@Override
		public void visitLdcInsn(Object value) {
			// skip
		}

		@Override
		public void visitIincInsn(int varIndex, int increment) {
			// skip
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			// skip
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			// skip
		}

		@Override
		public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
			// skip
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			// skip
			return null;
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			// skip
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			// skip
			return null;
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
			// skip
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			// skip
			return null;
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			// skip
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			// Stack ---> 2 (max size of value pushed to return)
			// Locals --> Match parameter sizes + 1 for 'this'
			super.visitMaxs(MAX_STACK, type.getArgumentsAndReturnSizes() + 1);
		}

		private static void register(@Nonnull String name, @Nonnull Consumer<MethodVisitor> consumer) {
			OBJECT_DEFAULTS.put(name, consumer);
		}

		private static void registerEmptyCollection(@Nonnull String name) {
			String simpleName = StringUtil.shortenPath(name);
			register(name, mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "empty" + simpleName, "()L" + name + ";", false));
		}

		private static void registerDefaultConstructor(@Nonnull String name) {
			register(name, mv -> {
				mv.visitTypeInsn(NEW, name);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
			});
		}

		static {
			registerEmptyCollection("java/util/List");
			registerEmptyCollection("java/util/Set");
			registerEmptyCollection("java/util/SortedSet");
			registerEmptyCollection("java/util/NavigableSet");
			registerEmptyCollection("java/util/Map");
			registerEmptyCollection("java/util/SortedMap");
			registerEmptyCollection("java/util/NavigableMap");
			registerEmptyCollection("java/util/Iterator");
			registerEmptyCollection("java/util/ListIterator");
			registerEmptyCollection("java/util/Enumeration");
			registerDefaultConstructor("java/util/ArrayList");
			registerDefaultConstructor("java/util/ArrayDeque");
			registerDefaultConstructor("java/util/HashMap");
			registerDefaultConstructor("java/util/HashSet");
			registerDefaultConstructor("java/util/Hashtable");
			registerDefaultConstructor("java/util/IdentityHashMap");
			registerDefaultConstructor("java/util/LinkedHashMap");
			registerDefaultConstructor("java/util/LinkedHashSet");
			registerDefaultConstructor("java/util/LinkedHashSet");
			registerDefaultConstructor("java/util/LinkedList");
			registerDefaultConstructor("java/util/PriorityQueue");
			registerDefaultConstructor("java/util/Properties");
			registerDefaultConstructor("java/util/Random");
			registerDefaultConstructor("java/util/Stack");
			registerDefaultConstructor("java/util/TreeMap");
			registerDefaultConstructor("java/util/TreeSet");
			registerDefaultConstructor("java/util/Vector");
			registerDefaultConstructor("java/util/WeakHashMap");
			registerDefaultConstructor("java/util/concurrent/ArrayBlockingQueue");
			registerDefaultConstructor("java/util/concurrent/ConcurrentHashMap");
			registerDefaultConstructor("java/util/concurrent/ConcurrentLinkedDeque");
			registerDefaultConstructor("java/util/concurrent/ConcurrentLinkedQueue");
			registerDefaultConstructor("java/util/concurrent/ConcurrentSkipListMap");
			registerDefaultConstructor("java/util/concurrent/ConcurrentSkipListSet");
			registerDefaultConstructor("java/util/concurrent/CopyOnWriteArrayList");
			registerDefaultConstructor("java/util/concurrent/CopyOnWriteArraySet");
			registerDefaultConstructor("java/util/concurrent/DelayQueue");
			registerDefaultConstructor("java/util/concurrent/LinkedBlockingDeque");
			registerDefaultConstructor("java/util/concurrent/LinkedBlockingQueue");
			registerDefaultConstructor("java/util/concurrent/LinkedTransferQueue");
			registerDefaultConstructor("java/util/concurrent/PriorityBlockingQueue");
			registerDefaultConstructor("java/util/Date");
			register("java/util/Optional", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false));
			register("java/util/OptionalDouble", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/OptionalDouble", "empty", "()Ljava/util/OptionalDouble;", false));
			register("java/util/OptionalInt", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/OptionalInt", "empty", "()Ljava/util/OptionalInt;", false));
			register("java/util/OptionalLong", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/OptionalLong", "empty", "()Ljava/util/OptionalLong;", false));
			register("java/util/UUID", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/UUID", "randomUUID", "()Ljava/util/UUID;", false));
			register("java/util/stream/Stream", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/util/stream/Stream", "empty", "()Ljava/util/stream/Stream;", false));
			register("java/time/Instant", mv -> mv.visitMethodInsn(INVOKESTATIC, "java/time/Instant", "now", "()Ljava/time/Instant;", false));
			register("java/util/Collection", OBJECT_DEFAULTS.get("java/util/List"));
			register("java/util/Queue", OBJECT_DEFAULTS.get("java/util/ArrayDeque"));
			register("java/util/RandomAccess", OBJECT_DEFAULTS.get("java/util/ArrayList"));
			register("java/time/temporal/Temporal", OBJECT_DEFAULTS.get("java/time/Instant"));
			register("java/time/temporal/TemporalAdjuster", OBJECT_DEFAULTS.get("java/time/Instant"));
		}
	}
}
