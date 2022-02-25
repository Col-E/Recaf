package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;
import org.objectweb.asm.Type;

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
		if (AccessFlag.isAbstract(model.getAccess())) {
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
		out.appendMultiLine("{\n" +
				"    throw new RuntimeException(\"Stub method\");\n" +
				"}" + Printer.FORCE_NEWLINE);
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
		String returnTypeName = methodType.getReturnType().getClassName();
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
		sb.append(model.getName());
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
		boolean isVarargs = AccessFlag.isVarargs(model.getAccess());
		int varIndex = AccessFlag.isStatic(model.getAccess()) ? 0 : 1;
		Type methodType = Type.getMethodType(model.getDesc());
		Type[] argTypes = methodType.getArgumentTypes();
		for (int param = 0; param < argTypes.length; param++) {
			// Get arg type text
			Type argType = argTypes[param];
			String argTypeName = argType.getClassName();
			if (argTypeName.contains("."))
				argTypeName = argTypeName.substring(argTypeName.lastIndexOf(".") + 1);
			boolean isLast = param == argTypes.length - 1;
			if (isVarargs && isLast && argType.getSort() == Type.ARRAY) {
				argTypeName = StringUtil.replaceLast(argTypeName, "[]", "...");
			}
			// Get arg name
			//  - TODO: Pull from variable table (if valid names)
			//     - Can lean on assembler and yoink 'em outta MethodDefinition
			String name = "p" + varIndex;
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
				.map(StringUtil::shortenPath)
				.collect(Collectors.joining(", "));
		sb.append(" throws ").append(shortNames);
	}
}
