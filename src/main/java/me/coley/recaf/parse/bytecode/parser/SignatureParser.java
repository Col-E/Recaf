package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.ast.SignatureAST;
import me.coley.recaf.util.AutoCompleteUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link SignatureAST} parser.
 *
 * @author Matt
 */
public class SignatureParser extends AbstractParser<SignatureAST> {
	@Override
	public SignatureAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			String sig = trim[1];
			// TODO: Verify signature?
			//  - Technically you can put in garbage data in here...
			// Create AST
			int start = line.indexOf(sig);
			return new SignatureAST(lineNo, getOffset() + start, sig);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for descriptor");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if(text.contains("("))
			return Collections.emptyList();
		// Suggest field types
		return AutoCompleteUtil.descriptorName(text.trim());
	}
}
