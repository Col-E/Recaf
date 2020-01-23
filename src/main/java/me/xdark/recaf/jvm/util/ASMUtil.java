package me.xdark.recaf.jvm.util;

import me.xdark.recaf.jvm.VMException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import sun.reflect.annotation.AnnotationParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CertainLach
 * @author xxDark
 */
public final class ASMUtil {
	private final List<String> ANNOTATION_METHODS = Arrays.asList(
			"equals(Ljava/lang/Object;)Z",
			"toString()Ljava/lang/String;",
			"hashCode()I",
			"annotationType()Ljava/lang/Class;"
	);

	private ASMUtil() {
	}

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

	/**
	 * Attempts to find a method by it's internal name
	 * e.g. hi()V
	 */
	public MethodNode findMethod(ClassNode node, String vmName) {
		List<MethodNode> methods = node.methods;
		for (int i = 0, j = methods.size(); i < j; i++) {
			MethodNode mn = methods.get(i);
			if (vmName.equals(mn.name + mn.desc)) return mn;
		}
		return null;
	}

	/**
	 * Attempts to find collection of method's by their's name
	 */
	public Collection<MethodNode> findMethodsName(ClassNode node, String name) {
		return node.methods.stream().filter(m -> name.equals(m.name)).collect(Collectors.toSet());
	}

	/**
	 * Attempts to find collection of method's by their's descriptor
	 */
	public Collection<MethodNode> findMethodsDescriptor(ClassNode node, String desc) {
		return node.methods.stream().filter(m -> desc.equals(m.desc)).collect(Collectors.toSet());
	}

	/**
	 * @return a field by it's internal name
	 * e.g. a;Ljava/lang/Object;
	 */
	public FieldNode findField(ClassNode node, String vmName) {
		int i = vmName.indexOf(';');
		String name = vmName.substring(0, i);
		String desc = vmName.substring(i + 1);
		List<FieldNode> fields = node.fields;
		for (int j = 0, k = fields.size(); j < k; j++) {
			FieldNode fn = fields.get(j);
			if (name.equals(fn.name) && desc.equals(fn.desc)) return fn;
		}
		return null;
	}

	/**
	 * Attempts to find collection of fields by their's name
	 */
	public Collection<FieldNode> findFieldsName(ClassNode node, String name) {
		return node.fields.stream().filter(f -> name.equals(f.name)).collect(Collectors.toSet());
	}

	/**
	 * Attempts to find collection of methods by their's descriptor
	 */
	public Collection<FieldNode> findFieldsDescriptor(ClassNode node, String desc) {
		return node.fields.stream().filter(f -> desc.equals(f.desc)).collect(Collectors.toSet());
	}

	/**
	 * @return asm annotation from a list
	 */
	public AnnotationNode findAsmAnnotation(List<AnnotationNode> nodes, String desc) {
		for (int i = 0, j = nodes.size(); i < j; i++) {
			AnnotationNode node = nodes.get(i);
			if (desc.equals(node.desc)) return node;
		}
		return null;
	}

	/**
	 * @return annotation from class node
	 */
	public <T extends Annotation> T readAsmAnnotation(ClassNode node, AnnotationType asmType, Class<T> type, ClassLoader loader) throws ClassNotFoundException {
		AnnotationNode selected = findAsmAnnotation(node.visibleAnnotations, node.invisibleAnnotations, asmType, Type.getDescriptor(type));
		return parseASMAnnotation(loader, type, selected);
	}

	/**
	 * @return annotation from method node
	 */
	public <T extends Annotation> T readAsmAnnotation(MethodNode node, AnnotationType asmType, Class<T> type, ClassLoader loader) throws ClassNotFoundException {
		AnnotationNode selected = findAsmAnnotation(node.visibleAnnotations, node.invisibleAnnotations, asmType, Type.getDescriptor(type));
		return parseASMAnnotation(loader, type, selected);
	}

	/**
	 * @return annotation from field node
	 */
	public <T extends Annotation> T readAsmAnnotation(FieldNode node, AnnotationType asmType, Class<T> type, ClassLoader loader) throws ClassNotFoundException {
		AnnotationNode selected = findAsmAnnotation(node.visibleAnnotations, node.invisibleAnnotations, asmType, Type.getDescriptor(type));
		return parseASMAnnotation(loader, type, selected);
	}

	/**
	 * Removes an annotation from a class
	 */
	public void removeAsmAnnotation(ClassNode node, Class<? extends Annotation> type) {
		removeAsmAnnotation0(type, node.visibleAnnotations, node.invisibleAnnotations);
	}

	/**
	 * Removes an annotation from a method
	 */
	public void removeAsmAnnotation(MethodNode node, Class<? extends Annotation> type) {
		removeAsmAnnotation0(type, node.visibleAnnotations, node.invisibleAnnotations);
	}

