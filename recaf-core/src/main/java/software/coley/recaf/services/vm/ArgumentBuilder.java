package software.coley.recaf.services.vm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.member.JavaMethod;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.mirror.type.JavaClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.ObjectValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Types;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for constructing arrays of {@link Argument} values.
 *
 * @author Matt Coley
 */
public class ArgumentBuilder {
	private static final Argument[] ARG_ARRAY_REF = new Argument[0];
	private final List<Argument> arguments = new ArrayList<>();
	private final MethodArgData argData;
	private final VirtualMachine vm;

	private ArgumentBuilder(@Nonnull VirtualMachine vm, @Nonnull MethodArgData argData) {
		this.argData = argData;
		this.vm = vm;
	}

	/**
	 * Adds a {@code null} as the next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder addNull() {
		return add(vm.getMemoryManager().nullValue());
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nullable ObjectValue value) {
		if (value == null)
			return addNull();
		arguments.add(Argument.reference(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nullable String value) {
		return value == null ? addNull() : add(vm.getOperations().newUtf8(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull boolean[] value) {
		return add(vm.getOperations().toVMBooleans(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull byte[] value) {
		return add(vm.getOperations().toVMBytes(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull short[] value) {
		return add(vm.getOperations().toVMShorts(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull int[] value) {
		return add(vm.getOperations().toVMInts(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull float[] value) {
		return add(vm.getOperations().toVMFloats(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull double[] value) {
		return add(vm.getOperations().toVMDoubles(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull long[] value) {
		return add(vm.getOperations().toVMLongs(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull char[] value) {
		return add(vm.getOperations().toVMChars(value));
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull String[] value) {
		VMOperations ops = vm.getOperations();
		ArrayValue array = ops.allocateArray(vm.getSymbols().java_lang_String(), value.length);
		for (int i = 0; i < value.length; i++) {
			InstanceValue vmString = ops.newUtf8(value[i]);
			ops.arrayStoreReference(array, i, vmString);
		}
		return add(array);
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull ObjectValue[] value) {
		return add(vm.getOperations().toVMReferences(value));
	}

	/**
	 * @param componentType
	 * 		Array component type.
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull JavaClass componentType, @Nonnull ObjectValue[] value) {
		VMOperations ops = vm.getOperations();
		ArrayValue array = ops.allocateArray(componentType, value.length);
		for (int i = 0; i < value.length; i++) {
			ops.arrayStoreReference(array, i, value[i]);
		}
		return add(array);
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(@Nonnull ArrayValue value) {
		arguments.add(Argument.reference(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(int value) {
		arguments.add(Argument.int32(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(long value) {
		arguments.add(Argument.int64(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(float value) {
		arguments.add(Argument.float32(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to add as next argument.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public ArgumentBuilder add(double value) {
		arguments.add(Argument.float64(value));
		return this;
	}

	/**
	 * @return Generated arguments.
	 *
	 * @throws IllegalArgumentException
	 * 		When there is a mismatch in the number of arguments,
	 * 		or the argument value types do not match what is declared by the method.
	 */
	@Nonnull
	public Argument[] build() throws IllegalArgumentException {
		// Validate number of arguments
		Type[] argTypes = argData.args;
		int virtualCountOffset = argData.isStatic ? 0 : 1;
		int expectedCount = virtualCountOffset + argTypes.length;
		int actualCount = arguments.size();
		if (expectedCount != actualCount)
			throw new IllegalArgumentException("Expected " + expectedCount + " args, got " + actualCount);

		// Validate type of arguments
		for (int i = virtualCountOffset; i < argTypes.length; i++) {
			Type argType = argTypes[i];
			Argument argument = arguments.get(i);
			int expectedSort = Types.getNormalizedSort(argType.getSort());
			int actualSort = Types.getNormalizedSort(argument.getType().getSort());
			if (expectedSort != actualSort)
				throw new IllegalArgumentException("Misatched argument types at index " + i);
		}

		return arguments.toArray(ARG_ARRAY_REF);
	}

	/**
	 * @param vm
	 * 		VM to create values within.
	 *
	 * @return Initial builder step.
	 */
	@Nonnull
	public static BuilderStage0 withinVM(@Nonnull VirtualMachine vm) {
		return new BuilderStage0(vm);
	}

	/**
	 * Initial stage for configuring classloader options within the VM.
	 */
	public static class BuilderStage0 {
		private final VirtualMachine vm;

		private BuilderStage0(@Nonnull VirtualMachine vm) {
			this.vm = vm;
		}

