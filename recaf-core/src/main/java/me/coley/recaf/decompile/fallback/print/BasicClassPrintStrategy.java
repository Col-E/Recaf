package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.ConstPool;
import me.coley.cafedude.Constants;
import me.coley.cafedude.constant.CpClass;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class printing strategy for general class types.
 *
 * @author Matt Coley
 */
public class BasicClassPrintStrategy implements ClassPrintStrategy {
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
				fieldPrinter.appendMultiLine(fieldModel.print());
			out.appendMultiLine(fieldPrinter.toString());
		}
		out.newLine();
		if (model.getMethods().size() > 0) {
			Printer methodPrinter = new Printer();
			methodPrinter.setIndent("    ");
			for (MethodModel methodModel : model.getMethods()) {
				methodPrinter.appendMultiLine(methodModel.print());
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
			String packageName = className.substring(0, className.lastIndexOf('/'));
			out.appendLine("package " + packageName.replace('/', '.') + ";");
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
		List<String> referencedClasses = pool.stream()
				.filter(cp -> cp.getTag() == Constants.ConstantPool.CLASS)
				.map(cp -> pool.getUtf(((CpClass) cp).getIndex()))
				.filter(name -> name.charAt(0) != '[')
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		if (!referencedClasses.isEmpty()) {
			// TODO: This isn't always correct since '$' should also be escaped when it represents the separation of
			//     an outer and inner class. Since we have workspace and runtime access we 'should' check this
			//     and attempt to make more accurate output
			String lastRootPackage = null;
			for (String ref : referencedClasses) {
				if (ref.contains("/")) {
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
					out.appendLine("import " + ref.replace('/', '.') + ";");
				}
			}
			out.newLine();
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

		// TODO: Print annotations
		//  - make annotation printing re-usable even on fields/methods with same logic used here

		if (decFlagsString.isBlank()) {
			sb.append("class ");
		} else {
			sb.append(decFlagsString)
					.append(" class ");
		}
		sb.append(StringUtil.shortenPath(model.getName()));
		String superName = model.getSuperName();
		if (superName != null && !superName.equals("java/lang/Object")) {
			sb.append(" extends ").append(StringUtil.shortenPath(superName));
		}
		if (model.getInterfaces().size() > 0) {
			sb.append(" implements ");
			String interfaces = model.getInterfaces().stream()
					.map(StringUtil::shortenPath)
					.collect(Collectors.joining(", "));
			sb.append(interfaces);
		}
		out.appendLine(sb.toString());
	}
}
