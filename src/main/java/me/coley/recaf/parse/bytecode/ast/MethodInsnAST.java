package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.Opcodes;
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
	private final ItfAST itf;

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
		this(line, start, opcode, owner, name, desc, null);
	}

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
	 * @param itf
	 *      Present on invokestatic if this is an interface method ref
	 */
	public MethodInsnAST(int line, int start, OpcodeAST opcode, TypeAST owner, NameAST name, DescAST desc, ItfAST itf) {
		super(line, start, opcode);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.itf = itf;
		addChild(owner);
		addChild(name);
		addChild(desc);
		if (itf != null) {
			addChild(itf);
		}
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

	/**
	 * @return Itf AST for static invocations of interface methods.
	 */
	public ItfAST getItf() {
		return itf;
	}

	@Override
	public String print() {
		String ret = getOpcode().print() + " " + owner.print() + "." + name.print() + desc.print();
		if (itf != null) {
			ret += " " + itf.print();
		}
		return ret;
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		int opcode = getOpcode().getOpcode();
		if (opcode == Opcodes.INVOKESTATIC) {
			compilation.addInstruction(new MethodInsnNode(opcode, getOwner().getType(),
					getName().getName(), getDesc().getDesc(), itf != null), this);
		} else {
			compilation.addInstruction(new MethodInsnNode(opcode, getOwner().getType(),
					getName().getName(), getDesc().getDesc()), this);
		}
	}
}
