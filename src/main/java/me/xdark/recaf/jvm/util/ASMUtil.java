package me.xdark.recaf.jvm.util;

import me.xdark.recaf.jvm.VMException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.Iterator;

/**
 * @author CertainLach
 */
public final class ASMUtil {
	private ASMUtil() { }

	public static void injectCodeAtStart(InsnList originalCode, InsnList injectedCode) {
		if (originalCode.size() == 0) {
			originalCode.add(injectedCode);
		} else {
			originalCode.insertBefore(originalCode.getFirst(), injectedCode);
		}
	}

	public static void injectCodeAfterSuperclassInit(InsnList originalCode, InsnList injectedCode) throws VMException {
		Iterator<AbstractInsnNode> it = originalCode.iterator();
		AbstractInsnNode injectionPoint = null;
		int instructions = 0;
		while (it.hasNext()) {
			instructions++;
			AbstractInsnNode cur = it.next();
			if (cur instanceof MethodInsnNode) {
				MethodInsnNode methodInsn = (MethodInsnNode) cur;
				if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL && methodInsn.name.equals("<init>")) {
					injectionPoint = methodInsn;
					break;
				}
			}
		}
		if (injectionPoint == null)
			throw new VMException("Injection point isn't found! It is a constructor at all? Instructions checked " + instructions);
		originalCode.insert(injectionPoint, injectedCode);
	}

	public static boolean modifierIsStatic(int flags) {
		return (flags & Modifier.STATIC) != 0;
	}

	public static boolean isStaticMethod(MethodNode methodNode) {
		return isStaticConstructor(methodNode) || modifierIsStatic(methodNode.access);
	}

	public static boolean isConstructor(MethodNode methodNode) {
		return isStaticConstructor(methodNode) || isNormalConstructor(methodNode);
	}

	public static boolean isStaticConstructor(MethodNode methodNode) {
		return methodNode.name.equals("<clinit>");
	}

	public static boolean isNormalConstructor(MethodNode methodNode) {
		return methodNode.name.equals("<init>");
	}

	public static MethodNode getOrCreateClinit(ClassNode classNode) {
		for (MethodNode method : classNode.methods) {
			if (isStaticConstructor(method)) {
				return method;
			}
		}
		MethodNode methodNode = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, new String[]{});
		methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
		return methodNode;
	}
}
