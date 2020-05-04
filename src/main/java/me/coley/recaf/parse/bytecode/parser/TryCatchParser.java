package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.RegexUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link ThrowsAST} parser.
 *
 * @author Matt
 */
public class TryCatchParser extends AbstractParser<TryCatchAST> {
	private static final int TRY_LEN = "TRY ".length();

	@Override
	public TryCatchAST visit(int lineNo, String line) throws ASTParseException {
		try {
			// TRY start end CATCH(type) handler
			String trim = line.trim();
			int start = line.indexOf(trim);
			NameParser nameParser = new NameParser(this);
			if (!trim.contains("CATCH"))
				throw new ASTParseException(lineNo, "Missing CATCH(<type>) <handler>");
			int catchIndex = trim.indexOf("CATCH");
			String[] parts = trim.substring(0, catchIndex).split("\\s+");
			// 0 = TRY
			// 1 = start
			// 2 = end
			NameAST lblStart = nameParser.visit(lineNo, parts[1]);
			nameParser.setOffset(start + TRY_LEN);
			nameParser.setOffset(trim.indexOf(parts[2]));
			NameAST lblEnd = nameParser.visit(lineNo, parts[2]);
			// parse type
			String typeS = RegexUtil.getFirstToken("(?<=\\().+(?=\\))", trim);
			if (typeS == null)
				throw new ASTParseException(lineNo, "Missing type in CATCH(<type>)");
			typeS = typeS.trim();
			TypeAST type = null;
			if (!typeS.equals("*")) {
				TypeParser typeParser = new TypeParser();
				typeParser.setOffset(line.indexOf(typeS));
				type = typeParser.visit(lineNo, typeS.trim());
			} else {
				// Wildcard, type is null internally
				type = new TypeAST(lineNo, line.indexOf(typeS), "*") {
					@Override
					public String getType() {
						return null;
					}
				};
			}
			// handler label
			int typeEnd = trim.indexOf(')');
			nameParser.setOffset(typeEnd + 1);
			NameAST lblHandler = nameParser.visit(lineNo, trim.substring(typeEnd + 1).trim());
			return new TryCatchAST(lineNo, start, lblStart, lblEnd, type, lblHandler);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for TRY");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// Suggest exception type
		int start = text.indexOf('(') + 1;
		int end = text.indexOf(')');
		if (start > 0 && end == -1) {
			String type = text.substring(start).trim();
			return new TypeParser().suggest(lastParse, type);
		}
		// No context of last parse, can't pull names from last parsed AST
		if(lastParse == null)
			return Collections.emptyList();
		// Suggest label names
		try {
			String trim = text.trim();
			NameParser nameParser = new NameParser(this);
			String[] parts = trim.split("\\s+");
			return nameParser.suggest(lastParse, parts[parts.length - 1]);
		} catch(Exception ex) {
			return Collections.emptyList();
		}
	}
}
