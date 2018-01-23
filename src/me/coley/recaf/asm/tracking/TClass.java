package me.coley.recaf.asm.tracking;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;

/**
 * ClassNode implementation that keeps track of elements
 * 
 * @author Matt
 */
public class TClass extends ClassNode {
	public TClass() {
		super(Recaf.INSTANCE.configs.asm.version);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
			final String[] exceptions) {
		TMethod mn = new TMethod(this, access, name, desc, signature, exceptions);
		methods.add(mn);
		return mn;
	}
//@formatter:off
/*
	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
		InnerClassNode icn = new InnerClassNode(name, outerName, innerName, access);
		innerClasses.add(icn);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
			final Object value) {
		FieldNode fn = new FieldNode(access, name, desc, signature, value);
		fields.add(fn);
		return fn;
	}
	
	@Override
	public ModuleVisitor visitModule(final String name, final int access, final String version) {
		return module = new ModuleNode(name, access, version);
	}
*/
}
