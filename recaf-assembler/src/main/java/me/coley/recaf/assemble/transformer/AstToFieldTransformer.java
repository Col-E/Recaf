package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

import java.util.Objects;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into normal field.
 *
 * @author Matt Coley
 */
public class AstToFieldTransformer {
	private final Unit unit;
	// For quick reference
	private final FieldDefinition definition;
	private final Code code;

	/**
	 * @param unit
	 * 		The unit to pull data from.
	 */
	public AstToFieldTransformer(Unit unit) {
		this.unit = Objects.requireNonNull(unit);
		this.definition = (FieldDefinition) unit.getDefinition();
		this.code = unit.getCode();
	}

	/**
	 * @return Generated field.
	 */
	public FieldNode get() {
		int access = definition.getModifiers().value();
		String name = definition.getName();
		String descriptor = definition.getDesc();
		String signature = code.getSignature() != null ? code.getSignature().getSignature() : null;
		Object value = null;
		if (code.getConstVal() != null) {
			value = code.getConstVal().getValue();
			// Handle is the only type that we need to map to the ASM implementation
			if (code.getConstVal().getValueType() == ArgType.HANDLE) {
				HandleInfo info = (HandleInfo) value;
				int tag = info.getTagVal();
				boolean itf = tag == Opcodes.H_INVOKEINTERFACE;
				value = new Handle(tag, info.getOwner(), info.getName(), info.getDesc(), itf);
			}
		}
		FieldNode field = new FieldNode(access, name, descriptor, signature, value);
		AnnotationHelper.visitAnnos(field, code.getAnnotations());
		return field;
	}
}
