package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.BoolConstant;
import me.darknet.dex.tree.definitions.constant.ByteConstant;
import me.darknet.dex.tree.definitions.constant.CharConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.DoubleConstant;
import me.darknet.dex.tree.definitions.constant.FloatConstant;
import me.darknet.dex.tree.definitions.constant.IntConstant;
import me.darknet.dex.tree.definitions.constant.LongConstant;
import me.darknet.dex.tree.definitions.constant.ShortConstant;
import me.darknet.dex.tree.definitions.constant.StringConstant;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstStringInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.visitor.DexAnnotationVisitor;
import me.darknet.dex.tree.visitor.DexClassVisitor;
import me.darknet.dex.tree.visitor.DexCodeVisitor;
import me.darknet.dex.tree.visitor.DexConstantVisitor;
import me.darknet.dex.tree.visitor.DexFieldVisitor;
import me.darknet.dex.tree.visitor.DexMethodVisitor;
import me.darknet.dex.tree.visitor.DexTreeWalker;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.BasicAnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.AndroidClassSearchVisitor;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.util.visitors.IndexCountingMethodVisitor;

import java.util.Objects;

import static software.coley.recaf.util.NumberUtil.isNonZero;

/**
 * General value search.
 *
 * @author Matt Coley
 * @see StringQuery
 * @see NumberQuery
 */
public abstract class AbstractValueQuery implements AndroidClassQuery, JvmClassQuery, FileQuery {
	private static final Number[] OP_TO_VALUE = {
			0, // NOP
			0, // NULL
			-1, 0, 1, 2, 3, 4, 5, // ICONST_X
			0L, 1L, // LCONST_X
			0F, 1F, 2F, // FCONST_X
			0D, 1D // DCONST_X
	};

	protected abstract boolean isMatch(Object value);

