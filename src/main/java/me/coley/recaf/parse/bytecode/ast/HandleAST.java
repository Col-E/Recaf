package me.coley.recaf.parse.bytecode.ast;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

/**
 * Handle AST.
 *
 * @author Matt
 */
public class HandleAST extends AST {
	private final TagAST tag;
	private final TypeAST owner;
	private final NameAST name;
	private final DescAST desc;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param tag
	 * 		Handle tag AST.
	 * @param owner
	 * 		Handle reference owner type AST.
	 * @param name
	 * 		Handle reference name AST.
	 * @param desc
	 * 		Handle reference descriptor AST.
	 */
	public HandleAST(int line, int start, TagAST tag, TypeAST owner, NameAST name, DescAST desc) {
		super(line, start);
		this.tag = tag;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		addChild(tag);
		addChild(owner);
		addChild(name);
		addChild(desc);
	}

	/**
	 * @return Handle tag AST.
	 */
	public TagAST getTag() {
		return tag;
	}

	/**
	 * @return Type AST of reference owner.
	 */
	public TypeAST getOwner() {
		return owner;
	}

	/**
	 * @return Name AST of reference name.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Desc AST of reference descriptor.
	 */
	public DescAST getDesc() {
		return desc;
	}

	@Override
	public String print() {
		String split = getTag().isMethod() ? "" : " ";
		return "handle[" + getTag().print() + " " +
				owner.print() + "." + name.print() + split + desc.print() + "]";
	}

	/**
	 * @return ASM handle from AST data.
	 */
	public Handle compile() {
		return new Handle(getTag().getTag(), getOwner().getUnescapedType(), getName().getUnescapedName(),
				getDesc().getUnescapedDesc(), getTag().getTag() == Opcodes.H_INVOKEINTERFACE);
	}
}
