package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.BasicAnnotationInfo;
import software.coley.recaf.info.member.BasicLocalVariable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.result.ClassReferenceResult;
import software.coley.recaf.services.search.result.MemberReferenceResult;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.visitors.IndexCountingMethodVisitor;

/**
 * Reference search implementation.
 *
 * @author Matt Coley
 */
public class ReferenceQuery implements JvmClassQuery {
	private final StringPredicate ownerPredicate;
	private final StringPredicate namePredicate;
	private final StringPredicate descriptorPredicate;
	private final boolean classRefOnly;


	/**
	 * Class reference query.
	 *
	 * @param ownerPredicate
	 * 		String matching predicate for comparison against reference owners.
	 */
	public ReferenceQuery(@Nonnull StringPredicate ownerPredicate) {
		this.ownerPredicate = ownerPredicate;
		this.namePredicate = null;
		this.descriptorPredicate = null;
		classRefOnly = true;
	}

	/**
	 * Member reference query.
	 * <p/>
	 * Do note that each target value is nullable/optional.
	 * Including only the owner and {@code null} for the name/desc will yield references to all members in the class.
	 * Including only the desc will yield references to all members of that desc in all classes.
	 *
	 * @param ownerPredicate
	 * 		String matching predicate for comparison against reference owners.
	 *        {@code null} to ignore matching against reference owner names.
	 * @param namePredicate
	 * 		String matching predicate for comparison against reference names.
	 *        {@code null} to ignore matching against reference names.
	 * @param descriptorPredicate
	 * 		String matching predicate for comparison against reference descriptors.
	 *        {@code null} to ignore matching against reference owner descriptors.
	 */
	public ReferenceQuery(@Nullable StringPredicate ownerPredicate,
	                      @Nullable StringPredicate namePredicate,
	                      @Nullable StringPredicate descriptorPredicate) {
		this.ownerPredicate = ownerPredicate;
		this.namePredicate = namePredicate;
		this.descriptorPredicate = descriptorPredicate;
		classRefOnly = false;
	}

	private boolean isClassRefMatch(@Nullable String className) {
		if (!classRefOnly || className == null || ownerPredicate == null) return false;
		return StringUtil.isNullOrEmpty(className) || ownerPredicate.match(className);
	}

	//@SuppressWarnings("DataFlowIssue") // The class-ref check addresses this
	private boolean isMemberRefMatch(@Nullable String owner, @Nullable String name, @Nullable String desc) {
		if (classRefOnly) return false;

		// The parameters are null if we only are searching against a type.
		// In these cases since we're comparing to a type, then any name/desc comparison should be ignored.
		if (name == null && namePredicate != null) return false;
		if (desc == null && descriptorPredicate != null) return false;

		// Check if match modes succeed.
		// If our query predicates are null, that field can skip comparison, and we move on to the next.
		// If all of our non-null query arguments match the given parameters, we have a match.
		if (ownerPredicate == null || StringUtil.isNullOrEmpty(owner) || ownerPredicate.match(owner))
			if (namePredicate == null || StringUtil.isNullOrEmpty(name) || namePredicate.match(name))
				return descriptorPredicate == null || StringUtil.isNullOrEmpty(desc) || descriptorPredicate.match(desc);

		return false;
	}

	@Nonnull
	private static String getInternalName(@Nonnull String classDesc) {
		return Type.getType(classDesc).getInternalName();
	}

	@Nonnull
	private static ClassReferenceResult.ClassReference cref(@Nonnull String name) {
		return new ClassReferenceResult.ClassReference(name);
	}

