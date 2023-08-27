package me.coley.recaf.search.query;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.FieldInstruction;
import me.coley.recaf.assemble.ast.insn.IndyInstruction;
import me.coley.recaf.assemble.ast.insn.LdcInstruction;
import me.coley.recaf.assemble.ast.insn.MethodInstruction;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A query that looks for references to a member in all accessible locations.<br>
 * Not all parameters are required, for instance only including the {@link #owner} will
 * yield all references to the class.
 *
 * @author Matt Coley
 */
public class ReferenceQuery implements Query {
	private static final Logger logger = Logging.get(ReferenceQuery.class);
	private final String owner;
	private final String name;
	private final String desc;
	private final TextMatchMode mode;

	/**
	 * @param owner
	 * 		The class defining the referenced member.
	 * @param name
	 * 		The name of the referenced member.
	 * @param desc
	 * 		The type descriptor of the referenced member.
	 * @param mode
	 * 		The matching strategy of the query against the reference type texts.
	 */
	public ReferenceQuery(String owner, String name, String desc, TextMatchMode mode) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.mode = mode;
	}

	@Override
	public QueryVisitor createVisitor(Resource resource, QueryVisitor delegate) {
		return new RefClassVisitor(resource, delegate);
	}

	private void whenMatched(String owner, String name, String desc, Consumer<ResultBuilder> builderConsumer) {
		// The parameters are null if we only are searching against a type.
		// In these cases since we're comparing to a type, then any name/desc comparison should be ignored.
		if (name == null && this.name != null)
			return;
		if (desc == null && this.desc != null)
			return;
		// Check if match modes succeed.
		// If our query arguments are null, that field can skip comparison, and we move on to the next.
		// If all of our non-null query arguments match the given parameters, we have a match.
		if (StringUtil.isAnyNullOrEmpty(this.owner, owner) || mode.match(this.owner, owner)) {
			if (StringUtil.isAnyNullOrEmpty(this.name, name) || mode.match(this.name, name)) {
				if (StringUtil.isAnyNullOrEmpty(this.desc, desc) || mode.match(this.desc, desc)) {
					builderConsumer.accept(ResultBuilder.reference(owner, name, desc));
				}
			}
		}
	}

	@Override
	public String toString() {
		List<String> lines = new ArrayList<>();
		if (owner != null)
			lines.add("owner=" + owner);
		if (name != null)
			lines.add("name=" + name);
		if (desc != null)
			lines.add("desc=" + desc);
		return "References [" + String.join(", ", lines) + ']';
	}

	/**
	 * Base class visitor.
	 */
	private class RefClassVisitor extends QueryVisitor {
		public RefClassVisitor(Resource resource, QueryVisitor delegate) {
			super(resource, delegate);
		}

		@Override
		public void visitFile(FileInfo fileInfo) {
			// no-op
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodInfo methodInfo = currentClass.findMethod(name, desc);
			if (methodInfo != null) {
				return new RefMethodVisitor(mv, methodInfo);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", currentClass.getName(), name, desc);
				return mv;
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			FieldInfo fieldInfo = currentClass.findField(name, desc);
			if (fieldInfo != null) {
				return new RefFieldVisitor(fv, fieldInfo);
			} else {
				logger.error("Failed to lookup field for query: {}.{}{}", currentClass.getName(), name, desc);
				return fv;
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new RefClassAV(av, currentClass.getName());
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new RefClassAV(av, currentClass.getName());
		}

		/**
		 * Sub visitor for fields.
		 * <br>
		 * Its only real purpose is to delegate to searching in field annotations.
		 */
		private class RefFieldVisitor extends FieldVisitor {
			private final FieldInfo fieldInfo;

			protected RefFieldVisitor(FieldVisitor fv, FieldInfo fieldInfo) {
				super(RecafConstants.getAsmVersion(), fv);
				this.fieldInfo = fieldInfo;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				AnnotationVisitor av = super.visitAnnotation(desc, visible);
				String type = desc.substring(1, desc.length() - 1);
				whenMatched(type, null, null,
						builder -> addFieldAnno(builder, fieldInfo, type));
				return new RefFieldAV(av, fieldInfo);
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				String type = desc.substring(1, desc.length() - 1);
				whenMatched(type, null, null,
						builder -> addFieldAnno(builder, fieldInfo, type));
				return new RefFieldAV(av, fieldInfo);
			}
		}

		/**
		 * Sub visitor for methods.
		 * <br>
		 * Most reference results are instructions, so most of the stuff found will be done so in here.
		 * Also delegates to searching in method annotations.
		 */
		private class RefMethodVisitor extends MethodVisitor {
			private final MethodInfo methodInfo;

			public RefMethodVisitor(MethodVisitor delegate, MethodInfo methodInfo) {
				super(RecafConstants.getAsmVersion(), delegate);
				this.methodInfo = methodInfo;
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				AnnotationVisitor av = super.visitAnnotationDefault();
				return new RefMethodAV(av, methodInfo);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				AnnotationVisitor av = super.visitAnnotation(desc, visible);
				if(desc.isEmpty()) return av;
				String type = desc.substring(1, desc.length() - 1);
				whenMatched(type, null, null,
						builder -> addMethodAnno(builder, methodInfo, type));
				return new RefMethodAV(av, methodInfo);
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
				if(desc.isEmpty()) return av;
				String type = desc.substring(1, desc.length() - 1);
				whenMatched(type, null, null,
						builder -> addMethodAnno(builder, methodInfo, type));
				return new RefMethodAV(av, methodInfo);
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
				AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
				if(desc.isEmpty()) return av;
				String type = desc.substring(1, desc.length() - 1);
				whenMatched(type, null, null,
						builder -> addMethodAnno(builder, methodInfo, type));
				return new RefMethodAV(av, methodInfo);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, owner, name, desc);
				whenMatched(owner, name, desc,
						builder -> addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(),
								new FieldInstruction(opcode, owner, name, desc)));
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
				whenMatched(owner, name, desc,
						builder -> addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(),
								new MethodInstruction(opcode, owner, name, desc, isInterface)));
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String desc, Handle bsmHandle,
											   Object... bootstrapMethodArguments) {
				super.visitInvokeDynamicInsn(name, desc, bsmHandle, bootstrapMethodArguments);
				IndyInstruction indy = new IndyInstruction(Opcodes.INVOKEDYNAMIC, name, desc,
						new HandleInfo(bsmHandle),
						Arrays.stream(bootstrapMethodArguments)
								.map(arg -> IndyInstruction.BsmArg.of(IndyInstruction.BsmArg::new, arg))
								.collect(Collectors.toList()));
				whenMatched(bsmHandle.getOwner(), bsmHandle.getName(), bsmHandle.getDesc(),
						builder -> addMethodInsn(builder, methodInfo.getName(),
								methodInfo.getDescriptor(), indy));
				for (Object bsmArg : bootstrapMethodArguments) {
					if (bsmArg instanceof Handle) {
						Handle handle = (Handle) bsmArg;
						whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
								builder -> addMethodInsn(builder, methodInfo.getName(),
										methodInfo.getDescriptor(), indy));
					} else if (bsmArg instanceof ConstantDynamic) {
						ConstantDynamic dynamic = (ConstantDynamic) bsmArg;
						Handle handle = dynamic.getBootstrapMethod();
						whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
								builder -> addMethodInsn(builder, methodInfo.getName(),
										methodInfo.getDescriptor(), indy));
					}
				}
			}

			@Override
			public void visitLdcInsn(Object value) {
				super.visitLdcInsn(value);
				if (value instanceof Handle) {
					Handle handle = (Handle) value;
					whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
							builder -> addMethodInsn(builder, methodInfo.getName(),
									methodInfo.getDescriptor(), LdcInstruction.of(value)));
				} else if (value instanceof Type) {
					Type type = (Type) value;
					handleLdcType(type, type);
				}
			}

			private void handleLdcType(Type original, Type type) {
				if (type.getSort() == Type.OBJECT)
					whenMatched(type.getInternalName(), null, null,
							builder -> addMethodInsn(builder, methodInfo.getName(),
									methodInfo.getDescriptor(), LdcInstruction.of(original)));
				else if (type.getSort() == Type.ARRAY)
					handleLdcType(original, type.getElementType());
				else if (type.getSort() == Type.METHOD) {
					handleLdcType(original, type.getReturnType());
					for (Type arg : type.getArgumentTypes()) {
						handleLdcType(original, arg);
					}
				}
			}
		}

		private abstract class RefAnnotationVisitor extends AnnotationVisitor {
			protected RefAnnotationVisitor(AnnotationVisitor av) {
				super(RecafConstants.getAsmVersion(), av);
			}

			protected abstract void matchAnnoRef(String internalName);

			protected abstract RefAnnotationVisitor wrapThis(AnnotationVisitor av);

			@Override
			public void visit(String name, Object value) {
				super.visit(name, value);
				if (value instanceof Type) {
					Type type = (Type) value;
					if (type.getSort() == Type.ARRAY)
						type = type.getElementType();
					matchAnnoRef(type.getInternalName());
				}
			}

			@Override
			public void visitEnum(String name, String desc, String value) {
				super.visitEnum(name, desc, value);
				matchAnnoRef(desc.substring(1, desc.length() - 1));
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String desc) {
				AnnotationVisitor av = super.visitAnnotation(name, desc);
				matchAnnoRef(desc.substring(1, desc.length() - 1));
				return wrapThis(av);
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor av = super.visitArray(name);
				return wrapThis(av);
			}
		}

		private class RefClassAV extends RefAnnotationVisitor {
			private final String className;

			protected RefClassAV(AnnotationVisitor av, String className) {
				super(av);
				this.className = className;
			}

			@Override
			protected void matchAnnoRef(String internalName) {
				whenMatched(internalName, null, null,
						builder -> addClassAnno(builder, internalName));
			}

			@Override
			protected RefAnnotationVisitor wrapThis(AnnotationVisitor av) {
				return new RefClassAV(av, className);
			}
		}

		private class RefFieldAV extends RefAnnotationVisitor {
			private final FieldInfo info;

			protected RefFieldAV(AnnotationVisitor av, FieldInfo info) {
				super(av);
				this.info = info;
			}

			@Override
			protected void matchAnnoRef(String internalName) {
				whenMatched(internalName, null, null,
						builder -> addFieldAnno(builder, info, internalName));
			}

			@Override
			protected RefAnnotationVisitor wrapThis(AnnotationVisitor av) {
				return new RefFieldAV(av, info);
			}
		}

		private class RefMethodAV extends RefAnnotationVisitor {
			private final MethodInfo info;

			protected RefMethodAV(AnnotationVisitor av, MethodInfo info) {
				super(av);
				this.info = info;
			}

			@Override
			protected void matchAnnoRef(String internalName) {
				whenMatched(internalName, null, null,
						builder -> addMethodAnno(builder, info, internalName));
			}

			@Override
			protected RefAnnotationVisitor wrapThis(AnnotationVisitor av) {
				return new RefMethodAV(av, info);
			}
		}
	}
}