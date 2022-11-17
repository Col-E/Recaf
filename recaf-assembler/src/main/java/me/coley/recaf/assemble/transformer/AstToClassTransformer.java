package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.arch.ClassDefinition;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import me.coley.recaf.assemble.ast.arch.InnerClass;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveClassSupplier;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Transforms a {@link ClassDefinition} into a {@link ClassNode}.
 *
 * @author Nowilltolife
 */
public class AstToClassTransformer {
	private final ClassDefinition definition;
	private final ClassSupplier classSupplier = ReflectiveClassSupplier.getInstance();
	private final InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.getInstance();

	public AstToClassTransformer(ClassDefinition definition) {
		this.definition = definition;
	}

	// TODO: Finish this bla bla bla.
	//  - cleanup 'ClassDefinition'
	//  - validate / add unit test for this transformer
	//  - implement in UI module

	/**
	 * @return Generated class.
	 *
	 * @throws MethodCompileException
	 * 		When a contained method could not be generated.
	 */
	public ClassNode buildClass() throws MethodCompileException {
		// TODO: This does not populate all possible elements of the class
		//  - can be completed later though
		ClassNode node = new ClassNode();
		node.version = definition.getVersion();
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
		for (InnerClass innerClass : definition.getInnerClasses()) {
			node.visitInnerClass(innerClass.getName(),
					innerClass.getOuterName(),
					innerClass.getInnerName(),
					innerClass.getModifiers().value());
		}
		if(definition.getSourceFile() != null) {
			node.sourceFile = definition.getSourceFile();
		}
		if(definition.getNestHost() != null) {
			node.visitNestHost(definition.getNestHost());
		}
		for (String nestMember : definition.getNestMembers()) {
			node.visitNestMember(nestMember);
		}
		return node;
	}

}