	@Nonnull
	@Override
	public AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate) {
		return new AndroidVisitor(delegate);
	}

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return new JvmVisitor(delegate);
	}

	/**
	 * Points {@link #visitor(AndroidClassSearchVisitor)} to {@link DexClassValueVisitor}
	 */
	private class AndroidVisitor implements AndroidClassSearchVisitor {
		private final AndroidClassSearchVisitor delegate;

		public AndroidVisitor(@Nullable AndroidClassSearchVisitor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void visit(@Nonnull ResultSink resultSink, @Nonnull ClassPathNode classPath, @Nonnull AndroidClassInfo classInfo) {
			if (delegate != null) delegate.visit(resultSink, classPath, classInfo);

			DexTreeWalker.accept(classInfo.getBackingDefinition(), new DexClassValueVisitor(resultSink, classPath, classInfo));
		}
	}

	/**
	 * Visits values in classes.
	 */
	private class DexClassValueVisitor extends DexClassVisitor {
		private final Logger logger = Logging.get(DexClassValueVisitor.class);
		private final ResultSink resultSink;
		private final ClassPathNode classPath;
		private final AndroidClassInfo classInfo;

		public DexClassValueVisitor(@Nonnull ResultSink resultSink,
		                            @Nonnull ClassPathNode classPath,
		                            @Nonnull AndroidClassInfo classInfo) {
			this.resultSink = resultSink;
			this.classPath = classPath;
			this.classInfo = classInfo;
		}

		@Nullable
		@Override
		public DexFieldVisitor visitField(@Nonnull me.darknet.dex.tree.definitions.FieldMember field) {
			DexFieldVisitor fv = super.visitField(field);

			String name = field.getName();
			String desc = field.getType().descriptor();

			FieldMember fieldMember = classInfo.getDeclaredField(name, desc);
			if (fieldMember != null)
				return new DexFieldValueVisitor(fv, fieldMember, resultSink, classPath);

			logger.error("Failed to lookup field for query: {}.{} {}", classInfo.getName(), name, desc);
			return fv;
		}

		@Nullable
		@Override
		public DexMethodVisitor visitMethod(@Nonnull me.darknet.dex.tree.definitions.MethodMember method) {
			DexMethodVisitor mv = super.visitMethod(method);

			String name = method.getName();
			String desc = method.getType().descriptor();

			MethodMember methodMember = classInfo.getDeclaredMethod(name, desc);
			if (methodMember != null)
				return new DexMethodValueVisitor(mv, methodMember, resultSink, classPath);

			logger.error("Failed to lookup method for query: {}.{} {}", classInfo.getName(), name, desc);
			return mv;
		}

		@Override
		public @Nullable DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			AnnotationInfo annotationInfo = findAnnotation(classInfo, visible, part.type().descriptor());

			return new DexAnnotationValueVisitor(av, visible, resultSink,
					classPath.child(annotationInfo));
		}
	}

	/**
	 * Visits values in fields.
	 */
	private class DexFieldValueVisitor extends DexFieldVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final FieldMember fieldMember;

		public DexFieldValueVisitor(@Nullable DexFieldVisitor delegate,
		                            @Nonnull FieldMember fieldMember,
		                            @Nonnull ResultSink resultSink,
		                            @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.fieldMember = fieldMember;
			this.memberPath = classLocation.child(fieldMember);
		}

		@Override
		@Nullable
		public DexConstantVisitor visitStaticValue(@Nonnull Constant value) {
			DexConstantVisitor cv = super.visitStaticValue(value);
			return new DexConstantValueVisitor(cv, resultSink, memberPath);
		}

		@Override
		public @Nullable DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			AnnotationInfo annotationInfo = findAnnotation(fieldMember, visible, part.type().descriptor());

			return new DexAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}
	}

	/**
	 * Visits values in methods.
	 */
	private class DexMethodValueVisitor extends DexMethodVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final MethodMember methodMember;

		public DexMethodValueVisitor(@Nullable DexMethodVisitor delegate,
		                             @Nonnull MethodMember methodMember,
		                             @Nonnull ResultSink resultSink,
		                             @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.methodMember = methodMember;
			this.memberPath = classLocation.child(methodMember);
		}

		@Override
		public @Nullable DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			AnnotationInfo annotationInfo = findAnnotation(methodMember, visible, part.type().descriptor());

			return new DexAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}

		@Nonnull
		@Override
		public DexCodeVisitor visitCode(@Nonnull Code code) {
			DexCodeVisitor cv = super.visitCode(code);
			return new DexCodeVisitor() {
				private int insnIndex;

				@Override
				public void visitInstruction(@Nonnull Instruction instruction) {
					insnIndex++;
				}

				@Override
				public void visitConstWideInstruction(@Nonnull ConstWideInstruction instruction) {
					long value = instruction.value();
					if (isMatch(value))
						resultSink.accept(memberPath.childInsn(instruction, insnIndex), value);
				}

				@Override
				public void visitConstStringInstruction(@Nonnull ConstStringInstruction instruction) {
					String value = instruction.string();
					if (isMatch(value))
						resultSink.accept(memberPath.childInsn(instruction, insnIndex), value);
				}

				@Override
				public void visitConstInstruction(@Nonnull ConstInstruction instruction) {
					int value = instruction.value();
					if (isMatch(value))
						resultSink.accept(memberPath.childInsn(instruction, insnIndex), value);
				}

				@Override
				public void visitBinaryLiteralInstruction(@Nonnull BinaryLiteralInstruction instruction) {
					int value = instruction.constant();
					if (isMatch(value))
						resultSink.accept(memberPath.childInsn(instruction, insnIndex), value);
				}
			};
		}
	}

	/**
	 * Visits values in annotations.
	 */
	private class DexAnnotationValueVisitor extends DexAnnotationVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentAnnoLocation;
		private final boolean visible;

		public DexAnnotationValueVisitor(@Nullable DexAnnotationVisitor delegate,
		                                 boolean visible,
		                                 @Nonnull ResultSink resultSink,
		                                 @Nonnull PathNode<?> currentAnnoLocation) {
			super(delegate);
			this.visible = visible;
			this.resultSink = resultSink;
			this.currentAnnoLocation = currentAnnoLocation;
		}

		@Nullable
		@Override
		public DexConstantVisitor visitElement(@Nonnull String name, @Nonnull Constant value) {
			DexConstantVisitor dcv = super.visitElement(name, value);
			return new DexConstantValueVisitor(dcv, resultSink, currentAnnoLocation);
		}
	}

	/**
	 * Visits constants in dex structures.
	 */
	private class DexConstantValueVisitor extends DexConstantVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentLocation;

		public DexConstantValueVisitor(@Nullable DexConstantVisitor delegate,
		                               @Nonnull ResultSink resultSink,
		                               @Nonnull PathNode<?> currentLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.currentLocation = currentLocation;
		}

		@Nullable
		@Override
		public DexAnnotationVisitor visitAnnotationConstant(@Nonnull AnnotationConstant constant) {
			DexAnnotationVisitor av = super.visitAnnotationConstant(constant);

			boolean visible = true;
			String descriptor = constant.annotation().type().descriptor();

			if (currentLocation.getValue() instanceof Annotated annotated) {
				AnnotationInfo annotationInfo = annotated.getAnnotations().stream()
						.filter(ai -> ai.getDescriptor().equals(descriptor))
						.findFirst()
						.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor));
				switch (currentLocation) {
					case ClassPathNode classPath -> {
						return new DexAnnotationValueVisitor(av, visible, resultSink,
								classPath.child(annotationInfo));
					}
					case ClassMemberPathNode memberPath -> {
						return new DexAnnotationValueVisitor(av, visible, resultSink,
								memberPath.childAnnotation(annotationInfo));
					}
					case AnnotationPathNode annotationPath -> {
						return new DexAnnotationValueVisitor(av, visible, resultSink,
								annotationPath.child(annotationInfo));
					}
					default -> {}
				}
			}
			return new DexAnnotationValueVisitor(av, true, resultSink, currentLocation);
		}

		@Nullable
		@Override
		public DexConstantVisitor visitArrayConstant(@Nonnull ArrayConstant constant) {
			DexConstantVisitor cv = super.visitArrayConstant(constant);
			return new DexConstantValueVisitor(cv, resultSink, currentLocation);
		}

		@Override
		public void visitBoolConstant(@Nonnull BoolConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitByteConstant(@Nonnull ByteConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitCharConstant(@Nonnull CharConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitDoubleConstant(@Nonnull DoubleConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitFloatConstant(@Nonnull FloatConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitIntConstant(@Nonnull IntConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitLongConstant(@Nonnull LongConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitShortConstant(@Nonnull ShortConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}

		@Override
		public void visitStringConstant(@Nonnull StringConstant constant) {
			var value = constant.value();
			if (isMatch(value))
				resultSink.accept(currentLocation, value);
		}
	}

	/**
	 * Points {@link #visitor(JvmClassSearchVisitor)} to {@link AsmClassValueVisitor}
	 */
	private class JvmVisitor implements JvmClassSearchVisitor {
		private final JvmClassSearchVisitor delegate;

		private JvmVisitor(@Nullable JvmClassSearchVisitor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void visit(@Nonnull ResultSink resultSink,
		                  @Nonnull ClassPathNode classPath,
		                  @Nonnull JvmClassInfo classInfo) {
			if (delegate != null) delegate.visit(resultSink, classPath, classInfo);

			classInfo.getClassReader().accept(new AsmClassValueVisitor(resultSink, classPath, classInfo), 0);
		}
	}

	/**
	 * Visits values in classes.
	 */
	private class AsmClassValueVisitor extends ClassVisitor {
		private final Logger logger = Logging.get(AsmClassValueVisitor.class);
		private final ResultSink resultSink;
		private final ClassPathNode classPath;
		private final JvmClassInfo classInfo;

		protected AsmClassValueVisitor(@Nonnull ResultSink resultSink,
		                               @Nonnull ClassPathNode classPath,
		                               @Nonnull JvmClassInfo classInfo) {
			super(RecafConstants.getAsmVersion());
			this.resultSink = resultSink;
			this.classPath = classPath;
			this.classInfo = classInfo;
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			FieldMember fieldMember = classInfo.getDeclaredField(name, desc);
			if (fieldMember != null) {
				if (isMatch(value))
					resultSink.accept(classPath.child(fieldMember), value);
				return new AsmFieldValueVisitor(fv, fieldMember, resultSink, classPath);
			}

			logger.error("Failed to lookup field for query: {}.{} {}", classInfo.getName(), name, desc);
			return fv;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodMember methodMember = classInfo.getDeclaredMethod(name, desc);
			if (methodMember != null)
				return new AsmMethodValueVisitor(mv, methodMember, resultSink, classPath);

			logger.error("Failed to lookup method for query: {}.{}{}", classInfo.getName(), name, desc);
			return mv;

		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			AnnotationInfo annotationInfo = findAnnotation(classInfo, visible, desc);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					classPath.child(annotationInfo));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationInfo annotationInfo = findTypeAnnotation(classInfo, visible, desc, typeRef, typePath);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					classPath.child(annotationInfo));
		}
	}

	/**
	 * Visits values in fields.
	 */
	private class AsmFieldValueVisitor extends FieldVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final FieldMember fieldMember;

		public AsmFieldValueVisitor(@Nullable FieldVisitor delegate,
		                            @Nonnull FieldMember fieldMember,
		                            @Nonnull ResultSink resultSink,
		                            @Nonnull ClassPathNode classLocation) {
			super(RecafConstants.getAsmVersion(), delegate);
			this.resultSink = resultSink;
			this.fieldMember = fieldMember;
			this.memberPath = classLocation.child(fieldMember);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			AnnotationInfo annotationInfo = findAnnotation(fieldMember, visible, desc);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationInfo annotationInfo = findTypeAnnotation(fieldMember, visible, desc, typeRef, typePath);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}
	}

	/**
	 * Visits values in methods.
	 */
	private class AsmMethodValueVisitor extends IndexCountingMethodVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final MethodMember methodMember;

		public AsmMethodValueVisitor(@Nullable MethodVisitor delegate,
		                             @Nonnull MethodMember methodMember,
		                             @Nonnull ResultSink resultSink,
		                             @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.methodMember = methodMember;
			this.memberPath = classLocation.child(methodMember);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsmHandle,
		                                   Object... bsmArgs) {
			super.visitInvokeDynamicInsn(name, desc, bsmHandle, bsmArgs);
			for (Object bsmArg : bsmArgs) {
				if (isMatch(bsmArg)) {
					InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode(name, desc, bsmHandle, bsmArgs);
					resultSink.accept(memberPath.childInsn(indy, index), bsmArg);
				}
			}
		}

		@Override
		public void visitInsn(int opcode) {
			super.visitInsn(opcode);
			if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
				Number value = OP_TO_VALUE[opcode];
				if (isMatch(value))
					resultSink.accept(memberPath.childInsn(new InsnNode(opcode), index), value);
			}
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			super.visitIntInsn(opcode, operand);
			if (opcode != Opcodes.NEWARRAY && isMatch(operand))
				resultSink.accept(memberPath.childInsn(new IntInsnNode(opcode, operand), index), operand);
		}

		@Override
		public void visitLdcInsn(Object value) {
			super.visitLdcInsn(value);
			if (isMatch(value))
				resultSink.accept(memberPath.childInsn(new LdcInsnNode(value), index), value);
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			super.visitLookupSwitchInsn(dflt, keys, labels);
			for (int key : keys) {
				if (isMatch(key)) {
					resultSink.accept(memberPath.childInsn(new InsnNode(Opcodes.LOOKUPSWITCH), index), key);
				}
			}
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			super.visitTableSwitchInsn(min, max, dflt, labels);
			for (int i = min; i <= max; i++) {
				if (isMatch(i)) {
					resultSink.accept(memberPath.childInsn(new InsnNode(Opcodes.TABLESWITCH), index), i);
				}
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			AnnotationInfo annotationInfo = findAnnotation(methodMember, visible, desc);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationInfo annotationInfo = findTypeAnnotation(methodMember, visible, desc, typeRef, typePath);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			AnnotationVisitor av = super.visitAnnotationDefault();
			return new AsmAnnotationValueVisitor(av, true, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)));
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc,
		                                                 boolean visible) {
			AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
		                                                      Label[] start, Label[] end, int[] index,
		                                                      String desc, boolean visible) {
			AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end,
					index, desc, visible);
			return new AsmAnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}
	}

	/**
	 * Visits values in annotations.
	 */
	private class AsmAnnotationValueVisitor extends AnnotationVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentAnnoLocation;
		private final boolean visible;

		public AsmAnnotationValueVisitor(@Nullable AnnotationVisitor delegate,
		                                 boolean visible,
		                                 @Nonnull ResultSink resultSink,
		                                 @Nonnull PathNode<?> currentAnnoLocation) {
			super(RecafConstants.getAsmVersion(), delegate);
			this.visible = visible;
			this.resultSink = resultSink;
			this.currentAnnoLocation = currentAnnoLocation;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			AnnotationVisitor av = super.visitAnnotation(name, descriptor);
			if (currentAnnoLocation.getValue() instanceof Annotated annotated) {
				AnnotationInfo annotationInfo = annotated.getAnnotations().stream()
						.filter(ai -> ai.getDescriptor().equals(descriptor))
						.findFirst()
						.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor));
				switch (currentAnnoLocation) {
					case ClassPathNode classPath -> {
						return new AsmAnnotationValueVisitor(av, visible, resultSink,
								classPath.child(annotationInfo));
					}
					case ClassMemberPathNode memberPath -> {
						return new AsmAnnotationValueVisitor(av, visible, resultSink,
								memberPath.childAnnotation(annotationInfo));
					}
					case AnnotationPathNode annotationPath -> {
						return new AsmAnnotationValueVisitor(av, visible, resultSink,
								annotationPath.child(annotationInfo));
					}
					default -> {}
				}
			}
			throw new IllegalStateException("Unsupported non-annotatable path: " + currentAnnoLocation);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor av = super.visitArray(name);
			return new AsmAnnotationValueVisitor(av, visible, resultSink, currentAnnoLocation);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			if (isMatch(value))
				resultSink.accept(currentAnnoLocation, value);
		}
	}

	@Nonnull
	private static AnnotationInfo findAnnotation(@Nonnull Annotated annotated, boolean visible, @Nonnull String descriptor) {
		return annotated.getAnnotations().stream()
				.filter(ai -> ai.getDescriptor().equals(descriptor))
				.findFirst()
				.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor));
	}

	@Nonnull
	private static AnnotationInfo findTypeAnnotation(@Nonnull Annotated annotated, boolean visible, @Nonnull String descriptor,
	                                                 int typeRef, @Nullable TypePath typePath) {
		return annotated.getTypeAnnotations().stream()
				.filter(ai -> ai.getDescriptor().equals(descriptor))
				.filter(ai -> ai.getTypeRef() == typeRef)
				.filter(ai -> Objects.equals(ai.getTypePath(), typePath))
				.map(ai -> (AnnotationInfo) ai)
				.findFirst()
				.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor).withTypeInfo(typeRef, typePath));
	}
}
