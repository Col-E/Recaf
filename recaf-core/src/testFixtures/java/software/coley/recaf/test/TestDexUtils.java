package software.coley.recaf.test;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.type.Types;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;

import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

/**
 * Various test utils for dex class and method creation.
 *
 * @author Matt Coley
 */
public class TestDexUtils {
	/**
	 * @param className
	 * 		Internal name of the class to create.
	 * @param methods
	 * 		Methods to include in the class definition.
	 *
	 * @return Android class with the specified name and methods.
	 */
	@Nonnull
	public static AndroidClassInfo newAndroidClass(@Nonnull String className,
	                                               @Nonnull MethodMember... methods) {
		return newAndroidClass(className, null, methods);
	}

	/**
	 * @param className
	 * 		Internal name of the class to create.
	 * @param consumer
	 * 		Consumer to modify the class definition before building the class info.
	 * @param methods
	 * 		Methods to include in the class definition.
	 *
	 * @return Android class with the specified name and methods.
	 */
	@Nonnull
	public static AndroidClassInfo newAndroidClass(@Nonnull String className,
	                                               @Nullable Consumer<ClassDefinition> consumer,
	                                               @Nonnull MethodMember... methods) {
		ClassDefinition definition = new ClassDefinition(
				Types.instanceTypeFromInternalName(className),
				Types.OBJECT,
				ACC_PUBLIC);
		for (me.darknet.dex.tree.definitions.MethodMember method : methods)
			definition.putMethod(method);
		if (consumer != null)
			consumer.accept(definition);
		return new AndroidClassInfoBuilder(definition).build();
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param access
	 * 		Method access flags.
	 * @param codeConsumer
	 * 		Consumer to build the method's code.
	 *
	 * @return Built method.
	 */
	@Nonnull
	public static MethodMember newDexMethod(@Nonnull String name,
	                                        @Nonnull String desc,
	                                        int access,
	                                        @Nonnull Consumer<CodeBuilder> codeConsumer) {

		CodeBuilder codeBuilder = new CodeBuilder();
		codeConsumer.accept(codeBuilder);
		Code code = codeBuilder.build();
		return newDexMethod(name, desc, access, code);
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param access
	 * 		Method access flags.
	 * @param code
	 * 		Method's code.
	 *
	 * @return Built method.
	 */
	@Nonnull
	public static MethodMember newDexMethod(@Nonnull String name,
	                                        @Nonnull String desc,
	                                        int access,
	                                        @Nonnull Code code) {
		MethodMember method = new MethodMember(
				name,
				Types.methodTypeFromDescriptor(desc),
				access);
		method.setCode(code);
		return method;
	}
}
