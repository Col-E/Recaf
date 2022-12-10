package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.arch.module.*;
import me.coley.recaf.assemble.ast.arch.module.Module;
import me.coley.recaf.assemble.ast.arch.record.Record;
import me.coley.recaf.assemble.ast.arch.record.RecordComponent;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveClassSupplier;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforms a {@link ClassDefinition} into a {@link ClassNode}.
 *
 * @author Justus Garbe
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
		node.access = definition.getModifiers().value() | (definition.isDeprecated() ? Opcodes.ACC_DEPRECATED : 0);
		node.name = definition.getName();
		node.superName = definition.getSuperClass();
		node.interfaces = definition.getInterfaces();
		List<AnnotationNode> visibleAnnotations = new ArrayList<>();
		List<AnnotationNode> invisibleAnnotations = new ArrayList<>();
		for (Annotation annotation : definition.getAnnotations()) {
			AnnotationNode annotationNode = new AnnotationNode("L" + annotation.getType() + ";");
			annotationNode.values = new ArrayList<>();
			annotation.getArgs().forEach((argName, argVal) -> {
				annotationNode.values.add(argName);
				annotationNode.values.add(AnnotationHelper.map(argVal));
			});
			if (annotation.isVisible()) {
				visibleAnnotations.add(annotationNode);
			} else {
				invisibleAnnotations.add(annotationNode);
			}
		}
		if (visibleAnnotations.size() > 0)
			node.visibleAnnotations = visibleAnnotations;
		else
			node.visibleAnnotations = null;
		if (invisibleAnnotations.size() > 0)
			node.invisibleAnnotations = invisibleAnnotations;
		else
			node.invisibleAnnotations = null;
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
		Module module = definition.getModule();
		if(module != null) {
			ModuleVisitor visitor = node.visitModule(module.getName(), module.getModifiers().value(), module.getVersion());
			for (String aPackage : module.getPackages()) {
				visitor.visitPackage(aPackage);
			}
			for (ModuleRequire require : module.getRequires()) {
				visitor.visitRequire(require.getName(), require.getModifiers().value(), require.getVersion());
			}
			for (ModuleExport export : module.getExports()) {
				visitor.visitExport(export.getName(), export.getModifiers().value(), export.getPackages().toArray(new String[0]));
			}
			for (ModuleOpen open : module.getOpens()) {
				visitor.visitOpen(open.getName(), open.getModifiers().value(), open.getPackages().toArray(new String[0]));
			}
			for (String use : module.getUses()) {
				visitor.visitUse(use);
			}
			for (ModuleProvide provide : module.getProvides()) {
				visitor.visitProvide(provide.getName(), provide.getPackages().toArray(new String[0]));
			}
			visitor.visitEnd();
		}
		Record record = definition.getRecord();
		if(record != null) {
			for (RecordComponent component : record.getComponents()) {
				String signature = component.getSignature() == null ? null : component.getSignature().getSignature();
				RecordComponentVisitor visitor = node.visitRecordComponent(component.getName(), component.getDescriptor(), signature);
				for (Annotation annotation : component.getAnnotations()) {
					AnnotationVisitor annotationVisitor = visitor.visitAnnotation("L" + annotation.getType() + ";", annotation.isVisible());
					annotation.getArgs().forEach((argName, argVal) -> annotationVisitor.visit(argName, AnnotationHelper.map(argVal)));
					annotationVisitor.visitEnd();
				}
				visitor.visitEnd();
			}
		}
		return node;
	}

}
