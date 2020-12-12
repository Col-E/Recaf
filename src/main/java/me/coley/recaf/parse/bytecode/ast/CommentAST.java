package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * Comment AST.
 *
 * @author Matt
 */
public class CommentAST extends AST implements Compilable {
	private final String comment;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param comment
	 * 		Content of comment.
	 */
	public CommentAST(int line, int start, String comment) {
		super(line, start);
		this.comment = comment;
	}

	/**
	 * @return Content of comment.
	 */
	public String getComment() {
		return comment;
	}


	@Override
	public String print() {
		return "//" + comment;
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addComment(comment.trim());
	}
}
