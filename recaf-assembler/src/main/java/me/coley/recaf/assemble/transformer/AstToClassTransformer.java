package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.util.*;
import org.objectweb.asm.tree.*;

public class AstToClassTransformer {

    private final ClassDefinition definition;
    private final ClassSupplier classSupplier = ReflectiveClassSupplier.getInstance();
    private final InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.getInstance();

    public AstToClassTransformer(ClassDefinition definition) {
        this.definition = definition;
    }

    public ClassNode buildClass() throws MethodCompileException {
        ClassNode node = new ClassNode();
        node.access = definition.getModifiers().value();
        node.name = definition.getName();
        node.superName = definition.getSuperClass();
        node.interfaces = definition.getInterfaces();
        AstToFieldTransformer fieldTransformer = new AstToFieldTransformer();
        for (FieldDefinition definedField : definition.getDefinedFields()) {
            fieldTransformer.setDefinition(definedField);
            FieldNode field = fieldTransformer.buildField();
            node.fields.add(field);
        }
        AstToMethodTransformer methodTransformer = new AstToMethodTransformer(classSupplier, definition.getName());
        methodTransformer.setInheritanceChecker(inheritanceChecker);
        for (MethodDefinition definedMethod : definition.getDefinedMethods()) {
            methodTransformer.setDefinition(definedMethod);
            methodTransformer.visit();
            node.methods.add(methodTransformer.buildMethod());
        }
        return node;
    }

}
