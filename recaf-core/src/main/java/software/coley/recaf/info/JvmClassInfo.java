package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import software.coley.cafedude.classfile.ConstantPoolConstants;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.builtin.ReferencedClassesProperty;
import software.coley.recaf.info.properties.builtin.StringDefinitionsProperty;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.visitors.TypeVisitor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Outline of a JVM class.
 *
 * @author Matt Coley
 */
public interface JvmClassInfo extends ClassInfo {
	/**
	 * Denotes the base version offset.
	 * <ul>
	 *     <li>For version 1 of you would use {@code BASE_VERSION + 1}.</li>
	 *     <li>For version 2 of you would use {@code BASE_VERSION + 2}.</li>
	 *     <li>...</li>
	 *     <li>For version N of you would use {@code BASE_VERSION + N}.</li>
	 * </ul>
	 */
	int BASE_VERSION = 44;

	/**
	 * @return New builder wrapping this class information.
	 */
	@Nonnull
	default JvmClassInfoBuilder toJvmClassBuilder() {
		return new JvmClassInfoBuilder(this);
	}

	/**
	 * @return Java class file version.
	 */
	int getVersion();

	/**
	 * @return Bytecode of class.
	 */
	@Nonnull
	byte[] getBytecode();

	/**
	 * @return Class reader of {@link #getBytecode()}.
	 */
	@Nonnull
	ClassReader getClassReader();

	/**
	 * @return Set of all classes referenced in the constant pool.
	 */
	@Nonnull
	default NavigableSet<String> getReferencedClasses() {
		NavigableSet<String> classes = ReferencedClassesProperty.get(this);
		if (classes != null)
			return classes;

		Set<String> classNames = new HashSet<>();
		ClassReader reader = getClassReader();

		// Iterate over pool entries. Supe fast way to discover most of the referenced types.
		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];
		for (int i = 1; i < itemCount; i++) {
			int offset = reader.getItem(i);
			if (offset >= 10) {
				try {
					int itemTag = reader.readByte(offset - 1);
					if (itemTag == ConstantPoolConstants.CLASS) {
						String className = reader.readUTF8(offset, buffer);
						if (className.isEmpty())
							continue;
						addName(className, classNames);
					} else if (itemTag == ConstantPoolConstants.NAME_TYPE) {
						String desc = reader.readUTF8(offset + 2, buffer);
						if (desc.isEmpty())
							continue;
						if (desc.charAt(0) == '(') {
							addMethodType(Type.getMethodType(desc), classNames);
						} else {
							Type type = Type.getType(desc);
							addType(type, classNames);
						}
					} else if (itemTag == ConstantPoolConstants.METHOD_TYPE) {
						String methodDesc = reader.readUTF8(offset, buffer);
						if (methodDesc.isEmpty() || methodDesc.charAt(0) != '(')
							continue;
						addMethodType(Type.getMethodType(methodDesc), classNames);
					}
				} catch (Throwable ignored) {
					// Exists only to catch situations where obfuscators put unused junk pool entries
					// with malformed descriptors, which cause ASM's type parser to crash.
				}
			}
		}

		// In some cases like interface classes, there may be UTF8 pool entries outlining method descriptors which
		// are not directly linked in NameType or MethodType pool entries. We need to iterate over fields and methods
		// to get the descriptors in these cases.
		reader.accept(new TypeVisitor(t -> {
			if (t.getSort() == Type.METHOD)
				addMethodType(t, classNames);
			else
				addType(t, classNames);
		}), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);

		ReferencedClassesProperty.set(this, classNames);
		return Objects.requireNonNull(ReferencedClassesProperty.get(this));
	}

	private static void addMethodType(@Nonnull Type methodType, @Nonnull Set<String> classNames) {
		for (Type argumentType : methodType.getArgumentTypes())
			addType(argumentType, classNames);
		Type returnType = methodType.getReturnType();
		addType(returnType, classNames);
	}

	private static void addType(@Nonnull Type type, @Nonnull Set<String> classNames) {
		if (type.getSort() == Type.ARRAY)
			type = type.getElementType();
		if (!Types.isPrimitive(type))
			addName(type.getInternalName(), classNames);
	}

	private static void addName(@Nonnull String className, @Nonnull Set<String> classNames) {
		if (className.isEmpty())
			return;
		if (className.indexOf(0) == '[' || className.charAt(className.length() - 1) == ';')
			addType(Type.getType(className), classNames);
		else if (className.indexOf(0) == '(')
			addMethodType(Type.getMethodType(className), classNames);
		else
			classNames.add(className);
	}

	/**
	 * @return Set of all string constants listed in the constant pool.
	 */
	@Nonnull
	default Set<String> getStringConstants() {
		SortedSet<String> strings = StringDefinitionsProperty.get(this);
		if (strings != null)
			return strings;

		Set<String> stringSet = new HashSet<>();
		ClassReader reader = getClassReader();
		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];
		for (int i = 1; i < itemCount; i++) {
			int offset = reader.getItem(i);
			if (offset >= 10) {
				int itemTag = reader.readByte(offset - 1);
				if (itemTag == ConstantPoolConstants.STRING) {
					String string = reader.readUTF8(offset, buffer);
					stringSet.add(string);
				}
			}
		}
		StringDefinitionsProperty.set(this, stringSet);
		return stringSet;
	}

	@Override
	default void acceptIfJvmClass(@Nonnull Consumer<JvmClassInfo> action) {
		action.accept(this);
	}

	@Override
	default void acceptIfAndroidClass(@Nonnull Consumer<AndroidClassInfo> action) {
		// no-op
	}

	@Override
	default boolean testIfJvmClass(@Nonnull Predicate<JvmClassInfo> predicate) {
		return predicate.test(this);
	}

	@Override
	default boolean testIfAndroidClass(@Nonnull Predicate<AndroidClassInfo> predicate) {
		return false;
	}

	@Nonnull
	@Override
	default JvmClassInfo asJvmClass() {
		return this;
	}

	@Nonnull
	@Override
	default AndroidClassInfo asAndroidClass() {
		throw new IllegalStateException("JVM class cannot be cast to Android class");
	}

	@Override
	default boolean isJvmClass() {
		return true;
	}

	@Override
	default boolean isAndroidClass() {
		return false;
	}
}
