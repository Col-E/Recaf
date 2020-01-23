package me.xdark.recaf.jvm;

import me.xdark.recaf.jvm.instrumentation.ClassFileTransformer;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public interface Compiler {
	Class compileClass(Class parent, UnresolvedClass unresolvedClass);

	Field compileField(Class declaringClass, FieldNode node);

	Method compileMethod(Class declaringClass, MethodNode node);

	void registerTransformer(ClassFileTransformer transformer);
}
