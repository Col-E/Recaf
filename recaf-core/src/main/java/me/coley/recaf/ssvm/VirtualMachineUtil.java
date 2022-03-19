package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.asm.DelegatingInsnNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
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
		return vm.getSymbols().java_lang_Object.getNode().version - 44;
	}

	/**
	 * SSVM will patch some methods for better performance or
	 * due to its internals.
	 * This method may be used to undo all changes.
	 *
	 * @param node
	 * 		Method node to patch.
	 */
	public static void restoreMethod(MethodNode node) {
		ListIterator<AbstractInsnNode> iterator = node.instructions.iterator();
		while (iterator.hasNext()) {
			AbstractInsnNode insn = iterator.next();
			if (insn instanceof DelegatingInsnNode) {
				iterator.set(((DelegatingInsnNode<?>) insn).getDelegate());
			}
		}
	}

	/**
	 * SSVM will patch some classes for better performance or
	 * due to its internals.
	 * This method may be used to undo all changes.
	 *
	 * @param node
	 * 		Class node to patch.
	 */
	public static void restoreClass(ClassNode node) {
		for (MethodNode mn : node.methods) {
			restoreMethod(mn);
		}
	}
}
