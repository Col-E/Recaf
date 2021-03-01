package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;
import org.objectweb.asm.Type;
import me.coley.recaf.util.EscapeUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link DescAST} parser.
 *
 * @author Matt
 */
public class DescParser extends AbstractParser<DescAST> {
	@Override
	public DescAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			trim = EscapeUtil.unescape(trim);
			// Verify
			if(trim.contains("(")) {
				Type type = Type.getMethodType(trim);
				if(!validate(type.getReturnType().getDescriptor()))
					throw new ASTParseException(lineNo,
							"Invalid method return type " + type.getReturnType().getDescriptor());
				for(Type arg : type.getArgumentTypes())
					if(!validate(arg.getDescriptor()))
						throw new ASTParseException(lineNo,
								"Invalid method arg type " + arg.getDescriptor());
			} else {
				if(!validate(trim))
					throw new ASTParseException(lineNo, "Invalid field descriptor: " + trim);
			}
			// Create AST
			int start = line.indexOf(trim);
			return new DescAST(lineNo, getOffset() + start, trim);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for descriptor: " + ex.getMessage());
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if(text.contains("("))
			return Collections.emptyList();
		// Suggest field types
		return AutoCompleteUtil.descriptorName(text.trim());
	}

	private static boolean validate(String token) {
		// Void check
		if(token.equals("V"))
			return true;
		// Ensure type is not an array
		Type type = Type.getType(token);
		while(type.getSort() == Type.ARRAY)
			type = type.getElementType();
		// Check for primitives
		if(type.getSort() < Type.ARRAY)
			return true;
		// Verify L...; pattern
		// - getDescriptor doesn't modify the original element type (vs getInternalName)
		String desc = type.getDescriptor();
		return desc.startsWith("L") && desc.endsWith(";");
	}
}
