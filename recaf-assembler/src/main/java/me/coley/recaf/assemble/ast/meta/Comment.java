package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.PrintContext;

/**
 * User documentation that exists within a body of some kind.
 *
 * @author Matt Coley
 */
public class Comment extends BaseElement implements CodeEntry {
	private final String comment;

	/**
	 * @param comment
	 * 		Comment text.
	 */
	public Comment(String comment) {
		this.comment = comment;
	}

	@Override
	public void insertInto(Code code) {
		code.addComment(this);
	}

	@Override
	public String print(PrintContext context) {
		if (isMultiLine()) {
			return "/*\n" + comment + "\n*/";
		} else {
			return "//" + comment;
		}
	}

	/**
	 * @return {@code true} when the comment text contains multiple lines.
	 */
	public boolean isMultiLine() {
		return comment.indexOf('\n') > 0;
	}

	/**
	 * @return Comment text.
	 */
	public String getComment() {
		return comment;
	}
}
