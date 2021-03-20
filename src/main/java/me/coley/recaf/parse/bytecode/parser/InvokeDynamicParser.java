package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.RegexUtil;

import java.util.*;

/**
 * {@link InvokeDynamicAST} parser.
 *
 * @author Matt
 */
public class InvokeDynamicParser extends AbstractParser<InvokeDynamicAST> {
	private static String BRACKET_WRAPPING = "\\w*\\[.+]";
	private static String BRACKET_WRAPPING_OR_EMPTY = "\\w*\\[.*]";

	@Override
	public InvokeDynamicAST visit(int lineNo, String line) throws ASTParseException {
		try {
			// Split here:
			//              v    v    v
			// INVOKEDYNAMIC name desc handle[...] args[...]
			String[] trim = line.trim().split("\\s+(?=.*\\[(?=.*\\[))");
			if (trim.length < 4)
				throw new ASTParseException(lineNo, "Not enough paramters");
			// 0 = op
			// 1 = name
			// 2 = desc
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// name
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(trim[1]));
			NameAST name = nameParser.visit(lineNo, trim[1]);
			// desc
			DescParser descParser = new DescParser();
			descParser.setOffset(line.indexOf(trim[2]));
			DescAST desc = descParser.visit(lineNo, trim[2]);
			// handle & args
			// - Split space between handle and args
			trim = line.substring(RegexUtil.indexOf("(?:(?<=\\s)handle|handle|\\s)\\[\\s*H_", line))
					.split("(?<=\\])\\s+(?=.*\\[)");
			// handle
			String handleS = trim[0];
			if (!handleS.matches(BRACKET_WRAPPING))
				throw new ASTParseException(lineNo, "Invalid handle, require wrapping in '[' and ']'");
			handleS = handleS.substring(handleS.indexOf('[') + 1, handleS.indexOf(']'));
			HandleParser handleParser = new HandleParser();
			handleParser.setOffset(line.indexOf(trim[0]));
			HandleAST handle = handleParser.visit(lineNo, handleS);
			// args
			String argsS = trim[1];
			if (!argsS.matches(BRACKET_WRAPPING_OR_EMPTY))
				throw new ASTParseException(lineNo, "Invalid args, require wrapping in '[' and ']'");
			argsS = argsS.substring(argsS.indexOf('[') + 1, argsS.lastIndexOf(']'));
			// if the args has a string with commas, this will break...
			// we'll fix that whenever it happens
			List<AST> args = new ArrayList<>();
			if (!argsS.isEmpty()) {
				String[] argsSplit = argsS.split(",\\s*(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
				for(String arg : argsSplit) {
					AST ast = parseArg(lineNo, arg);
					if(ast == null)
						throw new ASTParseException(lineNo, "Failed parsing BSM arg: " + arg);
					args.add(ast);
				}
			}
			return new InvokeDynamicAST(lineNo, start, op, name, desc, handle, args);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for invokedynamic instruction");
		}
	}

	/**
	 * @param lineNo
	 * 		Line number.
	 * @param arg
	 * 		Arg text.
	 *
	 * @return BSM arg ast value.
	 *
	 * @throws ASTParseException
	 * 		When the arg cannot be parsed.
	 */
	private static AST parseArg(int lineNo, String arg) throws ASTParseException {
		AbstractParser parser = null;
		if(arg.contains("\""))
			parser = new StringParser();
		else if(arg.matches("-?\\d+"))
			parser = new IntParser();
		else if(arg.matches("-?\\d+[LlJj]?"))
			parser = new LongParser();
		else if(arg.matches("-?\\d+\\.\\d+[Ff]?"))
			parser = new FloatParser();
		else if(arg.matches("-?\\d+\\.\\d+[Dd]?"))
			parser = new DoubleParser();
		else if(arg.matches(BRACKET_WRAPPING)) {
			parser = new HandleParser();
			arg = arg.substring(arg.indexOf('[') + 1, arg.indexOf(']'));
		} else
			parser = new DescParser();
		return parser.visit(lineNo, arg);
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// INVOKEDYNAMIC name desc handle[...] args[...]
		//  - Can only really suggest the handle...
		int b1 = text.indexOf('[');
		int b2 = text.lastIndexOf('[');
		if(b1 == -1)
			// not at handle yet
			return Collections.emptyList();
		else if(b2 != b1)
			// past handle, at args
			return Collections.emptyList();
		else
			// in the handle
			return new HandleParser().suggest(lastParse, text.substring(b1 + 1));
	}
}
