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
public class TableSwitchInsnParser extends AbstractParser<TableSwitchInsnAST> {
	@Override
	public TableSwitchInsnAST visit(int lineNo, String line) throws ASTParseException {
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
			if (data.length < 3)
				throw new ASTParseException(lineNo, "Not enough parameters");
			// min & max
			String minMaxS = data[0];
			if (!minMaxS.contains(":"))
				throw new ASTParseException(lineNo, "Bad range format, expected <MIN>:<MAX>");
			int minMaxStart = line.indexOf(minMaxS);
			String[] minMaxS2 = minMaxS.split(":");
			IntParser intParser = new IntParser();
			intParser.setOffset(minMaxStart);
			NumberAST min = intParser.visit(lineNo, minMaxS2[0]);
			NumberAST max = intParser.visit(lineNo, minMaxS2[1]);
			// labels
			List<NameAST> labels = new ArrayList<>();
			String labelsS = data[1];
			int labelsStart = line.indexOf(labelsS);
			NameParser parser = new NameParser(this);
			String[] labelsS2 = labelsS.split(",\\s*");
			for (String name : labelsS2) {
				parser.setOffset(labelsStart + labelsS.indexOf(name));
				labels.add(parser.visit(lineNo, name));
			}
			// dflt
			String dfltS = data[2];
			parser.setOffset(line.lastIndexOf(dfltS));
			NameAST dflt = parser.visit(lineNo, dfltS);
			return new TableSwitchInsnAST(lineNo, start, op, min, max, labels, dflt);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for table-switch instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if (text.contains(" ")) {
			String last = RegexUtil.getLastToken("\\w+", text);
			return new NameParser(this).suggest(lastParse, last);
		}
		return Collections.emptyList();
	}
}
