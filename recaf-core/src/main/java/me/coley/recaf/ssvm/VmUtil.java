package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.Locals;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.mirror.JavaClass;
import dev.xdark.ssvm.mirror.JavaMethod;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.ObjectValue;
import dev.xdark.ssvm.value.Value;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recaf VM helper.
 *
 * @author xDark
 */
public final class VmUtil {
	private final VirtualMachine vm;

	/**
	 * @param vm
	 * 		VM instance.
	 */
	private VmUtil(VirtualMachine vm) {
		this.vm = vm;
	}

	/**
	 * @param vm
	 * 		VM instance.
	 *
	 * @return VM helper.
	 */
	public static VmUtil create(VirtualMachine vm) {
		return new VmUtil(vm);
	}

	/**
	 * Adds url to classpath of a system class loader.
	 *
	 * @param urlPath
	 * 		Path to a file.
	 */
	public void addUrl(String urlPath) {
		VirtualMachine vm = this.vm;
		InstanceValue file = newInstance((InstanceJavaClass) vm.findBootstrapClass("java/io/File"), "(Ljava/lang/String;)V", vm.getHelper().newUtf8(urlPath));
		InstanceValue asUri = (InstanceValue) invokeVirtual("toURI", "()Ljava/net/URI;", file).getResult();
		InstanceValue asUrl = (InstanceValue) invokeVirtual("toURL", "()Ljava/net/URL;", asUri).getResult();
		InstanceJavaClass urlClassPath = (InstanceJavaClass) vm.findBootstrapClass("jdk/internal/loader/URLClassPath");
		if (urlClassPath == null) {
			urlClassPath = (InstanceJavaClass) vm.findBootstrapClass("sun/misc/URLClassPath");
		}
		InstanceValue classLoader = (InstanceValue) getSystemClassLoader();
		InstanceJavaClass klass = classLoader.getJavaClass();
		ObjectValue ucp = vm.getOperations().getField(classLoader, klass, "ucp", urlClassPath.getDescriptor());
		invokeVirtual("addURL", "(Ljava/net/URL;)V", ucp, asUrl);
	}

