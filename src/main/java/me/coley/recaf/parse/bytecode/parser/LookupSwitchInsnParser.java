package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.RegexUtil;

import java.util.*;

/**
 * {@link TableSwitchInsnAST} parser.
 *
 * @author Matt
 */
public class LookupSwitchInsnParser extends AbstractParser<LookupSwitchInsnAST> {
	@Override
	public LookupSwitchInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			String opS = RegexUtil.getFirstWord(trim);
			if (opS == null)
				throw new ASTParseException(lineNo, "Missing TABLESWITCH opcode");
			int start = line.indexOf(opS);
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(start);
			OpcodeAST op = opParser.visit(lineNo, opS);
			// Collect parameters
			String[] data = RegexUtil.allMatches(line, "(?<=\\[).*?(?=\\])");
			if (data.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			// mapping
			String mapS = data[0];
			Map<NumberAST, NameAST> mapping = new LinkedHashMap<>();
			if (!mapS.isEmpty()) {
				NameParser nameParser = new NameParser(this);
				IntParser intParser = new IntParser();
				String[] mapS2 = mapS.split(",\\s*");
				for (String map : mapS2) {
					// map: Value=Label
					if (!map.contains("="))
						throw new ASTParseException(lineNo, "Invalid mapping format, expected: <Value>=<Label>");
					nameParser.setOffset(line.indexOf(map));
					intParser.setOffset(line.indexOf(map));
					String[] mapKV = map.split("=");
					mapping.put(intParser.visit(lineNo, mapKV[0]), nameParser.visit(lineNo, mapKV[1]));
				}
			}
			// dflt
			String dfltS = data[1];
			if (mapS.isEmpty()) {
				// Handle case where mapping is empty
				dfltS = trim.substring(trim.lastIndexOf('[') + 1, trim.lastIndexOf(']'));
			}
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.lastIndexOf(dfltS));
			NameAST dflt = nameParser.visit(lineNo, dfltS);
			return new LookupSwitchInsnAST(lineNo, start, op, mapping, dflt);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for table-switch instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if (text.matches(".*[\\[=]\\w+")) {
			String last = RegexUtil.getLastToken("\\w+", text);
			return new NameParser(this).suggest(lastParse, last);
		}
		return Collections.emptyList();
	}
}
