package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.*;

/**
 * Base parser.
 *
 * @param <T>
 * 		Node type to parse.
 *
 * @author Matt
 */
public abstract class AbstractParser<T extends AST> {
	protected final AbstractParser parent;
	private int offset;

	/**
	 * Create parser with no parent context.
	 */
	public AbstractParser() {
		this(null);
	}

	/**
	 * Create parser with parent context.
	 *
	 * @param parent
	 * 		Parent parser.
	 */
	public AbstractParser(AbstractParser parent) {
		this.parent = parent;
	}

	/**
	 * @param lineNo
	 * 		Line number.
	 * @param text
	 * 		Text to complete.
	 *
	 * @return AST node.
	 *
	 * @throws ASTParseException
	 * 		When the line did not follow the expected format.
	 */
	public abstract T visit(int lineNo, String text) throws ASTParseException;

	/**
	 * @param lastParse
	 * 		Previously generated AST, used for hints and look-ups if not {@code null}.
	 * @param text
	 * 		Text to complete.
	 *
	 * @return Suggestions for given text.
	 */
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		return Collections.emptyList();
	}

	/**
	 * Used when there are multiple parsers per line and each's parsing content is not the full
	 * text of the line.
	 *
	 * @return Offset to add to start position of generated AST nodes in
	 * {@link #visit(int, String)}.
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset
	 * 		Offset to add to start position of generated AST nodes in {@link #visit(int, String)}.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
}
