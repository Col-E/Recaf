package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.Descriptor;
import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.VariableReference;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Variable instruction.
 *
 * @author Matt Coley
 */
public class VarInstruction extends AbstractInstruction implements Opcodes, Named, Descriptor, VariableReference {
	private final String identifier;

	/**
	 * @param opcode
	 * 		Variable instruction opcode.
	 * @param identifier
	 * 		Variable identifier.
	 */
	public VarInstruction(int opcode, String identifier) {
		super(opcode);
		this.identifier = identifier;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.VAR;
	}

	@Override
	public String getVariableIdentifier() {
		return getName();
	}

	@Override
	public String getVariableDescriptor() {
		return getDesc();
	}

	@Override
	public boolean isObjectDescriptorExplicitlyDeclared() {
		// Without stack analysis, we can only know something like 'ASTORE' implies 'Object'
		return false;
	}

	@Override
	public OpType getVariableOperation() {
		int opcode = getOpcodeVal();
		if (opcode == ALOAD || opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD)
			return OpType.LOAD;
		else
			return OpType.ASSIGN;
	}

	@Override
	public String getDesc() {
		Type type = Types.fromVarOpcode(getOpcodeVal());
		if (type != null)
			return type.getDescriptor();
		throw new IllegalStateException("Variable opcode '" + getOpcode() + "' does not have a type mapping!");
	}

	@Override
	public String getName() {
		return identifier;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + " " + getEscapedVariableIdentifier();
	}
}
