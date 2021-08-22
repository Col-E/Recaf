package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link TypeAST} parser.
 *
 * @author Matt
 */
public class LdcInsnParser extends AbstractParser<LdcInsnAST> {
	@Override
	public LdcInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			int ti = line.indexOf(trim);
			int space = line.indexOf(' ');
			String opS = trim.substring(0, space);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(opS));
			OpcodeAST op = opParser.visit(lineNo, opS);
			// content
			String content = trim.substring(space + 1);
			AST ast = null;
			if(content.contains("\"")) {
				// String
				StringParser parser = new StringParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			} else if(content.contains("H_") && content.contains("]")) {
				// Handle
				HandleParser parser = new HandleParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content.substring(content.indexOf('[') + 1, content.lastIndexOf(']')));
			} else if(content.startsWith("(") || content.contains("[") || content.contains(";")) {
				// Type
				DescParser parser = new DescParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			} else if(content.matches("-?(?:(?:Infinity|NaN)|-?(?:\\d+\\.\\d+))[Ff]") ||
					content.matches("-?[\\d.]+[eE](?:-?\\d+)?[Ff]")) {
				// Float
				FloatParser parser = new FloatParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			} else if(content.matches("-?\\d+[LlJj]")) {
				// Long
				LongParser parser = new LongParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			} else if(content.matches("-?(?:Infinity|NaN)|-?(?:\\d+\\.\\d+[Dd]?)") ||
					content.matches("-?[\\d.]+[eE](?:-?\\d+)?[dD]?")) {
				// Double
				DoubleParser parser = new DoubleParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			} else {
				// Integer
				IntParser parser = new IntParser();
				parser.setOffset(ti + space + 1);
				ast = parser.visit(lineNo, content);
			}
			return new LdcInsnAST(lineNo, ti, op, ast);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for LDC");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// Attempt to complete content for Type values
		if (text.contains(" ") && !text.contains("\"")) {
			String[] parts = text.split("\\s+");
			return new TypeParser().suggest(lastParse, parts[parts.length - 1]);
		}
		return Collections.emptyList();
	}
}
