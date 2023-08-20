package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.arch.Annotatable;
import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	public static void visitAnnos(Annotatable code, boolean visible, List<AnnotationNode> annotations) {
		if (annotations == null)
			return;
		for (AnnotationNode annotation : annotations) {
			// By default ASM uses the descriptor format.
			// That's a bit of extra vebosity we don't need, and thus our format maps them to plain old types.
			String desc = annotation.desc;
			if (!Types.isValidDesc(desc))
				continue;
			String type = Type.getType(desc).getInternalName();
			Map<String, Annotation.AnnoArg> args = mapArgs(annotation);
			code.addAnnotation(new Annotation(visible, type, args));
		}
	}

	/**
	 * Convert arg value wrappers to the types ASM expects to use.
	 *
	 * @param arg
	 * 		Value wrapper used by {@link Annotation#getArgs()}.
	 *
	 * @return Value to be used for {@link AnnotationNode#values} pairs.
	 */
	@SuppressWarnings("unchecked")
	public static Object map(BaseArg arg) {
		ArgType type = arg.getType();
		switch (type) {
			case TYPE:
			case STRING:
			case SHORT:
			case BOOLEAN:
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

	@SuppressWarnings("unchecked")
	private static Map<String, Annotation.AnnoArg> mapArgs(AnnotationNode annotation) {
		Map<String, Annotation.AnnoArg> args = new LinkedHashMap<>();
		if (annotation.values != null) {
			for (int i = 0; i < annotation.values.size(); i += 2) {
				String name = String.valueOf(annotation.values.get(i));
				Object value = annotation.values.get(i + 1);
				if (value instanceof AnnotationNode) {
					AnnotationNode annoValue = (AnnotationNode) value;
					Map<String, Annotation.AnnoArg> subArgs = mapArgs(annoValue);
					args.put(name, new Annotation.AnnoArg(
							ArgType.ANNO,
							new Annotation(true, Type.getType(annoValue.desc).getInternalName(), subArgs)));
					continue;
				}
				if (value instanceof List) {
					value = ((List<Object>) value).stream()
							.map(v -> BaseArg.of(Annotation.AnnoArg::new, mapAnnotationArgValue(v)))
							.collect(Collectors.toList());
				}
				args.put(name, BaseArg.of(Annotation.AnnoArg::new, value));
			}
		}
		return args;
	}

	private static Object mapAnnotationArgValue(Object value) {
		// Annotation argument of a sub-annotation needs to be mapped to our AST format
		if (value instanceof AnnotationNode) {
			AnnotationNode anno = (AnnotationNode) value;
			String type = Type.getType(anno.desc).getInternalName();
			value = new Annotation(true, type, mapArgs((AnnotationNode) value));
		}
		return value;
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
}
