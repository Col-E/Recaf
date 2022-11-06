package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Collects all types referenced within a class and (optionally) types referenced within class method code.
 *
 * @author yapht
 */
public class TypeCollectionClassVisitor extends ClassVisitor {
    private final Collection<String> types;
    private final boolean shouldVisitMethods;
    private final MethodVisitor methodVisitor;

    /**
     * @param types              A set to store the collected types in
     * @param classVisitor       Parent class visitor, can be null
     * @param methodVisitor      Parent method visitor, can be null
     * @param shouldVisitMethods Whether to collect types from method code
     */
    public TypeCollectionClassVisitor(Collection<String> types, ClassVisitor classVisitor, MethodVisitor methodVisitor, boolean shouldVisitMethods) {
        super(RecafConstants.ASM_VERSION, classVisitor);
        this.types = types;
        this.shouldVisitMethods = shouldVisitMethods;
        this.methodVisitor = methodVisitor;
    }

    public TypeCollectionClassVisitor(Collection<String> types, boolean shouldVisitMethods) {
        this(types, null, null, shouldVisitMethods);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        types.add(name);
        types.add(superName);
        types.addAll(Arrays.asList(interfaces));
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        types.add(descriptor);
        types.add(signature);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        types.add(permittedSubclass);
        super.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        types.add(descriptor);
        types.add(signature);
        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        types.add(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        types.add(descriptor);
        types.add(signature);
        if (exceptions != null) {
            Collections.addAll(types, exceptions);
        }

        if (shouldVisitMethods) {
            return new TypeCollectionMethodVisitor(types, methodVisitor);
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
