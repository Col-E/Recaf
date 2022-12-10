package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into normal field.
 *
 * @author Matt Coley
 */
public class AstToFieldTransformer {
	private FieldDefinition definition;

	/**
	 * @return Generated field.
	 */
	public FieldNode buildField() {
		if (definition == null)
			throw new IllegalStateException("No definition set!");
		int access = definition.getModifiers().value() | (definition.isDeprecated() ? Opcodes.ACC_DEPRECATED : 0);;
		String name = definition.getName();
		String descriptor = definition.getDesc();
		String signature = definition.getSignature() != null ? definition.getSignature().getSignature() : null;
		Object value = null;
		if (definition.getConstVal() != null) {
			value = definition.getConstVal().getValue();
			// Handle is the only type that we need to map to the ASM implementation
			if (definition.getConstVal().getValueType() == ArgType.HANDLE) {
				HandleInfo info = (HandleInfo) value;
				int tag = info.getTagVal();
				boolean itf = tag == Opcodes.H_INVOKEINTERFACE;
				value = new Handle(tag, info.getOwner(), info.getName(), info.getDesc(), itf);
			}
		}
		List<AnnotationNode> visibleAnnotations = new ArrayList<>();
		List<AnnotationNode> invisibleAnnotations = new ArrayList<>();
		for (Annotation annotation : definition.getAnnotations()) {
			AnnotationNode node = new AnnotationNode("L" + annotation.getType() + ";");
			node.values = new ArrayList<>();
			annotation.getArgs().forEach((argName, argVal) -> {
				node.values.add(argName);
				node.values.add(AnnotationHelper.map(argVal));
			});
			if (annotation.isVisible()) {
				visibleAnnotations.add(node);
			} else {
				invisibleAnnotations.add(node);
			}
		}
		FieldNode field = new FieldNode(access, name, descriptor, signature, value);
		if (visibleAnnotations.size() > 0)
			field.visibleAnnotations = visibleAnnotations;
		else
			field.visibleAnnotations = null;
		if (invisibleAnnotations.size() > 0)
			field.invisibleAnnotations = invisibleAnnotations;
		else
			field.invisibleAnnotations = null;
		return field;
	}

	/**
	 * @param definition
	 * 		Definition to transform.
	 */
	public void setDefinition(FieldDefinition definition) {
		this.definition = Objects.requireNonNull(definition);
	}
}
