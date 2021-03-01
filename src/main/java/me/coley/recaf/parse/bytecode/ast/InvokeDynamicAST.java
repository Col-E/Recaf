package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Invokedynamic instruction AST.
 *
 * @author Matt
 */
public class InvokeDynamicAST extends InsnAST {
	private final NameAST name;
	private final DescAST desc;
	private final HandleAST handle;
	private final List<AST> args;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param name
	 * 		Invokedynamic name AST.
	 * @param desc
	 * 		Invokedynamic descriptor AST.
	 * @param handle
	 * 		Invokedynamic handle AST.
	 * @param args
	 * 		Invokedynamic arguments.
	 */
	public InvokeDynamicAST(int line, int start, OpcodeAST opcode, NameAST name, DescAST desc,
							HandleAST handle, List<AST> args) {
		super(line, start, opcode);
		this.name = name;
		this.desc = desc;
		this.handle = handle;
		this.args = args;
		addChild(name);
		addChild(desc);
		addChild(handle);
		args.forEach(this::addChild);
	}

	/**
	 * @return Name AST of invokedynamic.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Desc AST of invokedynamic.
	 */
	public DescAST getDesc() {
		return desc;
	}

	/**
	 * @return Invoke dynamic handle AST.
	 */
	public HandleAST getHandle() {
		return handle;
	}

	/**
	 * @return Invokedynamic arguments.
	 */
	public List<AST> getArgs() {
		return args;
	}

	@Override
	public String print() {
		String argsStr = args.stream()
				.map(AST::print)
				.collect(Collectors.joining(", "));
		return getOpcode().print() + " " +  name.print() + " " + desc.print() + " " +
				handle.print() + " args[" + argsStr + "]";
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		Object[] convertedArgs = new Object[args.size()];
		for(int i = 0; i < args.size(); i++) {
			AST arg = args.get(i);
			if(arg instanceof NumberAST) {
				convertedArgs[i] = ((NumberAST) arg).getValue();
			} else if(arg instanceof StringAST) {
				convertedArgs[i] = ((StringAST) arg).getUnescapedValue();
			} else if(arg instanceof HandleAST) {
				convertedArgs[i] = ((HandleAST) arg).compile();
			} else if(arg instanceof TypeAST) {
				convertedArgs[i] = Type.getType(((TypeAST) arg).getType());
			} else if(arg instanceof DescAST) {
				convertedArgs[i] = Type.getType(((DescAST) arg).getDesc());
			}
		}
		compilation.addInstruction(new InvokeDynamicInsnNode(getName().getUnescapedName(), getDesc().getUnescapedDesc(),
				getHandle().compile(), convertedArgs), this);
	}
}
