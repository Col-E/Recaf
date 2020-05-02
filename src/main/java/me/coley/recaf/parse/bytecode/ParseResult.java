package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.AST;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.List;

/**
 * AST parse result.
 *
 * @param <T>
 * 		Type of root node AST.
 */
public class ParseResult<T extends AST> {
	private final List<ASTParseException> problems;
	private final T root;

	/**
	 * @param root
	 * 		Root node of AST.
	 * @param problems
	 * 		Parse problems.
	 */
	public ParseResult(T root, List<ASTParseException> problems) {
		this.root = root;
		this.problems = problems;
	}

	/**
	 * @return {@code true} if no problems found when building the AST.
	 */
	public boolean isSuccess() {
		return problems.isEmpty();
	}

	/**
	 * @return Parse problems.
	 */
	public List<ASTParseException> getProblems() {
		return problems;
	}

	/**
	 * @return Parsed AST. May be incomplete if there are {@link #getProblems() problems}.
	 */
	public T getRoot() {
		return root;
	}

}
