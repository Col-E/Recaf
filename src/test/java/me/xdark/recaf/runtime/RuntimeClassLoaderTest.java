package me.xdark.recaf.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.ref.WeakReference;
import java.security.cert.Certificate;

public class RuntimeClassLoaderTest implements Opcodes {

    @Test
    public void testGC() {
        RuntimeClassLoader classLoader = new RuntimeClassLoader(ClassLoader.getSystemClassLoader());
        ClassWriter test = new ClassWriter(0);
        test.visit(V1_8, ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        MethodVisitor mv = test.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        test.visitEnd();
        Class<?> defined = classLoader.defineClass(new Certificate[0], "TestClass",
                ClassHost.class, test.toByteArray(), null);
        WeakReference<Class<?>> ref = new WeakReference<>(defined);
        defined = null;
        classLoader.close();
        Assertions.assertNull(ref.get());
    }
}
