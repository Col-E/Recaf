package me.xdark.recaf.jvm;

import me.xdark.recaf.jvm.classloading.ClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Class {
	private boolean resolved;
	private final Compiler compiler;
	private final Class superclass;
	private UnresolvedClass unresolvedClass;
	private final String name;
	private final int modifiers;
	private ClassLoader classLoader;
	private Map<MemberIdentifier, Method> dispatchTable;
	private Map<MemberIdentifier, Field> fieldTable;

	protected Class(Compiler compiler, Class superclass, UnresolvedClass unresolvedClass) {
		this.compiler = compiler;
		this.superclass = superclass;
		this.unresolvedClass = unresolvedClass;
		ClassReader reader = unresolvedClass.getClassReader();
		this.name = reader.getClassName();
		this.modifiers = reader.getAccess();
	}

	public String getName() {
		return name;
	}

	public int getModifiers() {
		return modifiers;
	}

	public Class getSuperclass() {
		return superclass;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Method getMethod(String name, String desc) {
		resolve();
		return dispatchTable.get(new MemberIdentifier(name, desc));
	}

	public Field getField(String name, String desc) {
		resolve();
		return fieldTable.get(new MemberIdentifier(name, desc));
	}

	public void resolve() {
		synchronized (this) {
			if (this.resolved) return;
			this.resolved = true;
		}
		ClassNode handle = unresolvedClass.resolve();
		unresolvedClass = null;
		// Fill dispatch table
		{
			List<MethodNode> methods = handle.methods;
			int size = methods.size();
			Map<MemberIdentifier, Method> dispatchTable = this.dispatchTable = new HashMap<>(size);
			while (size-- > 0) {
				MethodNode node = methods.get(size);
				dispatchTable.put(new MemberIdentifier(node.name, node.desc), compiler.compileMethod(this, node));
			}
		}
		// Fill fields table
		{
			List<FieldNode> fields = handle.fields;
			int size = fields.size();
			Map<MemberIdentifier, Field> fieldTable = this.fieldTable = new HashMap<>(size);
			while (size-- > 0) {
				FieldNode node = fields.get(size);
				fieldTable.put(new MemberIdentifier(node.name, node.desc), compiler.compileField(this, node));
			}
		}
	}
}