		/**
		 * Forces lookup in bootstrap class path.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public BuilderStage1 withoutClassloader() {
			return new BuilderStage1(vm);
		}

		/**
		 * Enables lookup in the given classloader.
		 *
		 * @param classloaderRef
		 * 		Reference in the VM pointing to a {@link ClassLoader} instance.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public BuilderStage1 withClassloader(@Nonnull ObjectValue classloaderRef) {
			return new BuilderStage1(vm, classloaderRef);
		}

		/**
		 * Enables lookup in the same classloader used to define the given class.
		 *
		 * @param classWithinLoader
		 * 		Class in VM.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public BuilderStage1 withSameClassloaderAs(@Nonnull JavaClass classWithinLoader) {
			return new BuilderStage1(vm, classWithinLoader);
		}
	}

	/**
	 * Second stage for configuring initialization behavior and which method is to be invoked.
	 */
	public static class BuilderStage1 {
		private final VirtualMachine vm;
		private final JavaClass classWithinLoader;
		private final ObjectValue classloaderRef;
		private boolean doInitClasses = true;

		private BuilderStage1(@Nonnull VirtualMachine vm) {
			this(vm, null, null);
		}

		private BuilderStage1(@Nonnull VirtualMachine vm, @Nonnull JavaClass classWithinLoader) {
			this(vm, classWithinLoader, null);
		}

		private BuilderStage1(@Nonnull VirtualMachine vm, @Nonnull ObjectValue classloaderRef) {
			this(vm, null, classloaderRef);
		}

		private BuilderStage1(@Nonnull VirtualMachine vm, @Nullable JavaClass classWithinLoader, @Nullable ObjectValue classloaderRef) {
			this.vm = vm;
			this.classWithinLoader = classWithinLoader;
			this.classloaderRef = classloaderRef;
		}

		/**
		 * Disables class initialization when loading classes for the first time.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public BuilderStage1 withoutInitializingTargetClasses() {
			doInitClasses = false;
			return this;
		}

		/**
		 * @param declaringClass
		 * 		Class that declares the method.
		 * @param method
		 * 		Method to pull argument data from.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ArgumentBuilder forMethod(@Nonnull ClassInfo declaringClass, @Nonnull MethodMember method) {
			Type[] argumentTypes = Type.getMethodType(method.getDescriptor()).getArgumentTypes();
			boolean isStatic = method.hasStaticModifier();
			return newBuilder(vm, new MethodArgData(declaringClass.getName(), argumentTypes, isStatic));
		}

		/**
		 * @param method
		 * 		Method to pull argument data from.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ArgumentBuilder forMethod(@Nonnull JavaMethod method) {
			String declaringClass = method.getOwner().getInternalName();
			Type[] argumentTypes = Type.getMethodType(method.getDesc()).getArgumentTypes();
			boolean isStatic = AccessFlag.isStatic(method.getModifiers());
			return newBuilder(vm, new MethodArgData(declaringClass, argumentTypes, isStatic));
		}

		/**
		 * @param declaringClass
		 * 		Name of declaring class.
		 * @param methodDesc
		 * 		Method descriptor to pull argument types from.
		 * @param isStatic
		 * 		Flag for if the method is static.
		 *
		 * @return Builder.
		 */
		@Nonnull
		public ArgumentBuilder forMethod(@Nonnull VirtualMachine vm, @Nonnull String declaringClass, @Nonnull String methodDesc, boolean isStatic) {
			Type[] argumentTypes = Type.getMethodType(methodDesc).getArgumentTypes();
			return newBuilder(vm, new MethodArgData(declaringClass, argumentTypes, isStatic));
		}

		@Nonnull
		private ArgumentBuilder newBuilder(@Nonnull VirtualMachine vm, @Nonnull MethodArgData argData) {
			ArgumentBuilder builder = new ArgumentBuilder(vm, argData);
			if (!argData.isStatic) {
				// Add 'this'
				String declaringClassName = argData.declaringClass;
				InstanceClass declaringClass;
				if (classloaderRef != null)
					declaringClass = (InstanceClass) vm.getOperations()
							.findClass(classloaderRef, declaringClassName, doInitClasses);
				else if (classWithinLoader != null)
					declaringClass = (InstanceClass) vm.getOperations()
							.findClass(classWithinLoader, declaringClassName, doInitClasses);
				else
					declaringClass = (InstanceClass) vm.findBootstrapClass(declaringClassName, doInitClasses);

				// Validate class was found
				if (declaringClass == null)
					throw new IllegalStateException("Cannot find class: " + declaringClassName);

				// Add the argument
				InstanceValue thisArg = vm.getMemoryManager().newInstance(declaringClass);
				builder.add(thisArg);

				// TODO: Call constructor if possible to ensure the class is fully initialized
			}
			return builder;
		}
	}

	private record MethodArgData(@Nonnull String declaringClass, @Nonnull Type[] args, boolean isStatic) {
	}
}
