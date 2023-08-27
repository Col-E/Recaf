package me.coley.recaf.search.query;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.IndyInstruction;
import me.coley.recaf.assemble.ast.insn.LdcInstruction;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.*;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A query that looks for text matches in all accessible locations.
 *
 * @author Matt Coley
 */
public class TextQuery implements Query {
	private static final Logger logger = Logging.get(TextQuery.class);
	private final String query;
	private final TextMatchMode mode;

	/**
	 * @param query
	 * 		The text, or pattern depending on the matching mode, to look for.
	 * @param mode
	 * 		The matching strategy of the query against discovered text.
	 */
	public TextQuery(String query, TextMatchMode mode) {
		this.query = query;
		this.mode = mode;
	}

	@Override
	public QueryVisitor createVisitor(Resource resource, QueryVisitor delegate) {
		return new TextClassVisitor(resource, delegate);
	}

	private void whenMatched(Object value, Consumer<ResultBuilder> builderConsumer) {
		if (value instanceof String) {
			String text = (String) value;
			if (mode.match(query, text)) {
				builderConsumer.accept(ResultBuilder.text(text));
			}
		}
	}

	@Override
	public String toString() {
		List<String> lines = new ArrayList<>();
		if (query != null)
			lines.add("value=" + query);
		if (mode != null)
			lines.add("mode=" + mode.name());
		return "Text [" + String.join(", ", lines) + ']';
	}

	private class TextClassVisitor extends QueryVisitor {
		public TextClassVisitor(Resource resource, QueryVisitor delegate) {
			super(resource, delegate);
		}

		@Override
		public void visitFile(FileInfo fileInfo) {
			if (fileInfo.isText()) {
				String text = new String(fileInfo.getValue(), StandardCharsets.UTF_8);
				String[] lines = text.split("[\\n\\r]+");
				for (String line : lines)
					whenMatched(line.trim(), builder -> addFileText(builder, fileInfo));
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			whenMatched(value, builder -> addField(builder, name, desc));
			FieldInfo fieldInfo = currentClass.findField(name, desc);
			if (fieldInfo != null) {
				return new TextFieldVisitor(fv, fieldInfo);
			} else {
				logger.error("Failed to lookup field for query: {}.{} {}", currentClass.getName(), name, desc);
				return fv;
			}
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodInfo methodInfo = currentClass.findMethod(name, desc);
			if (methodInfo != null) {
				return new TextMethodVisitor(mv, methodInfo);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", currentClass.getName(), name, desc);
				return mv;
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new TextAnnotationVisitor(av, builder -> addClassAnno(builder, desc));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new TextAnnotationVisitor(av, builder -> addClassAnno(builder, desc));
		}

		private class TextFieldVisitor extends FieldVisitor {
			private final FieldInfo fieldInfo;

			public TextFieldVisitor(FieldVisitor delegate, FieldInfo fieldInfo) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.fieldInfo = fieldInfo;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				AnnotationVisitor av = super.visitAnnotation(desc, visible);
				return new TextAnnotationVisitor(av, builder -> addFieldAnno(builder,
						fieldInfo.getName(), fieldInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				return new TextAnnotationVisitor(av, builder -> addFieldAnno(builder,
						fieldInfo.getName(), fieldInfo.getDescriptor(), desc));
			}
		}

		private class TextMethodVisitor extends MethodVisitor {
			private final MethodInfo methodInfo;

			public TextMethodVisitor(MethodVisitor delegate, MethodInfo methodInfo) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.methodInfo = methodInfo;
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String desc, Handle bsmHandle,
											   Object... bootstrapMethodArguments) {
				super.visitInvokeDynamicInsn(name, desc, bsmHandle, bootstrapMethodArguments);
				if (bootstrapMethodArguments.length != 0) {
					IndyInstruction insn = new IndyInstruction(Opcodes.INVOKEDYNAMIC, name, desc,
							new HandleInfo(bsmHandle),
							Arrays.stream(bootstrapMethodArguments)
									.map(arg -> IndyInstruction.BsmArg.of(IndyInstruction.BsmArg::new, arg))
									.collect(Collectors.toList()));
					for (Object bsmArg : bootstrapMethodArguments) {
						whenMatched(bsmArg, builder ->
							addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(), insn)
						);
					}
				}
			}

			@Override
			public void visitLdcInsn(Object value) {
				super.visitLdcInsn(value);
				whenMatched(value, builder -> addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(),
						LdcInstruction.of(value)));
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				AnnotationVisitor av = super.visitAnnotation(desc, visible);
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				AnnotationVisitor av = super.visitAnnotationDefault();
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), null));
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
				AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
				if(desc.isEmpty()) return av;
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
				if(desc.isEmpty()) return av;
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc,
															 boolean visible) {
				AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
				if(desc.isEmpty()) return av;
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
																  Label[] start, Label[] end, int[] index,
																  String desc, boolean visible) {
				AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end,
						index, desc, visible);
				if(desc.isEmpty()) return av;
				return new TextAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}
		}

		private class TextAnnotationVisitor extends AnnotationVisitor {
			private final Consumer<ResultBuilder> resultDelegate;

			public TextAnnotationVisitor(AnnotationVisitor delegate, Consumer<ResultBuilder> resultDelegate) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.resultDelegate = resultDelegate;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String descriptor) {
				AnnotationVisitor av = super.visitAnnotation(name, descriptor);
				return new TextAnnotationVisitor(av, resultDelegate);
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor av = super.visitArray(name);
				return new TextAnnotationVisitor(av, resultDelegate);
			}

			@Override
			public void visit(String name, Object value) {
				super.visit(name, value);
				whenMatched(value, resultDelegate);
			}
		}
	}
}
