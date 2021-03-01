package me.coley.recaf.parse.bytecode.ast;
	
import me.coley.recaf.util.EscapeUtil;

/**
 * Member descriptor AST.
 *
 * @author Matt
 */
public class DescAST extends AST {
	private final String desc;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param desc
	 * 		Member descriptor.
	 */
	public DescAST(int line, int start, String desc) {
		super(line, start);
		this.desc = desc;
	}

	/**
	 * @return Member descriptor.
	 */
	public String getDesc() {
		return desc;
	}
	
	/**
	 * @return Desc without escapes.
	 */
	public String getUnescapedDesc() {
		return EscapeUtil.unescape(desc);
	}

	@Override
	public String print() {
		return desc;
	}
}
