package me.coley.recaf.decompile.fallback;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.BuildConfig;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

/**
 * A basic decompiler implementation that generates a rough outline of the class.
 * The intent is to be used as a fallback option if other decompilers fail.
 *
 * @author Matt Coley
 */
public class FallbackDecompiler extends Decompiler {
	private static final String VERSION = "1X:" + BuildConfig.GIT_REVISION;

	/**
	 * New basic decompiler instance.
	 */
	public FallbackDecompiler() {
		super("Fallback", VERSION);
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo info) {
		try {
			// TODO: Resilience measures Handle funky unicode escapes
			ClassFile classFile = new ClassFileReader().read(info.getValue());
			ClassModel model = new ClassModel(classFile);
			return model.print();
		} catch (Throwable t) {
			StringWriter trace = new StringWriter();
			t.printStackTrace(new PrintWriter(trace));
			return "// Could not parse class: " + info.getName() + "\n// " +
					trace.toString().replace("\n", "\n// ");
		}
	}


	private void appendFields(StringBuilder out, ClassInfo info, ClassFile classFile) {
		if (!info.getFields().isEmpty()) {
			for (FieldInfo field : info.getFields()) {
				String typeStr = Type.getType(field.getDescriptor()).getClassName();
				if (typeStr.contains("."))
					typeStr = typeStr.substring(typeStr.lastIndexOf(".") + 1);
				out.append("    ")
						.append(AccessFlag.sortAndToString(AccessFlag.Type.FIELD, field.getAccess()))
						.append(" ")
						.append(typeStr)
						.append(" ")
						.append(field.getName())
						.append(";\n");
			}
			out.append("\n");
		}
	}

	private void appendMethods(StringBuilder out, ClassInfo info, ClassFile classFile) {
		if (!info.getMethods().isEmpty()) {
			for (MethodInfo method : info.getMethods()) {
				String methodName = method.getName();
				Type methodType = Type.getMethodType(method.getDescriptor());
				String retTypeStr = methodType.getReturnType().getClassName();
				if (retTypeStr.contains("."))
					retTypeStr = retTypeStr.substring(retTypeStr.lastIndexOf(".") + 1);

				String args = "";

				if (methodName.equals("<init>")) {
					out.append("    ")
							.append(AccessFlag.sortAndToString(AccessFlag.Type.METHOD, method.getAccess()))
							.append(" ")
							.append(StringUtil.shortenPath(info.getName()))
							.append("(")
							.append(args)
							.append(") {\n")
							.append("        throw new RuntimeException(\"Stub constructor\");\n")
							.append("    }\n\n");
				} else if (methodName.equals("<clinit>")) {
					out.append("    static {\n")
							.append("        throw new RuntimeException(\"Stub static initializer\");\n")
							.append("    }\n\n");
				} else {
					out.append("    ")
							.append(AccessFlag.sortAndToString(AccessFlag.Type.METHOD, method.getAccess()))
							.append(" ")
							.append(retTypeStr)
							.append(" ")
							.append(methodName)
							.append("(")
							.append(args)
							.append(") {\n")
							.append("        throw new RuntimeException(\"Stub method\");\n")
							.append("    }\n\n");
				}

			}
		}
	}


	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		return Collections.emptyMap();
	}
}
