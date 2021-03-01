package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.EscapeUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link NameAST} parser.
 *
 * @author Matt
 */
public class NameParser extends AbstractParser<NameAST> {
	/**
	 * Create name parser with parent context.
	 *
	 * @param parent
	 * 		Parent parser.
	 */
	public NameParser(AbstractParser parent) {
		super(parent);
	}

	@Override
	public NameAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if (trim.isEmpty())
				throw new ASTParseException(lineNo, "Name cannot be empty!");
			trim = EscapeUtil.unescape(trim);
			int start = line.indexOf(trim);
			return new NameAST(lineNo, getOffset() + start, trim);
		} catch (Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for name");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// No context of last parse, can't pull names from last parsed AST
		// No context parent parser, unsure what to suggest
		if (lastParse == null || parent == null)
			return Collections.emptyList();
		// Complete labels if we belong to a label parser
		boolean tableParent = parent instanceof TableSwitchInsnParser || parent instanceof LookupSwitchInsnParser;
		if (parent instanceof TryCatchParser || parent instanceof JumpInsnParser || parent instanceof LineInsnParser
			|| tableParent) {
			return lastParse.getRoot().search(LabelAST.class).stream()
					.map(l -> l.getName().getName())
					.filter(n -> n.startsWith(text))
					.collect(Collectors.toList());
		}
		// Complete variable names if we belong to a variable parser
		else if (parent instanceof VarInsnParser || parent instanceof IincInsnParser) {
			return lastParse.getRoot().search(NameAST.class).stream()
					.filter(ast -> ast.getName().startsWith(text))
					.filter(ast -> ast.getParent() instanceof VarInsnAST || ast.getParent() instanceof DefinitionArgAST)
					.map(NameAST::getName)
					.distinct()
					.sorted()
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}
