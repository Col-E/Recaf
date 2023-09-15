package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import me.coley.cafedude.classfile.ConstantPoolConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.util.Types;

import java.util.HashSet;
import java.util.Set;
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
	default Set<String> getReferencedClasses() {
		Set<String> classNames = new HashSet<>();
		Consumer<String> nameHandler = className -> {
			if (className.indexOf(0) == '[')
				className = className.substring(className.lastIndexOf('[') + 1, className.indexOf(';'));
			classNames.add(className);
		};
		Consumer<Type> typeConsumer = t -> {
			if (t.getSort() == Type.ARRAY)
				t = t.getElementType();
			if (!Types.isPrimitive(t))
				nameHandler.accept(t.getInternalName());
		};

		ClassReader reader = getClassReader();
		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];
		for (int i = 1; i < itemCount; i++) {
			int offset = reader.getItem(i);
			if (offset >= 10) {
				int itemTag = reader.readByte(offset - 1);
				if (itemTag == ConstantPoolConstants.CLASS) {
					String className = reader.readUTF8(offset, buffer);
					if (className.isEmpty())
						continue;
					if (className.indexOf(0) == '[')
						className = className.substring(className.lastIndexOf('[') + 1, className.indexOf(';'));
					classNames.add(className);
				} else if (itemTag == ConstantPoolConstants.NAME_TYPE) {
					String desc = reader.readUTF8(offset + 2, buffer);
					if (desc.isEmpty())
						continue;
					if (desc.charAt(0) == '(') {
						Type methodType = Type.getMethodType(desc);
						for (Type argumentType : methodType.getArgumentTypes())
							typeConsumer.accept(argumentType);
						Type returnType = methodType.getReturnType();
						typeConsumer.accept(returnType);
					} else {
						Type type = Type.getType(desc);
						typeConsumer.accept(type);
					}
				}
			}
		}
		return classNames;
	}

	/**
	 * @return Set of all string constants listed in the constant pool.
	 */
	@Nonnull
	default Set<String> getStringConstants() {
		Set<String> classNames = new HashSet<>();
		ClassReader reader = getClassReader();
		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];
		for (int i = 1; i < itemCount; i++) {
			int offset = reader.getItem(i);
			if (offset >= 10) {
				int itemTag = reader.readByte(offset - 1);
				if (itemTag == ConstantPoolConstants.STRING) {
					String string = reader.readUTF8(offset, buffer);
					classNames.add(string);
				}
			}
		}
		return classNames;
	}

	@Override
	default void acceptIfJvmClass(Consumer<JvmClassInfo> action) {
		action.accept(this);
	}

	@Override
	default void acceptIfAndroidClass(Consumer<AndroidClassInfo> action) {
		// no-op
	}

	@Override
	default boolean testIfJvmClass(Predicate<JvmClassInfo> predicate) {
		return predicate.test(this);
	}

	@Override
	default boolean testIfAndroidClass(Predicate<AndroidClassInfo> predicate) {
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
