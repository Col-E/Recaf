package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.*;

import java.util.Collection;

/**
 * Collects all types referenced within a method.
 *
 * @author yapht
 */
public class TypeCollectionMethodVisitor extends MethodVisitor {
    private final Collection<String> types;

    /**
     * @param types
     *          A set to store the collected types in
     * @param visitor
     *          Parent method visitor, can be null
     */
    public TypeCollectionMethodVisitor(Collection<String> types, MethodVisitor visitor) {
        super(RecafConstants.ASM_VERSION, visitor);
        this.types = types;
    }

    public TypeCollectionMethodVisitor(Collection<String> types) {
        this(types, null);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        types.add(type);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        types.add(descriptor);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        types.add(owner);
        types.add(descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        types.add(owner);
        types.add(descriptor);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        types.add(descriptor);
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        types.add(type);
        super.visitTryCatchBlock(start, end, handler, type);
    }
}
