package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for dealing with annotations in AST &lt;--&gt; Bytecode form.
 *
 * @author Matt Coley
 */
public class AnnotationHelper {
	/**
	 * Populates AST with annotations.
	 *
	 * @param code
	 * 		AST to populate.
	 * @param visible
	 * 		Flag for if annotations are intended to be visible.
	 * @param annotations
	 * 		The annotations to add.
	 */
	public static void visitAnnos(Code code, boolean visible, List<AnnotationNode> annotations) {
		if (annotations == null)
			return;
		for (AnnotationNode annotation : annotations) {
			// By default ASM uses the descriptor format.
			// That's a bit of extra vebosity we don't need, and thus our format maps them to plain old types.
			String desc = annotation.desc;
			if (!Types.isValidDesc(desc))
				continue;
			String type = Type.getType(desc).getInternalName();
			Map<String, Annotation.AnnoArg> args = new LinkedHashMap<>();
			if (annotation.values != null) {
				for (int i = 0; i < annotation.values.size(); i += 2) {
					String name = String.valueOf(annotation.values.get(i));
					Object value = annotation.values.get(i + 1);
					args.put(name, BaseArg.of(Annotation.AnnoArg::new, value));
				}
			}
			code.addAnnotation(new Annotation(visible, type, args));
		}
	}

	/**
	 * Populates field with annotations.
	 *
	 * @param field
	 * 		Field to update.
	 * @param annotations
	 * 		Annotations to add.
	 */
	public static void visitAnnos(FieldNode field, List<Annotation> annotations) {
		for (Annotation annotation : annotations) {
			if (annotation.isVisible()) {
				if (field.visibleAnnotations == null)
					field.visibleAnnotations = new ArrayList<>();
				field.visibleAnnotations.add(create(annotation));
			} else {
				if (field.invisibleAnnotations == null)
					field.invisibleAnnotations = new ArrayList<>();
				field.invisibleAnnotations.add(create(annotation));
			}
		}
	}

	/**
	 * Populates method with annotations.
	 *
	 * @param method
	 * 		Method to update.
	 * @param annotations
	 * 		Annotations to add.
	 */
	public static void visitAnnos(MethodNode method, List<Annotation> annotations) {
		for (Annotation annotation : annotations) {
			if (annotation.isVisible()) {
				if (method.visibleAnnotations == null)
					method.visibleAnnotations = new ArrayList<>();
				method.visibleAnnotations.add(create(annotation));
			} else {
				if (method.invisibleAnnotations == null)
					method.invisibleAnnotations = new ArrayList<>();
				method.invisibleAnnotations.add(create(annotation));
			}
		}
	}

	private static AnnotationNode create(Annotation annotation) {
		AnnotationNode annoNode = new AnnotationNode("L" + annotation.getType() + ";");
		if (annotation.getArgs().size() > 0)
			annoNode.values = new ArrayList<>();
		annotation.getArgs().forEach((key, arg) -> {
			Object value = map(arg);
			annoNode.visit(key, value);
		});
		return annoNode;
	}

	@SuppressWarnings("unchecked")
	private static Object map(Annotation.AnnoArg arg) {
		switch (arg.getType()) {
			case TYPE:
			case STRING:
			case INTEGER:
			case FLOAT:
			case DOUBLE:
			case LONG:
			case HANDLE:
				// These values do not need special case mappings
				return arg.getValue();
			case ANNO:
				return create((Annotation) arg.getValue());
			case ANNO_LIST:
				List<Object> list = new ArrayList<>();
				List<Annotation.AnnoArg> value = (List<Annotation.AnnoArg>) arg.getValue();
				for (Annotation.AnnoArg oldArg : value) {
					list.add(oldArg.getValue());
				}
				return list;
			case ANNO_ENUM:
				Annotation.AnnoEnum enumValue = (Annotation.AnnoEnum) arg;
				return new String[]{"L" + enumValue.getEnumType() + ";", enumValue.getEnumName()};
			default:
				throw new IllegalStateException("Unsupported annotation arg type: " + arg.getType());
		}
	}
}
