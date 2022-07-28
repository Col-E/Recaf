package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.asm.DelegatingInsnNode;
import dev.xdark.ssvm.asm.Modifier;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.value.IntValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ListIterator;

/**
 * Utils for SSVM.
 *
 * @author xDark
 */
public class SsvmUtil {
	private final static Logger logger = LoggerFactory.getLogger(SsvmUtil.class);

	/**
	 * Deny all constructions.
	 */
	private SsvmUtil() {
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

	/**
	 * Shutdowns VM.
	 *
	 * @param vm
	 * 		VM instance.
	 * @param code
	 * 		Exit code.
	 */
	public static void shutdown(VirtualMachine vm, int code) {
		InstanceJavaClass cl = (InstanceJavaClass) vm.findBootstrapClass("java/lang/Shutdown", true);
		VmUtil util = VmUtil.create(vm);
		try {
			util.invokeStatic(cl, "shutdown", "()V");
			util.invokeStatic(cl, "beforeHalt", "()V");
			util.invokeStatic(cl, "halt", "(I)V", IntValue.of(code));
		} catch (VMException ex) {
			logger.error("Could not shutdown virtual machine\n{}", util.throwableToString(ex.getOop()));
		}
	}
}