	/**
	 * Removes an annotation from a field
	 */
	public void removeAsmAnnotation(FieldNode node, Class<? extends Annotation> type) {
		removeAsmAnnotation0(type, node.visibleAnnotations, node.invisibleAnnotations);
	}

	/**
	 * @return {@code true} if method belongs to annotationn
	 */
	public boolean isAnnotationMethod(Method m) {
		return !ANNOTATION_METHODS.contains(m.getName() + Type.getMethodDescriptor(Type.getReturnType(m), Type.getArgumentTypes(m)));
	}

	/**
	 * Injects default annotation values into the map
	 */
	public void injectAnnotationDefaultValues(Class<? extends Annotation> type, Map<String, Object> map) {
		for (Method m : type.getMethods()) {
			if (!isAnnotationMethod(m)) continue;
			String name = m.getName();
			if (!map.containsKey(name)) {
				map.put(name, m.getDefaultValue());
			}
		}
	}

	private void removeAsmAnnotation0(Class<? extends Annotation> type, List<AnnotationNode> visibleAnnotations, List<AnnotationNode> invisibleAnnotations) {
		Object[] information = findAnnotationLists(visibleAnnotations, invisibleAnnotations, type);
		List<AnnotationNode>[] result = (List<AnnotationNode>[]) information[0];
		AnnotationNode[] nodes = (AnnotationNode[]) information[1];
		if (result[0] != null) result[0].remove(nodes[0]);
		if (result[1] != null) result[1].remove(nodes[1]);
	}

	private <T extends Annotation> T parseASMAnnotation(ClassLoader cl, Class<T> type, AnnotationNode node) throws ClassNotFoundException {
		if (node == null) return null;
		List<Object> values = node.values;
		int j = values.size();
		Map<String, Object> map = new HashMap<String, Object>(j / 2);
		int i = 0;
		while (i < j) {
			String name = (String) values.get(i++);
			Object value = values.get(i++);
			if (value instanceof AnnotationNode) {
				AnnotationNode _node = (AnnotationNode) value;
				map.put(name, parseASMAnnotation(cl, (Class<? extends Annotation>) cl.loadClass(Type.getType(_node.desc).getClassName()), _node));
			} else if (value instanceof Type) {
				map.put(name, cl.loadClass(((Type) value).getClassName()));
			} else if (value instanceof List) {
				map.put(name, ((List) value).toArray());
			} else if (value instanceof String[]) {
				String[] array = (String[]) value;
				if (array.length != 2) {
					throw new InternalError("Expected array length == 2, but got: " + array.length);
				}
				map.put(name, Enum.valueOf((Class) cl.loadClass(Type.getType(array[0]).getClassName()), array[1]));
			} else map.put(name, value);
		}
		injectAnnotationDefaultValues(type, map);
		return (T) AnnotationParser.annotationForMap(type, map);
	}

	private AnnotationNode findAsmAnnotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, AnnotationType asmType, String type) {
		if (asmType == AnnotationType.ANY) {
			AnnotationNode _visible = findAsmAnnotationInList(visible, type);
			return _visible != null ? _visible : findAsmAnnotationInList(invisible, type);
		}
		return findAsmAnnotationInList(selectList(visible, invisible, asmType), type);
	}

	private AnnotationNode findAsmAnnotationInList(List<AnnotationNode> list, String type) {
		if (list == null) return null;
		for (int i = 0, j = list.size(); i < j; i++) {
			AnnotationNode a = list.get(i);
			if (type.equals(a.desc)) return a;
		}
		return null;
	}

	private Object[] findAnnotationLists(List<AnnotationNode> visible, List<AnnotationNode> invisible, Class<? extends Annotation> type) {
		List<AnnotationNode>[] result = new List[2];
		String desc = Type.getDescriptor(type);
		AnnotationNode[] nodes = new AnnotationNode[2];
		assignAnnotationNode(0, nodes, result, visible, desc);
		assignAnnotationNode(1, nodes, result, invisible, desc);
		return new Object[]{result, nodes};
	}

	private void assignAnnotationNode(int i, AnnotationNode[] nodes, List<AnnotationNode>[] result, List<AnnotationNode> list, String desc) {
		if (list == null) return;
		for (int i1 = 0, j = list.size(); i1 < j; i1++) {
			AnnotationNode a = list.get(i1);
			if (desc.equals(a.desc)) {
				nodes[i] = a;
				result[i] = list;
				break;
			}
		}
	}

	private List<AnnotationNode> selectList(List<AnnotationNode> visible, List<AnnotationNode> invisible, AnnotationType type) {
		if (type == AnnotationType.VISIBLE) return visible;
		if (type == AnnotationType.INVISIBLE) return invisible;
		throw new UnsupportedOperationException("Don't know how to handle: " + type);
	}

	public enum AnnotationType {
		VISIBLE,
		INVISIBLE,
		ANY,
	}
}