	/**
	 * @return System class loader.
	 */
	public ObjectValue getSystemClassLoader() {
		VirtualMachine vm = this.vm;
		JavaMethod getSystemClassLoader = vm.getLinkResolver().resolveStaticMethod(vm.getSymbols().java_lang_ClassLoader(), "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
		return (ObjectValue) vm.getHelper().invoke(getSystemClassLoader, vm.getThreadStorage().newLocals(getSystemClassLoader)).getResult();
	}

	/**
	 * Makes exact call.
	 *
	 * @param klass
	 * 		Method owner.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method desc.
	 * @param args
	 * 		Method arguments.
	 *
	 * @return Invocation context.
	 */
	public ExecutionContext invokeExact(InstanceJavaClass klass, String name, String desc, Value... args) {
		VirtualMachine vm = this.vm;
		VMHelper helper = vm.getHelper();
		JavaMethod target = vm.getLinkResolver().resolveSpecialMethod(klass, name, desc);
		Locals locals = vm.getThreadStorage().newLocals(target);
		locals.copyFrom(args, 0, 0, args.length);
		return helper.invoke(target, locals);
	}

	/**
	 * Makes virtual call.
	 *
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method desc.
	 * @param args
	 * 		Method arguments.
	 *
	 * @return Invocation context.
	 */
	public ExecutionContext invokeVirtual(String name, String desc, Value... args) {
		VirtualMachine vm = this.vm;
		VMHelper helper = vm.getHelper();
		ObjectValue value = helper.checkNotNull(args[0]);
		JavaMethod target = vm.getLinkResolver().resolveVirtualMethod(value, name, desc);
		Locals locals = vm.getThreadStorage().newLocals(target);
		locals.copyFrom(args, 0, 0, args.length);
		return helper.invoke(target, locals);
	}

	/**
	 * Makes static call.
	 *
	 * @param klass
	 * 		Method owner.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method desc.
	 * @param args
	 * 		Method arguments.
	 *
	 * @return Invocation context.
	 */
	public ExecutionContext invokeStatic(InstanceJavaClass klass, String name, String desc, Value... args) {
		VirtualMachine vm = this.vm;
		JavaMethod target = vm.getLinkResolver().resolveStaticMethod(klass, name, desc);
		Locals locals = vm.getThreadStorage().newLocals(target);
		locals.copyFrom(args, 0, 0, args.length);
		return vm.getHelper().invoke(target, locals);
	}

	/**
	 * Allocates new instance.
	 *
	 * @param klass
	 * 		Class of the instance.
	 * @param desc
	 * 		Init descriptor.
	 * @param args
	 * 		Method arguments.
	 *
	 * @return Allocated instance.
	 */
	public InstanceValue newInstance(InstanceJavaClass klass, String desc, Value... args) {
		klass.initialize();
		VirtualMachine vm = this.vm;
		JavaMethod init = vm.getLinkResolver().resolveSpecialMethod(klass, "<init>", desc);
		InstanceValue value = vm.getOperations().allocateInstance(klass);
		Locals locals = vm.getThreadStorage().newLocals(init);
		locals.set(0, value);
		if (args.length != 0) {
			locals.copyFrom(args, 0, 1, args.length);
		}
		vm.getHelper().invoke(init, locals);
		return value;
	}

	/**
	 * Converts throwable to a string value
	 *
	 * @param throwable
	 * 		Throwable to convert.
	 *
	 * @return Throwable as string.
	 */
	public String throwableToString(InstanceValue throwable) {
		Objects.requireNonNull(throwable, "throwable");
		VMHelper helper = vm.getHelper();
		try {
			InstanceValue stringWriter = newInstance((InstanceJavaClass) vm.findBootstrapClass("java/io/StringWriter"), "()V");
			InstanceValue printWriter = newInstance((InstanceJavaClass) vm.findBootstrapClass("java/io/PrintWriter"), "(Ljava/io/Writer;)V", stringWriter);
			invokeVirtual("printStackTrace", "(Ljava/io/PrintWriter;)V", throwable, printWriter);
			Value throwableAsString = invokeVirtual("toString", "()Ljava/lang/String;", stringWriter).getResult();
			return helper.readUtf8(throwableAsString);
		} catch (VMException ignored) {
		}
		StringWriter writer = new StringWriter();
		helper.toJavaException(throwable).printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return String representation.
	 */
	public String toString(Value value) {
		String valueString = null;
		if (value.isNull()) {
			return "null";
		} else if (value instanceof InstanceValue) {
			InstanceValue instance = (InstanceValue) value;
			if (instance.getJavaClass().getInternalName().equals("java/lang/String")) {
				valueString = vm.getHelper().readUtf8(value);
			}
		} else if (value instanceof ArrayValue) {
			ArrayValue array = (ArrayValue) value;
			JavaClass cls = array.getJavaClass();
			Type arrayType = Type.getType(cls.getDescriptor());
			int length = array.getLength();
			List<String> parts = new ArrayList<>();
			Type element = arrayType.getElementType();
			switch (element.getSort()) {
				case Type.BOOLEAN:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getBoolean(i)));
					break;
				case Type.CHAR:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getChar(i)));
					break;
				case Type.BYTE:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getByte(i)));
					break;
				case Type.SHORT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getShort(i)));
					break;
				case Type.INT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getInt(i)));
					break;
				case Type.FLOAT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getFloat(i)));
					break;
				case Type.LONG:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getLong(i)));
					break;
				case Type.DOUBLE:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getDouble(i)));
					break;
				case Type.OBJECT:
					for (int i = 0; i < length; i++)
						parts.add(toString(array.getValue(i)));
					break;
				default:
					throw new IllegalStateException("Unsupported element type: " + element);
			}
			valueString = "[" + String.join(", ", parts) + "]";
		}
		if (valueString == null)
			valueString = value.toString();
		return valueString;
	}
}
