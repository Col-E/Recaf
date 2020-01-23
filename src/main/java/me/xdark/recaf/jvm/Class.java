package me.xdark.recaf.jvm;

import me.xdark.recaf.jvm.classloading.ClassLoader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Class {
	private boolean resolved;
	private final VirtualMachine vm;
	private final Class superclass;
	private final ClassNode handle;
	private final ClassLoader classLoader;
	private Map<MemberIdentifier, Method> dispatchTable;
	private Map<MemberIdentifier, Field> fieldTable;

	protected Class(VirtualMachine vm, Class superclass, ClassNode handle,ClassLoader classLoader) {
		this.vm = vm;
		this.superclass = superclass;
		this.handle = handle;
		this.classLoader = classLoader;
	}

	public String getName() {
		return handle.name;
	}

	public int getModifiers() {
		return handle.access;
	}

	public Class getSuperclass() {
		return superclass;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void resolve() {
		synchronized (this) {
			if (this.resolved) return;
			this.resolved = true;
		}
		Compiler compiler = vm.getCompiler();
		// Fill dispatch table
		{
			List<MethodNode> methods = handle.methods;
			int size = methods.size();
			Map<MemberIdentifier, Method> dispatchTable = this.dispatchTable = new HashMap<>(size);
			while (size-- > 0) {
				MethodNode node = methods.get(size);
				dispatchTable.put(new MemberIdentifier(node.name, node.desc), compiler.compileMethod(vm, this, node));
			}
		}
		// Fill fields table
		{
			List<FieldNode> fields = handle.fields;
			int size = fields.size();
			Map<MemberIdentifier, Field> fieldTable = this.fieldTable = new HashMap<>(size);
			while (size-- > 0) {
				FieldNode node = fields.get(size);
				fieldTable.put(new MemberIdentifier(node.name, node.desc), compiler.compileField(vm, this, node));
			}
		}
	}
}
