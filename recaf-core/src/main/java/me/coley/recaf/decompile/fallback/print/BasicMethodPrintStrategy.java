package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.cafedude.classfile.attribute.CodeAttribute;
import me.coley.cafedude.classfile.attribute.LocalVariableTableAttribute;
import me.coley.cafedude.classfile.constant.*;
import me.coley.cafedude.classfile.instruction.Instruction;
import me.coley.cafedude.classfile.instruction.IntOperandInstruction;
import me.coley.cafedude.io.InstructionReader;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.StringUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Method printing strategy for normal methods.
 *
 * @author Matt Coley
 */
public class BasicMethodPrintStrategy implements MethodPrintStrategy {
	@Override
	public String print(ClassModel parent, MethodModel model) {
		Printer out = new Printer();
		appendAnnotations(out, model);
		appendDeclaration(out, model);
		if (AccessFlag.isNative(model.getAccess()) || AccessFlag.isAbstract(model.getAccess())) {
			appendAbstractBody(out, model);
		} else {
			appendBody(out, model);
		}
		return out.toString();
	}

	/**
	 * Appends annotations on the method declaration to the printer.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	protected void appendAnnotations(Printer out, MethodModel model) {
		for (Annotation annotation : model.getAnnotations())
			out.appendLine(PrintUtils.annotationToString(model.getPool(), annotation));
	}

	/**
	 * Appends the method declaration to the printer.
	 * <ol>
	 *     <li>{@link #buildDeclarationFlags(StringBuilder, MethodModel)}</li>
	 *     <li>{@link #buildDeclarationReturnType(StringBuilder, MethodModel)}</li>
	 *     <li>{@link #buildDeclarationName(StringBuilder, MethodModel)}</li>
	 *     <li>{@link #buildDeclarationArgs(StringBuilder, MethodModel)}</li>
	 * </ol>
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	protected void appendDeclaration(Printer out, MethodModel model) {
		StringBuilder sb = new StringBuilder();
		buildDeclarationFlags(sb, model);
		buildDeclarationReturnType(sb, model);
		buildDeclarationName(sb, model);
		buildDeclarationArgs(sb, model);
		buildDeclarationThrows(sb, model);
		out.appendLine(sb.toString());
	}

	/**
	 * Appends the abstract method body to the printer.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	protected void appendAbstractBody(Printer out, MethodModel model) {
		out.appendLine(";");
	}

	/**
	 * Appends the method body to the printer.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	protected void appendBody(Printer out, MethodModel model) {
		// For now just hex dump the code, will disassemble later
		CodeAttribute code = model.getCodeAttribute();
		StringBuilder sb = new StringBuilder();
		InstructionReader ir = new InstructionReader();
		List<Instruction> instructions = ir.read(code);
		for (Instruction instruction : instructions) {
			int rawOp = instruction.getOpcode();
			int asmOp = OpcodeUtil.deindexVarOp(rawOp);
			int asmType = OpcodeUtil.opcodeHasType(asmOp) ? OpcodeUtil.opcodeToType(asmOp) : -1;
			String name = OpcodeUtil.opcodeToName(asmOp);
			if (asmOp != rawOp) {
				int index = OpcodeUtil.indexFromVarOp(instruction.getOpcode());
				name += "_" + index;
			}
			sb.append(String.format("%-16s : %s", instruction, name));
			switch (asmType) {
				case AbstractInsnNode.INSN:
					if (asmOp != Opcodes.BIPUSH && asmOp != Opcodes.SIPUSH)
						break;
				case AbstractInsnNode.METHOD_INSN:
				case AbstractInsnNode.TYPE_INSN:
				case AbstractInsnNode.FIELD_INSN:
				case AbstractInsnNode.LDC_INSN: {
					IntOperandInstruction intOp = (IntOperandInstruction) instruction;
					sb.append(" ").append(printCp(model.getPool(), model.getPool().get(intOp.getOperand())));
					break;
				}
				case AbstractInsnNode.VAR_INSN: {
					int index;
					boolean indexed = rawOp != asmOp;
					if (indexed)
						// Raw op is not ASM op, the index is associated with the op
						index = OpcodeUtil.indexFromVarOp(rawOp);
					else
						// Variable op that has an operand instead
						index = ((IntOperandInstruction) instruction).getOperand();
					LocalVariableTableAttribute lvt = model.getLocalVariableTable();
					if (lvt == null) {
						if (!indexed)
							sb.append(" ").append(index);
						break;
					}
					// Get variables of that index
					List<LocalVariableTableAttribute.VarEntry> collect = lvt.getEntries().stream()
							.filter(e -> e.getIndex() == index)
							.collect(Collectors.toList());
					// Append matching variable names
					if (collect.size() == 1) {
						int varNameIndex = collect.get(0).getNameIndex();
						ConstPoolEntry cpName = model.getPool().get(varNameIndex);
						if (cpName instanceof CpUtf8) {
							sb.append(" ").append(((CpUtf8) cpName).getText());
						}
					} else if (collect.size() > 1) {
						String varName = collect.stream()
								.filter(e -> model.getPool().get(e.getNameIndex()) instanceof CpUtf8)
								.map(e -> model.getPool().getUtf(e.getNameIndex()))
								.distinct()
								.collect(Collectors.joining(", "));
						sb.append(" ").append(varName);
					}
					break;
				}
			}
			sb.append('\n');
		}
		Printer disassemblePrinter = new Printer();
		disassemblePrinter.setIndent("    ");
		disassemblePrinter.appendMultiLine("/* ============= Method Bytecode =========== *\\\n" + sb + "*/");
		out.appendLine("{");
		out.appendMultiLine(disassemblePrinter.toString());
		out.appendLine("    throw new RuntimeException(\"Stub method\");");
		out.appendLine("}" + Printer.FORCE_NEWLINE);
	}

	/**
	 * Appends the following pattern to the builder:
	 * <pre>
	 * public static abstract...
	 * </pre>
	 *
	 * @param sb
	 * 		Builder to add to.
	 * @param model
	 * 		Model to pull info from.
	 *
	 * @see #appendDeclaration(Printer, MethodModel) parent caller
	 */
	protected void buildDeclarationFlags(StringBuilder sb, MethodModel model) {
		Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD, model.getAccess());
		flags = AccessFlag.sort(AccessFlag.Type.METHOD, flags);
		if (!flags.isEmpty()) {
			sb.append(AccessFlag.toString(flags)).append(' ');
		}
	}

	/**
	 * Appends the following pattern to the builder:
	 * <pre>
	 * ReturnType
	 * </pre>
	 *
	 * @param sb
	 * 		Builder to add to.
	 * @param model
	 * 		Model to pull info from.
	 *
	 * @see #appendDeclaration(Printer, MethodModel) parent caller
	 */
	protected void buildDeclarationReturnType(StringBuilder sb, MethodModel model) {
		Type methodType = Type.getMethodType(model.getDesc());
		String returnTypeName = EscapeUtil.escapeNonValid(methodType.getReturnType().getClassName());
		if (returnTypeName.contains("."))
			returnTypeName = returnTypeName.substring(returnTypeName.lastIndexOf(".") + 1);
		sb.append(returnTypeName).append(' ');
	}

	/**
	 * Appends the following pattern to the builder:
	 * <pre>
	 * methodName
	 * </pre>
	 *
	 * @param sb
	 * 		Builder to add to.
	 * @param model
	 * 		Model to pull info from.
	 *
	 * @see #appendDeclaration(Printer, MethodModel) parent caller
	 */
	protected void buildDeclarationName(StringBuilder sb, MethodModel model) {
		sb.append(PrintBase.filterName(model.getName()));
	}

	/**
	 * Appends the following pattern to the builder:
	 * <pre>
	 * (Type argName, Type argName)
	 * </pre>
	 *
	 * @param sb
	 * 		Builder to add to.
	 * @param model
	 * 		Model to pull info from.
	 *
	 * @see #appendDeclaration(Printer, MethodModel) parent caller
	 */
	protected void buildDeclarationArgs(StringBuilder sb, MethodModel model) {
		sb.append('(');
		LocalVariableTableAttribute locals = model.getLocalVariableTable();
		boolean isVarargs = AccessFlag.isVarargs(model.getAccess());
		int varIndex = AccessFlag.isStatic(model.getAccess()) ? 0 : 1;
		Type methodType = Type.getMethodType(model.getDesc());
		Type[] argTypes = methodType.getArgumentTypes();
		for (int param = 0; param < argTypes.length; param++) {
			// Get arg type text
			Type argType = argTypes[param];
			String argTypeName = EscapeUtil.escapeNonValid(argType.getClassName());
			if (argTypeName.contains("."))
				argTypeName = argTypeName.substring(argTypeName.lastIndexOf(".") + 1);
			boolean isLast = param == argTypes.length - 1;
			if (isVarargs && isLast && argType.getSort() == Type.ARRAY) {
				argTypeName = StringUtil.replaceLast(argTypeName, "[]", "...");
			}
			// Get arg name
			String name = "p" + varIndex;
			if (locals != null) {
				LocalVariableTableAttribute.VarEntry local = getLocal(locals, varIndex);
				if (local != null)
					name = model.getPool().getUtf(local.getNameIndex());
			}
			// Append to arg list
			sb.append(argTypeName).append(' ').append(name);
			if (!isLast) {
				sb.append(", ");
			}
			// Increment for next var
			varIndex += argType.getSize();
		}
		sb.append(')');
	}

	/**
	 * Appends the following pattern to the builder:
	 * <pre>
	 * throws Item1, Item2, ...
	 * </pre>
	 *
	 * @param sb
	 * 		Builder to add to.
	 * @param model
	 * 		Model to pull info from.
	 *
	 * @see #appendDeclaration(Printer, MethodModel) parent caller
	 */
	protected void buildDeclarationThrows(StringBuilder sb, MethodModel model) {
		List<String> thrownTypes = model.getThrownTypes();
		if (thrownTypes.isEmpty())
			return;
		String shortNames = thrownTypes.stream()
				.map(PrintBase::filterShortenName)
				.collect(Collectors.joining(", "));
		sb.append(" throws ").append(shortNames);
	}

	private static String printCp(ConstPool pool, ConstPoolEntry cp) {
		if (cp instanceof CpString) {
			int i = ((CpString) cp).getIndex();
			return "CP_STRING(" + i + ":" + EscapeUtil.escape(pool.getUtf(i)) + ")";
		} else if (cp instanceof CpClass) {
			int i = ((CpClass) cp).getIndex();
			return "CP_CLASS(" + i + ":" + EscapeUtil.escape(pool.getUtf(i)) + ")";
		} else if (cp instanceof ConstRef) {
			String type = "";
			boolean method = true;
			if (cp instanceof CpMethodRef) {
				type = "CP_METHOD_REF";
			} else if (cp instanceof CpInterfaceMethodRef) {
				type = "CP_INTERFACE_METHOD_REF";
			} else if (cp instanceof CpFieldRef) {
				type = "CP_FIELD_REF";
				method = false;
			}
			int c = ((ConstRef) cp).getClassIndex();
			int n = ((ConstRef) cp).getNameTypeIndex();
			try {
				CpNameType nt = (CpNameType) pool.get(n);
				int c2 = ((CpClass) pool.get(c)).getIndex();
				String rn = pool.getUtf(nt.getNameIndex());
				String rt = pool.getUtf(nt.getTypeIndex());
				String r = method ? (rn + rt) : (rt + " " + rn);
				String ref = EscapeUtil.escape(r);
				return type + "("
						+ c + ":" + EscapeUtil.escape(pool.getUtf(c2)) + ", " +
						+n + ":" + ref + ")";
			} catch (Throwable t) {
				return type + "(INVALID)";
			}
		} else if (cp instanceof CpInt) {
			return "CP_INT(" + ((CpInt) cp).getValue() + ")";
		} else if (cp instanceof CpLong) {
			return "CP_LONG(" + ((CpLong) cp).getValue() + ")";
		} else if (cp instanceof CpDouble) {
			return "CP_DOUBLE(" + ((CpDouble) cp).getValue() + ")";
		} else if (cp instanceof CpFloat) {
			return "CP_FLOAT(" + ((CpFloat) cp).getValue() + ")";
		}
		return "";
	}

	private static LocalVariableTableAttribute.VarEntry getLocal(LocalVariableTableAttribute table, int index) {
		if (table == null)
			return null;
		// Naive, but sufficient for our use case
		for (LocalVariableTableAttribute.VarEntry entry : table.getEntries())
			if (entry.getIndex() == index)
				return entry;
		return null;
	}
}
