package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.FieldNode;

/**
 * Assembler for field.
 *
 * @author Matt
 */
public class FieldAssembler implements Assembler<FieldNode> {
	@Override
	public FieldNode compile(ParseResult<RootAST> result) throws AssemblerException {
		if(!result.isSuccess()) {
			ASTParseException cause = result.getProblems().get(0);
			AssemblerException ex  = new AssemblerException(cause, "AST must not contain errors", cause.getLine());
			ex.addSubExceptions(result.getProblems());
			throw ex;
		}
		RootAST root = result.getRoot();
		// Get definition
		FieldDefinitionAST definition = root.search(FieldDefinitionAST.class).stream().findFirst().orElse(null);
		if (definition == null)
			throw new AssemblerException("AST must have definition statement");
		int access = toAccess(definition);
		String name = definition.getName().getName();
		String desc = definition.getType().getDesc();
		SignatureAST signatureAST = root.search(SignatureAST.class).stream().findFirst().orElse(null);
		DefaultValueAST defaultValueAST = root.search(DefaultValueAST.class).stream().findFirst().orElse(null);
		String signature = (signatureAST == null) ? null : signatureAST.getSignature();
		Object value = (defaultValueAST == null) ? null : defaultValueAST.toValue();
		return new FieldNode(access, name, desc, signature, value);
	}

	/**
	 * @param definition
	 * 		AST of definition. Contains modifier AST children.
	 *
	 * @return Combined value of modifier children.
	 */
	private int toAccess(FieldDefinitionAST definition) {
		return definition.search(DefinitionModifierAST.class).stream()
				.mapToInt(DefinitionModifierAST::getValue)
				.reduce(0, (a, b) -> a | b);
	}
}
