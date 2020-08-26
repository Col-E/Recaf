package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

/**
 * Method reference instruction AST.
 *
 * @author Matt
 */
public class MethodInsnAST extends InsnAST {
	private final TypeAST owner;
	private final NameAST name;
	private final DescAST desc;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param owner
	 * 		Method owner type AST.
	 * @param name
	 * 		Method name AST.
	 * @param desc
	 * 		Method descriptor AST.
	 */
	public MethodInsnAST(int line, int start, OpcodeAST opcode, TypeAST owner, NameAST name, DescAST desc) {
		super(line, start, opcode);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		addChild(owner);
		addChild(name);
		addChild(desc);
	}

	/**
	 * @return Type AST of method owner.
	 */
	public TypeAST getOwner() {
		return owner;
	}

	/**
	 * @return Name AST of method name.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Desc AST of method descriptor.
	 */
	public DescAST getDesc() {
		return desc;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + owner.print() + "." + name.print() + desc.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new MethodInsnNode(getOpcode().getOpcode(), getOwner().getType(),
				getName().getName(), getDesc().getDesc()), this);
	}
}
