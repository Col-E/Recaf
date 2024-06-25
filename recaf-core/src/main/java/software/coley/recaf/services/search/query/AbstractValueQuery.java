package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
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
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.util.visitors.IndexCountingMethodVisitor;

/**
 * General value search.
 *
 * @author Matt Coley
 * @see StringQuery
 * @see NumberQuery
 */
public abstract class AbstractValueQuery implements JvmClassQuery, FileQuery {
	private static final Number[] OP_TO_VALUE = {
			0, // NOP
			0, // NULL
			-1, 0, 1, 2, 3, 4, 5, // ICONST_X
			0L, 1L, // LCONST_X
			0F, 1F, 2F, // FCONST_X
			0D, 1D // DCONST_X
	};

	// TODO: Implement android query when android capabilities are fleshed out enough to have comparable
	//    search capabilities in method code

	protected abstract boolean isMatch(Object value);

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return new JvmVisitor(delegate);
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
			} else {
				logger.error("Failed to lookup field for query: {}.{} {}", classInfo.getName(), name, desc);
				return fv;
			}
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodMember methodMember = classInfo.getDeclaredMethod(name, desc);
			if (methodMember != null) {
				return new AsmMethodValueVisitor(mv, methodMember, resultSink, classPath);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", classInfo.getName(), name, desc);
				return mv;
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			AnnotationInfo annotationInfo = classInfo.getAnnotations().stream()
					.filter(ai -> ai.getDescriptor().equals(desc))
					.findFirst()
					.orElseGet(() -> new BasicAnnotationInfo(visible, desc));
			return new AnnotationValueVisitor(av, visible, resultSink,
					classPath.child(annotationInfo));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationInfo annotationInfo = classInfo.getAnnotations().stream()
					.filter(ai -> ai.getDescriptor().equals(desc))
					.findFirst()
					.orElseGet(() -> new BasicAnnotationInfo(visible, desc));
			return new AnnotationValueVisitor(av, visible, resultSink,
					classPath.child(annotationInfo
							.withTypeInfo(typeRef, typePath)));
		}
	}

	/**
	 * Visits values in fields.
	 */
	private class AsmFieldValueVisitor extends FieldVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;

		public AsmFieldValueVisitor(@Nullable FieldVisitor delegate,
									@Nonnull FieldMember fieldMember,
									@Nonnull ResultSink resultSink,
									@Nonnull ClassPathNode classLocation) {
			super(RecafConstants.getAsmVersion(), delegate);
			this.resultSink = resultSink;
			this.memberPath = classLocation.child(fieldMember);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}
	}

	/**
	 * Visits values in methods.
	 */
	private class AsmMethodValueVisitor extends IndexCountingMethodVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;

		public AsmMethodValueVisitor(@Nullable MethodVisitor delegate,
									 @Nonnull MethodMember methodMember,
									 @Nonnull ResultSink resultSink,
									 @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
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
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			AnnotationVisitor av = super.visitAnnotationDefault();
			return new AnnotationValueVisitor(av, true, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)));
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc,
														 boolean visible) {
			AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
															  Label[] start, Label[] end, int[] index,
															  String desc, boolean visible) {
			AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end,
					index, desc, visible);
			return new AnnotationValueVisitor(av, visible, resultSink,
					memberPath.childAnnotation(new BasicAnnotationInfo(visible, desc)
							.withTypeInfo(typeRef, typePath)));
		}
	}

	/**
	 * Visits values in annotations.
	 */
	private class AnnotationValueVisitor extends AnnotationVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentAnnoLocation;
		private final boolean visible;

		public AnnotationValueVisitor(@Nullable AnnotationVisitor delegate,
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
				if (currentAnnoLocation instanceof ClassPathNode classPath) {
					return new AnnotationValueVisitor(av, visible, resultSink,
							classPath.child(annotationInfo));
				} else if (currentAnnoLocation instanceof ClassMemberPathNode memberPath) {
					return new AnnotationValueVisitor(av, visible, resultSink,
							memberPath.childAnnotation(annotationInfo));
				} else if (currentAnnoLocation instanceof AnnotationPathNode annotationPath) {
					return new AnnotationValueVisitor(av, visible, resultSink,
							annotationPath.child(annotationInfo));
				}
			}
			throw new IllegalStateException("Unsupported non-annotatable path: " + currentAnnoLocation);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor av = super.visitArray(name);
			return new AnnotationValueVisitor(av, visible, resultSink, currentAnnoLocation);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			if (isMatch(value))
				resultSink.accept(currentAnnoLocation, value);
		}
	}
}
