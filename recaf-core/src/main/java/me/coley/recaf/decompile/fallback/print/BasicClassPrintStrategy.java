package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.ConstantPoolConstants;
import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.cafedude.classfile.constant.ConstPoolEntry;
import me.coley.cafedude.classfile.constant.CpClass;
import me.coley.cafedude.classfile.constant.CpMethodType;
import me.coley.cafedude.classfile.constant.CpNameType;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class printing strategy for general class types.
 *
 * @author Matt Coley
 */
public class BasicClassPrintStrategy implements ClassPrintStrategy, ConstantPoolConstants {
	@Override
	public String print(ClassModel model) {
		Printer out = new Printer();
		appendPackage(out, model);
		appendImports(out, model);
		appendDeclaration(out, model);
		out.appendLine("{");
		if (model.getFields().size() > 0) {
			Printer fieldPrinter = new Printer();
			fieldPrinter.setIndent("    ");
			for (FieldModel fieldModel : model.getFields())
				fieldPrinter.appendMultiLine(fieldModel.print(PrintContext.DEFAULT_CTX));
			out.appendMultiLine(fieldPrinter.toString());
		}
		out.newLine();
		if (model.getMethods().size() > 0) {
			Printer methodPrinter = new Printer();
			methodPrinter.setIndent("    ");
			for (MethodModel methodModel : model.getMethods()) {
				methodPrinter.appendMultiLine(methodModel.print(PrintContext.DEFAULT_CTX));
				methodPrinter.newLine();
			}
			out.appendMultiLine(methodPrinter.toString());
		}
		out.appendLine("}");
		return out.toString();
	}

	/**
	 * Appends the package name to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	private void appendPackage(Printer out, ClassModel model) {
		String className = model.getName();
		if (className.contains("/")) {
			String packageName = EscapeUtil.escapeNonValid(className.substring(0, className.lastIndexOf('/')));
			out.appendLine("package " + packageName.replace('/', '.') + ";");
			out.newLine();
		}
	}

	/**
	 * Appends each imported class to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	private void appendImports(Printer out, ClassModel model) {
		ConstPool pool = model.getClassFile().getPool();
		SortedSet<String> referencedClasses = new TreeSet<>();
		for (ConstPoolEntry cpEntry : pool) {
			int tag = cpEntry.getTag();
			if (tag == CLASS) {
				int classNameIndex = ((CpClass) cpEntry).getIndex();
				String name = pool.getUtf(classNameIndex);
				if (name.length() > 0 && name.charAt(0) != '[' && name.charAt(name.length() - 1) != ';')
					referencedClasses.add(name);
			} else if (tag == NAME_TYPE) {
				int typeIndex = ((CpNameType) cpEntry).getTypeIndex();
				collectTypes(pool.getUtf(typeIndex), referencedClasses);
			} else if (tag == METHOD_TYPE) {
				int typeIndex = ((CpMethodType) cpEntry).getIndex();
				collectTypes(pool.getUtf(typeIndex), referencedClasses);
			}
		}
		for (FieldModel field : model.getFields())
			collectTypes(field.getDesc(), referencedClasses);
		for (MethodModel method : model.getMethods())
			collectTypes(method.getDesc(), referencedClasses);
		referencedClasses.removeIf(n -> !n.contains("/") || (n.startsWith("java/lang/") && n.lastIndexOf('/') <= 10));
		referencedClasses.remove(model.getName());
		if (!referencedClasses.isEmpty()) {
			// TODO: Import names aren't always correct since '$' should also be escaped when it represents the separation of
			//     an outer and inner class. Since we have workspace and runtime access we 'should' check this
			//     and attempt to make more accurate output
			String lastRootPackage = referencedClasses.first();
			lastRootPackage = lastRootPackage.substring(0, lastRootPackage.indexOf('/'));
			for (String ref : referencedClasses) {
				String rootPackage = ref.substring(0, ref.indexOf('/'));
				// Break root package imports up for clarity. For example:
				//  - com.*
				//  - org.*
				// Between these two import groups will be a blank line.
				if (!rootPackage.equals(lastRootPackage)) {
					out.newLine();
					lastRootPackage = rootPackage;
				}
				// Add import
				out.appendLine("import " + PrintBase.filterShortenName(ref.replace('/', '.')) + ";");
			}
			out.newLine();
		}
	}

	/**
	 * Called from {@link #appendImports(Printer, ClassModel)}.
	 *
	 * @param referencedClasses
	 * 		Collection to add to.
	 */
	private void collectTypes(String desc, Collection<String> referencedClasses) {
		char c = desc.charAt(0);
		if (c == '(') {
			Type methodType = Type.getMethodType(desc);
			for (Type argType : methodType.getArgumentTypes()) {
				if (argType.getSort() == Type.OBJECT) {
					String argTypeName = argType.getInternalName();
					referencedClasses.add(argTypeName);
				}
			}
			Type returnType = methodType.getReturnType();
			if (returnType.getSort() == Type.OBJECT) {
				String returnTypeName = returnType.getInternalName();
				referencedClasses.add(returnTypeName);
			}
		} else if (c == 'L') {
			String fieldTypeName = Type.getType(desc).getInternalName();
			referencedClasses.add(fieldTypeName);
		}
	}

	/**
	 * Appends the class definition to the output. This pattern includes:
	 * <ul>
	 *     <li>Modifiers</li>
	 *     <li>Class / Interface / Enum keywords <i>(Depending on impl of this class)</i></li>
	 *     <li>Class name</li>
	 *     <li>Extended class</li>
	 *     <li>Implemented classes</li>
	 * </ul>
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param model
	 * 		Model to pull info from.
	 */
	protected void appendDeclaration(Printer out, ClassModel model) {
		int acc = model.getAccess();
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, acc);
		StringBuilder sb = new StringBuilder();
		for (Annotation annotation : model.getAnnotations())
			sb.append(PrintUtils.annotationToString(model.getPool(), annotation)).append("\n");
		if (decFlagsString.isBlank()) {
			sb.append("class ");
		} else {
			sb.append(decFlagsString)
					.append(" class ");
		}
		sb.append(PrintBase.filterShortenName(model.getName()));
		String superName = model.getSuperName();
		if (superName != null && !superName.equals("java/lang/Object")) {
			sb.append(" extends ").append(PrintBase.filterShortenName(superName));
		}
		if (model.getInterfaces().size() > 0) {
			sb.append(" implements ");
			String interfaces = model.getInterfaces().stream()
					.map(PrintBase::filterShortenName)
					.collect(Collectors.joining(", "));
			sb.append(interfaces);
		}
		out.appendLine(sb.toString());
	}
}