	@Nonnull
	private static MemberReferenceResult.MemberReference mref(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return new MemberReferenceResult.MemberReference(owner, name, desc);
	}

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return (resultSink, currentLocation, classInfo) -> {
			if (delegate != null)
				delegate.visit(resultSink, currentLocation, classInfo);
			classInfo.getClassReader().accept(new AsmReferenceClassVisitor(resultSink, currentLocation, classInfo), 0);
		};
	}

	/**
	 * Visits references in classes.
	 */
	private class AsmReferenceClassVisitor extends ClassVisitor {
		private final Logger logger = Logging.get(AsmReferenceClassVisitor.class);
		private final ResultSink resultSink;
		private final ClassPathNode classPath;
		private final JvmClassInfo classInfo;

		public AsmReferenceClassVisitor(@Nonnull ResultSink resultSink,
		                                @Nonnull ClassPathNode classPath,
		                                @Nonnull JvmClassInfo classInfo) {
			super(RecafConstants.getAsmVersion());
			this.resultSink = resultSink;
			this.classPath = classPath;
			this.classInfo = classInfo;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodMember methodMember = classInfo.getDeclaredMethod(name, desc);
			if (methodMember != null) {
				ClassMemberPathNode memberPath = classPath.child(methodMember);

				// Check exceptions
				if (exceptions != null)
					for (String exception : exceptions)
						if (isClassRefMatch(exception))
							resultSink.accept(memberPath.childThrows(exception), cref(exception));

				// Check descriptor components
				// - Only yield one match even if there are multiple class-refs in the desc
				Type methodType = Type.getMethodType(desc);
				String methodRetType = methodType.getReturnType().getInternalName();
				if (isClassRefMatch(methodRetType))
					resultSink.accept(memberPath, cref(methodRetType));
				else for (Type argumentType : methodType.getArgumentTypes())
					if (isClassRefMatch(argumentType.getInternalName())) {
						resultSink.accept(memberPath, cref(argumentType.getInternalName()));
						break;
					}

				// Visit method
				return new AsmReferenceMethodVisitor(mv, methodMember, resultSink, classPath);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", classInfo.getName(), name, desc);
				return mv;
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);
			FieldMember fieldMember = classInfo.getDeclaredField(name, desc);
			if (fieldMember != null) {
				ClassMemberPathNode memberPath = classPath.child(fieldMember);

				// Check descriptor
				String fieldType = getInternalName(desc);
				if (isClassRefMatch(fieldType))
					resultSink.accept(memberPath, cref(fieldType));

				// Visit field
				return new AsmReferenceFieldVisitor(fv, resultSink, memberPath);
			} else {
				logger.error("Failed to lookup field for query: {}.{}{}", classInfo.getName(), name, desc);
				return fv;
			}
		}
	}

	/**
	 * Visits references in methods.
	 */
	private class AsmReferenceMethodVisitor extends IndexCountingMethodVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final String ownerType;


		public AsmReferenceMethodVisitor(@Nullable MethodVisitor delegate,
		                                 @Nonnull MethodMember methodMember,
		                                 @Nonnull ResultSink resultSink,
		                                 @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.memberPath = classLocation.child(methodMember);

			ownerType = classLocation.getValue().getName();
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (isClassRefMatch(type)) {
				TypeInsnNode insn = new TypeInsnNode(opcode, type);
				resultSink.accept(memberPath.childInsn(insn, index), cref(type));
			}
			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			FieldInsnNode insn = new FieldInsnNode(opcode, owner, name, desc);

			// Check method ref
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(memberPath.childInsn(insn, index), mref(owner, name, desc));

			// Check types used in ref
			String fieldType = getInternalName(desc);
			if (isClassRefMatch(fieldType))
				resultSink.accept(memberPath.childInsn(insn, index), cref(fieldType));

			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			MethodInsnNode insn = new MethodInsnNode(opcode, owner, name, desc, isInterface);

			visitMethodLikeInsn(owner, name, desc, insn);

			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsmHandle, Object... bsmArgs) {
			InvokeDynamicInsnNode insn = new InvokeDynamicInsnNode(name, desc, bsmHandle, bsmArgs);

			visitMethodLikeInsn(ownerType, name, desc, insn);
			visitBsm(bsmHandle, bsmArgs, insn);

			super.visitInvokeDynamicInsn(name, desc, bsmHandle, bsmArgs);
		}

		@Override
		public void visitLdcInsn(Object value) {
			LdcInsnNode insn = new LdcInsnNode(value);

			visitArg(insn.cst, insn);

			super.visitLdcInsn(value);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int numDimensions) {
			if (Types.isValidDesc(desc)) {
				String type = getInternalName(desc);
				if (isClassRefMatch(type)) {
					MultiANewArrayInsnNode insn = new MultiANewArrayInsnNode(desc, numDimensions);
					resultSink.accept(memberPath.childInsn(insn, index), cref(type));
				}
			}
			super.visitMultiANewArrayInsn(desc, numDimensions);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			if (isClassRefMatch(type)) {
				resultSink.accept(memberPath.childCatch(type), cref(type));
			}
			super.visitTryCatchBlock(start, end, handler, type);
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			if (!Types.isValidDesc(desc) || Types.isPrimitive(desc)) {
				super.visitLocalVariable(name, desc, signature, start, end, index);
				return;
			}

			String type = getInternalName(desc);

			// Skip 'this' variables. Nobody cares that virtual methods have a type reference to themselves...
			if (index == 0 && name.equals("this") && type.equals(ownerType)) {
				super.visitLocalVariable(name, desc, signature, start, end, index);
				return;
			}

			if (isClassRefMatch(type)) {
				LocalVariable variable = new BasicLocalVariable(index, name, desc, signature);
				resultSink.accept(memberPath.childVariable(variable), cref(type));
			}

			super.visitLocalVariable(name, desc, signature, start, end, index);
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new AnnotationReferenceVisitor(super.visitAnnotationDefault(), true, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		private void visitBsm(@Nonnull Handle bsmHandle, @Nonnull Object[] bsmArgs, @Nonnull AbstractInsnNode insn) {
			// Visit the handle
			visitHandle(bsmHandle, insn);

			// Then all the args
			for (Object bsmArg : bsmArgs)
				visitArg(bsmArg, insn);
		}

		private void visitArg(@Nonnull Object arg, @Nonnull AbstractInsnNode insn) {
			switch (arg) {
				case Type typeArg -> visitType(typeArg, insn);
				case Handle handleArg -> visitHandle(handleArg, insn);
				case ConstantDynamic dynamicArg -> {
					int argCount = dynamicArg.getBootstrapMethodArgumentCount();
					Object[] args = new Object[argCount];
					for (int i = 0; i < argCount; i++)
						args[i] = dynamicArg.getBootstrapMethodArgument(i);
					visitBsm(dynamicArg.getBootstrapMethod(), args, insn);
				}
				default -> {
					// no-op
				}
			}
		}

		private void visitHandle(@Nonnull Handle handle, @Nonnull AbstractInsnNode insn) {
			// Check handle ref
			String handleDesc = handle.getDesc();
			if (isMemberRefMatch(handle.getOwner(), handle.getName(), handleDesc)) {
				resultSink.accept(memberPath.childInsn(insn, index),
						mref(handle.getOwner(), handle.getName(), handleDesc));
			}

			// Check types used in ref
			visitType(Type.getType(handle.getDesc()), insn);
		}

		private void visitMethodLikeInsn(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, @Nonnull AbstractInsnNode insn) {
			// Check method ref
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(memberPath.childInsn(insn, index), mref(owner, name, desc));

			// Check types used in ref
			Type methodType = Type.getMethodType(desc);
			visitType(methodType, insn);
		}

		private void visitType(@Nonnull Type type, @Nonnull AbstractInsnNode insn) {
			if (type.getSort() == Type.METHOD) {
				String methodRetType = type.getReturnType().getInternalName();
				if (isClassRefMatch(methodRetType))
					resultSink.accept(memberPath.childInsn(insn, index), cref(methodRetType));
				for (Type argumentType : type.getArgumentTypes()) {
					if (isClassRefMatch(argumentType.getInternalName()))
						resultSink.accept(memberPath.childInsn(insn, index), cref(argumentType.getInternalName()));
				}
			} else {
				String internalName = type.getInternalName();
				if (isClassRefMatch(internalName)) {
					resultSink.accept(memberPath.childInsn(insn, index), cref(internalName));
				}
			}
		}
	}

	/**
	 * Visits references in fields.
	 */
	private class AsmReferenceFieldVisitor extends FieldVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;

		public AsmReferenceFieldVisitor(@Nullable FieldVisitor delegate,
		                                @Nonnull ResultSink resultSink,
		                                @Nonnull ClassMemberPathNode memberPath) {
			super(RecafConstants.getAsmVersion(), delegate);
			this.resultSink = resultSink;
			this.memberPath = memberPath;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// Match annotation
			String type = getInternalName(desc);
			if (isClassRefMatch(type))
				resultSink.accept(memberPath, cref(type));

			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}
	}

	/**
	 * Visits references in annotations.
	 */
	private class AnnotationReferenceVisitor extends AnnotationVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentAnnoLocation;
		private final boolean visible;

		public AnnotationReferenceVisitor(@Nullable AnnotationVisitor delegate,
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

			// Match sub-annotation
			String type = getInternalName(descriptor);
			if (isClassRefMatch(type))
				resultSink.accept(currentAnnoLocation, cref(type));

			// Visit sub-annotation
			if (currentAnnoLocation.getValue() instanceof Annotated annotated) {
				AnnotationInfo annotationInfo = annotated.getAnnotations().stream()
						.filter(ai -> ai.getDescriptor().equals(descriptor))
						.findFirst()
						.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor));
				if (currentAnnoLocation instanceof ClassPathNode classPath) {
					return new AnnotationReferenceVisitor(av, visible, resultSink,
							classPath.child(annotationInfo));
				} else if (currentAnnoLocation instanceof ClassMemberPathNode memberPath) {
					return new AnnotationReferenceVisitor(av, visible, resultSink,
							memberPath.childAnnotation(annotationInfo));
				} else if (currentAnnoLocation instanceof AnnotationPathNode annotationPath) {
					return new AnnotationReferenceVisitor(av, visible, resultSink,
							annotationPath.child(annotationInfo));
				}
			}

			throw new IllegalStateException("Unsupported non-annotatable path: " + currentAnnoLocation);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor av = super.visitArray(name);
			return new AnnotationReferenceVisitor(av, visible, resultSink, currentAnnoLocation);
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			super.visitEnum(name, descriptor, value);

			// Match enum reference
			String owner = getInternalName(descriptor);
			if (isMemberRefMatch(owner, descriptor, value))
				resultSink.accept(currentAnnoLocation, mref(owner, value, descriptor));
		}
	}
}
