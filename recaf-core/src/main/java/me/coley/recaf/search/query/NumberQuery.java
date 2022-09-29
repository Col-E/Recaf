package me.coley.recaf.search.query;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.NumberMatchMode;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.*;

/**
 * A query that looks for numeric value matches in all accessible locations.
 *
 * @author Matt Coley
 */
public class NumberQuery implements Query {
	private static final Logger logger = Logging.get(NumberQuery.class);
	private final Number query;
	private final NumberMatchMode mode;

	/**
	 * @param query
	 * 		The numeric value to look for.
	 * @param mode
	 * 		The matching strategy of the query against discovered values.
	 */
	public NumberQuery(Number query, NumberMatchMode mode) {
		this.query = query;
		this.mode = mode;
	}

	@Override
	public QueryVisitor createVisitor(Resource resource, QueryVisitor delegate) {
		return new NumberClassVisitor(resource, delegate);
	}

	private void whenMatched(Object value, Consumer<ResultBuilder> builderConsumer) {
		if (value instanceof Number) {
			Number number = (Number) value;
			if (mode.match(query, number)) {
				builderConsumer.accept(ResultBuilder.number(number));
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
		return "Number [" + String.join(", ", lines) + ']';
	}

	private class NumberClassVisitor extends QueryVisitor {
		public NumberClassVisitor(Resource resource, QueryVisitor delegate) {
			super(resource, delegate);
		}

		@Override
		public void visitFile(FileInfo fileInfo) {
			// no-op
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			whenMatched(value, builder -> addField(builder, name, desc));
			FieldInfo fieldInfo = currentClass.findField(name, desc);
			if (fieldInfo != null) {
				return new NumberFieldVisitor(fv, fieldInfo);
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
				return new NumberMethodVisitor(mv, methodInfo);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", currentClass.getName(), name, desc);
				return mv;
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new NumberAnnotationVisitor(av, builder -> addClassAnno(builder, desc));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new NumberAnnotationVisitor(av, builder -> addClassAnno(builder, desc));
		}

		private class NumberFieldVisitor extends FieldVisitor {
			private final FieldInfo fieldInfo;

			public NumberFieldVisitor(FieldVisitor delegate, FieldInfo fieldInfo) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.fieldInfo = fieldInfo;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				AnnotationVisitor av = super.visitAnnotation(desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addFieldAnno(builder,
						fieldInfo.getName(), fieldInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addFieldAnno(builder,
						fieldInfo.getName(), fieldInfo.getDescriptor(), desc));
			}
		}

		private class NumberMethodVisitor extends MethodVisitor {
			private final MethodInfo methodInfo;

			public NumberMethodVisitor(MethodVisitor delegate, MethodInfo methodInfo) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.methodInfo = methodInfo;
			}

			@Override
			public void visitInsn(int opcode) {
				super.visitInsn(opcode);
				if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
					whenMatched(getValue(opcode), builder -> addMethodInsn(builder, methodInfo.getName(),
							methodInfo.getDescriptor(),
							new Instruction(opcode)));
				}
			}

			@Override
			public void visitIntInsn(int opcode, int operand) {
				super.visitIntInsn(opcode, operand);
				whenMatched(operand, builder -> addMethodInsn(builder, methodInfo.getName(),
						methodInfo.getDescriptor(),
						new IntInstruction(opcode, operand)));
			}

			@Override
			public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
				super.visitTableSwitchInsn(min, max, dflt, labels);
				whenMatched(min, builder -> addMethodInsn(builder, methodInfo.getName(),
						methodInfo.getDescriptor(),
						new TableSwitchInstruction(TABLESWITCH, min, max, Collections.emptyList(), "?")));
				whenMatched(max, builder -> addMethodInsn(builder, methodInfo.getName(),
						methodInfo.getDescriptor(),
						new TableSwitchInstruction(TABLESWITCH, min, max, Collections.emptyList(), "?")));
			}

			@Override
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
				super.visitLookupSwitchInsn(dflt, keys, labels);
				LookupSwitchInstruction insn = new LookupSwitchInstruction(LOOKUPSWITCH, IntStream.of(keys)
						.mapToObj(i -> new LookupSwitchInstruction.Entry(i, "?")).collect(Collectors.toList()),
						"?");
				for (int key : keys)
					whenMatched(key, builder ->
						addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(), insn)
					);
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrapMethodHandle,
											   Object... bootstrapMethodArguments) {
				super.visitInvokeDynamicInsn(name, desc, bootstrapMethodHandle, bootstrapMethodArguments);
				if (bootstrapMethodArguments.length != 0) {
					IndyInstruction insn = new IndyInstruction(INVOKEDYNAMIC, name, desc,
							new HandleInfo(bootstrapMethodHandle),
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
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				AnnotationVisitor av = super.visitAnnotationDefault();
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), null));
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
				AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc,
															 boolean visible) {
				AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			@Override
			public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
																  Label[] start, Label[] end, int[] index,
																  String desc, boolean visible) {
				AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end,
						index, desc, visible);
				return new NumberAnnotationVisitor(av, builder -> addMethodAnno(builder,
						methodInfo.getName(), methodInfo.getDescriptor(), desc));
			}

			private int getValue(int opcode) {
				switch (opcode) {
					case ICONST_M1:
						return -1;
					case FCONST_0:
					case LCONST_0:
					case DCONST_0:
					case ICONST_0:
						return 0;
					case FCONST_1:
					case LCONST_1:
					case DCONST_1:
					case ICONST_1:
						return 1;
					case FCONST_2:
					case ICONST_2:
						return 2;
					case ICONST_3:
						return 3;
					case ICONST_4:
						return 4;
					case ICONST_5:
						return 5;
					default:
						throw new IllegalArgumentException("Invalid opcode, does not have a known value: " + opcode);
				}
			}
		}

		private class NumberAnnotationVisitor extends AnnotationVisitor {
			private final Consumer<ResultBuilder> resultDelegate;

			public NumberAnnotationVisitor(AnnotationVisitor delegate, Consumer<ResultBuilder> resultDelegate) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.resultDelegate = resultDelegate;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String descriptor) {
				AnnotationVisitor av = super.visitAnnotation(name, descriptor);
				return new NumberAnnotationVisitor(av, resultDelegate);
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor av = super.visitArray(name);
				return new NumberAnnotationVisitor(av, resultDelegate);
			}

			@Override
			public void visit(String name, Object value) {
				super.visit(name, value);
				whenMatched(value, resultDelegate);
			}
		}
	}
}