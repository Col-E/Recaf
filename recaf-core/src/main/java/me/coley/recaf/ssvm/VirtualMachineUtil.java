package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.asm.DelegatingInsnNode;
import dev.xdark.ssvm.asm.Modifier;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.Value;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

/**
 * Utils for SSVM.
 *
 * @author xDark
 */
public class VirtualMachineUtil {
	/**
	 * Deny all constructions.
	 */
	private VirtualMachineUtil() {
	}

	/**
	 * @param vm
	 * 		VM instance.
	 *
	 * @return version of JDK the VM runs on.
	 */
	public static int getVersion(VirtualMachine vm) {
		return vm.getSymbols().java_lang_Object().getNode().version - 44;
	}

	/**
	 * SSVM will patch some classes for better performance or due to its internals.
	 * This method may be used to undo all changes.
	 *
	 * @param node
	 * 		Class node to patch.
	 */
	public static void restoreClass(ClassNode node) {
		node.access = Modifier.eraseClass(node.access);
		for (FieldNode field : node.fields) {
			restoreField(field);
		}
		for (MethodNode mn : node.methods) {
			restoreMethod(mn);
		}
	}

	/**
	 * SSVM will patch some fields for better performance or due to its internals.
	 * This method may be used to undo all changes.
	 *
	 * @param node
	 * 		Field node to patch.
	 */
	public static void restoreField(FieldNode node) {
		node.access = Modifier.eraseField(node.access);
	}

	/**
	 * SSVM will patch some methods for better performance or due to its internals.
	 * This method may be used to undo all changes.
	 *
	 * @param node
	 * 		Method node to patch.
	 */
	public static void restoreMethod(MethodNode node) {
		node.access = Modifier.eraseMethod(node.access);
		ListIterator<AbstractInsnNode> iterator = node.instructions.iterator();
		while (iterator.hasNext()) {
			AbstractInsnNode insn = iterator.next();
			if (insn instanceof DelegatingInsnNode) {
				iterator.set(((DelegatingInsnNode<?>) insn).getDelegate());
			}
		}
	}

	/**
	 * @param vm
	 * 		VM instance.
	 *
	 * @return System class loader.
	 */
	public static InstanceValue getSystemClassLoader(VirtualMachine vm) {
		return (InstanceValue) vm.getHelper().invokeStatic(vm.getSymbols().java_lang_ClassLoader(), "getSystemClassLoader", "()Ljava/lang/ClassLoader;", new Value[0], new Value[0]).getResult();
	}

	/**
	 * Adds url to system classpath.
	 *
	 * @param vm
	 * 		VM instance.
	 * @param path
	 * 		Path to add.
	 */
	public static void addUrl(VirtualMachine vm, String path) {
		VMHelper helper = vm.getHelper();
		InstanceJavaClass fileClass = (InstanceJavaClass) vm.findBootstrapClass("java/io/File", true);
		Value file = vm.getMemoryManager().newInstance(fileClass);
		helper.invokeExact(fileClass, "<init>", "(Ljava/lang/String;)V", new Value[0], new Value[]{file, helper.newUtf8(path)});
		Value uri = helper.invokeVirtual("toURI", "()Ljava/net/URI;", new Value[0], new Value[]{file}).getResult();
		Value url = helper.invokeVirtual("toURL", "()Ljava/net/URL;", new Value[0], new Value[]{uri}).getResult();
		InstanceValue scl = getSystemClassLoader(vm);
		int version = getVersion(vm);
		Value addUrlTo;
		if (version < 9) {
			addUrlTo = scl;
		} else {
			addUrlTo = scl.getValue("ucp", "Ljdk/internal/loader/URLClassPath;");
		}
		helper.invokeVirtual("addURL", "(Ljava/net/URL;)V", new Value[0], new Value[]{addUrlTo, url});
	}

	/**
	 * @param mode
	 * 		Access mode for {@link FileDescriptorManager}.
	 *
	 * @return Name representation of mode.
	 */
	public static String describeFileMode(int mode) {
		switch (mode) {
			case FileDescriptorManager.READ:
				return "READ";
			case FileDescriptorManager.WRITE:
				return "WRITE";
			case FileDescriptorManager.APPEND:
				return "APPEND";
			default:
				return "?";
		}
	}
}
