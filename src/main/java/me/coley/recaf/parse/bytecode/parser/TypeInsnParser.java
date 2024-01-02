package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link TypeInsnAST} parser.
 *
 * @author Matt
 */
public class TypeInsnParser extends AbstractParser<TypeInsnAST> {
	@Override
	public TypeInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// type
			TypeParser typeParser = new TypeParser();
			typeParser.setOffset(line.indexOf(trim[1]));
			TypeAST type = typeParser.visit(lineNo, trim[1]);
			return new TypeInsnAST(lineNo, start, op, type);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for type instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if (text.contains(" ")) {
			String[] parts = text.split("\\s+");
			return new TypeParser().suggest(lastParse, parts[parts.length - 1]);
		}
		return Collections.emptyList();
	}
}
