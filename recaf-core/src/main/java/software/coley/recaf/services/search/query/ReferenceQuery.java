package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.EnumConstant;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.constant.MemberConstant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.ReferenceType;
import me.darknet.dex.tree.visitor.DexAnnotationVisitor;
import me.darknet.dex.tree.visitor.DexClassVisitor;
import me.darknet.dex.tree.visitor.DexCodeVisitor;
import me.darknet.dex.tree.visitor.DexConstantVisitor;
import me.darknet.dex.tree.visitor.DexFieldVisitor;
import me.darknet.dex.tree.visitor.DexMethodVisitor;
import me.darknet.dex.tree.visitor.DexTreeWalker;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
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
import software.coley.recaf.services.search.AndroidClassSearchVisitor;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.result.ClassReference;
import software.coley.recaf.services.search.result.MemberReference;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.visitors.IndexCountingMethodVisitor;

import static software.coley.recaf.util.NumberUtil.isNonZero;

/**
 * Reference search implementation.
 *
 * @author Matt Coley
 */
public class ReferenceQuery implements AndroidClassQuery, JvmClassQuery {
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
	 * <p>
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
	 *        {@code null} to ignore matching against reference descriptors.
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

	private void visitAsmMemberReference(@Nonnull String owner,
	                                     @Nonnull String name,
	                                     @Nonnull String descriptor,
	                                     @Nonnull ResultSink resultSink,
	                                     @Nonnull PathNode<?> location) {
		if (isMemberRefMatch(owner, name, descriptor))
			resultSink.accept(location, mref(owner, name, descriptor));
	}

	private void visitAsmType(@Nonnull Type type,
	                          @Nonnull ResultSink resultSink,
	                          @Nonnull PathNode<?> location) {
		if (type.getSort() == Type.METHOD) {
			visitAsmMethodType(type, resultSink, location);
			return;
		}

		String internalName = type.getInternalName();
		if (isClassRefMatch(internalName))
			resultSink.accept(location, cref(internalName));
	}

	private void visitAsmMethodType(@Nonnull Type methodType,
	                                @Nonnull ResultSink resultSink,
	                                @Nonnull PathNode<?> location) {
		visitAsmType(methodType.getReturnType(), resultSink, location);
		for (Type argumentType : methodType.getArgumentTypes())
			visitAsmType(argumentType, resultSink, location);
	}

	private void visitFirstAsmMethodTypeMatch(@Nonnull Type methodType,
	                                          @Nonnull ResultSink resultSink,
	                                          @Nonnull PathNode<?> location) {
		String returnType = methodType.getReturnType().getInternalName();
		if (isClassRefMatch(returnType)) {
			resultSink.accept(location, cref(returnType));
			return;
		}

		for (Type argumentType : methodType.getArgumentTypes()) {
			String internalName = argumentType.getInternalName();
			if (isClassRefMatch(internalName)) {
				resultSink.accept(location, cref(internalName));
				return;
			}
		}
	}

	private void visitAsmHandle(@Nonnull Handle handle,
	                            @Nonnull ResultSink resultSink,
	                            @Nonnull PathNode<?> location) {
		visitAsmMemberReference(handle.getOwner(), handle.getName(), handle.getDesc(), resultSink, location);
		visitAsmType(Type.getType(handle.getDesc()), resultSink, location);
	}

	private void visitAsmBootstrap(@Nonnull Handle bootstrapHandle,
	                               @Nonnull Object[] bootstrapArguments,
	                               @Nonnull ResultSink resultSink,
	                               @Nonnull PathNode<?> location) {
		visitAsmHandle(bootstrapHandle, resultSink, location);
		for (Object argument : bootstrapArguments)
			visitAsmConstant(argument, resultSink, location);
	}

	private void visitAsmConstant(@Nonnull Object value,
	                              @Nonnull ResultSink resultSink,
	                              @Nonnull PathNode<?> location) {
		switch (value) {
			case Type type -> visitAsmType(type, resultSink, location);
			case Handle handle -> visitAsmHandle(handle, resultSink, location);
			case ConstantDynamic dynamic -> {
				int count = dynamic.getBootstrapMethodArgumentCount();
				Object[] arguments = new Object[count];
				for (int i = 0; i < count; i++)
					arguments[i] = dynamic.getBootstrapMethodArgument(i);
				visitAsmBootstrap(dynamic.getBootstrapMethod(), arguments, resultSink, location);
			}
			default -> {
				// no-op
			}
		}
	}

	private void visitAsmAnnotationDescriptor(@Nonnull String descriptor,
	                                          @Nonnull ResultSink resultSink,
	                                          @Nonnull PathNode<?> location) {
		String type = getInternalName(descriptor);
		if (isClassRefMatch(type))
			resultSink.accept(location, cref(type));
	}

	private void visitDexType(@Nonnull me.darknet.dex.tree.type.Type type,
	                          @Nonnull ResultSink resultSink,
	                          @Nonnull PathNode<?> location) {
		switch (type) {
			case MethodType methodType -> visitDexMethodType(methodType, resultSink, location);
			case ClassType classType -> visitDexClassType(classType, resultSink, location);
		}
	}

	private void visitDexMethodType(@Nonnull MethodType methodType,
	                                @Nonnull ResultSink resultSink,
	                                @Nonnull PathNode<?> location) {
		visitDexClassType(methodType.returnType(), resultSink, location);
		for (ClassType parameterType : methodType.parameterTypes())
			visitDexClassType(parameterType, resultSink, location);
	}

	private void visitFirstDexMethodTypeMatch(@Nonnull MethodType methodType,
	                                          @Nonnull ResultSink resultSink,
	                                          @Nonnull PathNode<?> location) {
		String returnType = getDexInternalName(methodType.returnType());
		if (isClassRefMatch(returnType)) {
			resultSink.accept(location, cref(returnType));
			return;
		}

		for (ClassType parameterType : methodType.parameterTypes()) {
			String argumentType = getDexInternalName(parameterType);
			if (isClassRefMatch(argumentType)) {
				resultSink.accept(location, cref(argumentType));
				return;
			}
		}
	}

	private void visitDexClassType(@Nonnull ClassType type,
	                               @Nonnull ResultSink resultSink,
	                               @Nonnull PathNode<?> location) {
		String internalName = getDexInternalName(type);
		if (isClassRefMatch(internalName))
			resultSink.accept(location, cref(internalName));
	}

	private void visitDexHandle(@Nonnull me.darknet.dex.tree.definitions.constant.Handle handle,
	                            @Nonnull ResultSink resultSink,
	                            @Nonnull PathNode<?> location) {
		String owner = handle.owner().internalName();
		String desc = handle.type().descriptor();
		if (isMemberRefMatch(owner, handle.name(), desc))
			resultSink.accept(location, mref(owner, handle.name(), desc));

		visitDexType(handle.type(), resultSink, location);
	}

	@Nonnull
	private static String getInternalName(@Nonnull String classDesc) {
		return Type.getType(classDesc).getInternalName();
	}

	@Nullable
	private static String getDexInternalName(@Nonnull me.darknet.dex.tree.type.Type type) {
		if (type instanceof ReferenceType referenceType)
			return referenceType.internalName();
		return null;
	}

	@Nonnull
	private static ClassReference cref(@Nonnull String name) {
		return new ClassReference(name);
	}

	@Nonnull
	private static MemberReference mref(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return new MemberReference(owner, name, desc);
	}

	@Nonnull
	private static PathNode<?> findAnnotationPath(@Nonnull PathNode<?> currentLocation, boolean visible, @Nonnull String descriptor) {
		if (currentLocation.getValue() instanceof Annotated annotated) {
			AnnotationInfo annotationInfo = findAnnotation(annotated, visible, descriptor);
			return switch (currentLocation) {
				case ClassPathNode classPath -> classPath.child(annotationInfo);
				case ClassMemberPathNode memberPath -> memberPath.childAnnotation(annotationInfo);
				case AnnotationPathNode annotationPath -> annotationPath.child(annotationInfo);
				default -> currentLocation;
			};
		}
		return currentLocation;
	}

	@Nonnull
	private static AnnotationInfo findAnnotation(@Nonnull Annotated annotated, boolean visible, @Nonnull String descriptor) {
		return annotated.getAnnotations().stream()
				.filter(ai -> ai.getDescriptor().equals(descriptor))
				.findFirst()
				.orElseGet(() -> new BasicAnnotationInfo(visible, descriptor));
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

	@Nonnull
	@Override
	public AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate) {
		return (resultSink, classPath, classInfo) -> {
			if (delegate != null)
				delegate.visit(resultSink, classPath, classInfo);
			DexTreeWalker.accept(classInfo.getBackingDefinition(), new DexReferenceClassVisitor(resultSink, classPath, classInfo));
		};
	}

	/**
	 * Visits references in dex classes.
	 */
	private class DexReferenceClassVisitor extends DexClassVisitor {
		private final Logger logger = Logging.get(DexReferenceClassVisitor.class);
		private final ResultSink resultSink;
		private final ClassPathNode classPath;
		private final AndroidClassInfo classInfo;

		public DexReferenceClassVisitor(@Nonnull ResultSink resultSink,
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
			if (fieldMember != null) {
				ClassMemberPathNode memberPath = classPath.child(fieldMember);

				visitDexClassType(field.getType(), resultSink, memberPath);

				return new DexReferenceFieldVisitor(fv, fieldMember, resultSink, classPath);
			}

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
			if (methodMember != null) {
				ClassMemberPathNode memberPath = classPath.child(methodMember);

				for (String exception : method.getThrownTypes()) {
					if (isClassRefMatch(exception))
						resultSink.accept(memberPath.childThrows(exception), cref(exception));
				}

				visitFirstDexMethodTypeMatch(method.getType(), resultSink, memberPath);
				return new DexReferenceMethodVisitor(mv, methodMember, resultSink, classPath);
			}

			logger.error("Failed to lookup method for query: {}.{} {}", classInfo.getName(), name, desc);
			return mv;
		}

		@Nullable
		@Override
		public DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			String annotationType = part.type().internalName();
			if (isClassRefMatch(annotationType))
				resultSink.accept(classPath, cref(annotationType));

			AnnotationInfo annotationInfo = findAnnotation(classInfo, visible, part.type().descriptor());
			return new DexAnnotationReferenceVisitor(av, visible, resultSink,
					classPath.child(annotationInfo));
		}
	}

	/**
	 * Visits references in dex fields.
	 */
	private class DexReferenceFieldVisitor extends DexFieldVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final FieldMember fieldMember;

		public DexReferenceFieldVisitor(@Nullable DexFieldVisitor delegate,
		                                @Nonnull FieldMember fieldMember,
		                                @Nonnull ResultSink resultSink,
		                                @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.fieldMember = fieldMember;
			this.memberPath = classLocation.child(fieldMember);
		}

		@Nullable
		@Override
		public DexConstantVisitor visitStaticValue(@Nonnull Constant value) {
			DexConstantVisitor cv = super.visitStaticValue(value);
			return new DexConstantReferenceVisitor(cv, resultSink, memberPath);
		}

		@Nullable
		@Override
		public DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			String annotationType = part.type().internalName();
			if (isClassRefMatch(annotationType))
				resultSink.accept(memberPath, cref(annotationType));

			AnnotationInfo annotationInfo = findAnnotation(fieldMember, visible, part.type().descriptor());
			return new DexAnnotationReferenceVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}
	}

	/**
	 * Visits references in dex methods.
	 */
	private class DexReferenceMethodVisitor extends DexMethodVisitor {
		private final ResultSink resultSink;
		private final ClassMemberPathNode memberPath;
		private final MethodMember methodMember;
		private final String ownerType;

		public DexReferenceMethodVisitor(@Nullable DexMethodVisitor delegate,
		                                 @Nonnull MethodMember methodMember,
		                                 @Nonnull ResultSink resultSink,
		                                 @Nonnull ClassPathNode classLocation) {
			super(delegate);
			this.resultSink = resultSink;
			this.methodMember = methodMember;
			this.memberPath = classLocation.child(methodMember);
			this.ownerType = classLocation.getValue().getName();
		}

		@Nullable
		@Override
		public DexAnnotationVisitor visitAnnotation(@Nonnull Annotation annotation) {
			DexAnnotationVisitor av = super.visitAnnotation(annotation);

			boolean visible = isNonZero(annotation.visibility());
			AnnotationPart part = annotation.annotation();
			String annotationType = part.type().internalName();
			if (isClassRefMatch(annotationType))
				resultSink.accept(memberPath, cref(annotationType));

			AnnotationInfo annotationInfo = findAnnotation(methodMember, visible, part.type().descriptor());
			return new DexAnnotationReferenceVisitor(av, visible, resultSink,
					memberPath.childAnnotation(annotationInfo));
		}

		@Nonnull
		@Override
		public DexCodeVisitor visitCode(@Nonnull Code code) {
			DexCodeVisitor cv = super.visitCode(code);
			return new DexCodeVisitor(cv) {
				private int insnIndex;

				@Override
				public void visitInstruction(@Nonnull Instruction instruction) {
					super.visitInstruction(instruction);
					insnIndex++;
				}

				@Override
				public void visitInvokeInstruction(@Nonnull InvokeInstruction instruction) {
					PathNode<?> instructionPath = memberPath.childInsn(instruction, insnIndex);
					String owner = instruction.owner().internalName();
					String desc = instruction.type().descriptor();
					if (isMemberRefMatch(owner, instruction.name(), desc))
						resultSink.accept(instructionPath, mref(owner, instruction.name(), desc));

					visitDexMethodType(instruction.type(), resultSink, instructionPath);
					super.visitInvokeInstruction(instruction);
				}

				@Override
				public void visitInvokeCustomInstruction(@Nonnull InvokeCustomInstruction instruction) {
					PathNode<?> instructionPath = memberPath.childInsn(instruction, insnIndex);
					visitDexMethodType(instruction.type(), resultSink, instructionPath);
					visitDexHandle(instruction.handle(), resultSink, instructionPath);
					super.visitInvokeCustomInstruction(instruction);
				}

				@Override
				public void visitInstanceFieldInstruction(@Nonnull InstanceFieldInstruction instruction) {
					PathNode<?> instructionPath = memberPath.childInsn(instruction, insnIndex);
					String owner = instruction.owner().internalName();
					String desc = instruction.type().descriptor();
					if (isMemberRefMatch(owner, instruction.name(), desc))
						resultSink.accept(instructionPath, mref(owner, instruction.name(), desc));

					visitDexClassType(instruction.type(), resultSink, instructionPath);
					super.visitInstanceFieldInstruction(instruction);
				}

				@Override
				public void visitStaticFieldInstruction(@Nonnull StaticFieldInstruction instruction) {
					PathNode<?> instructionPath = memberPath.childInsn(instruction, insnIndex);
					String owner = instruction.owner().internalName();
					String desc = instruction.type().descriptor();
					if (isMemberRefMatch(owner, instruction.name(), desc))
						resultSink.accept(instructionPath, mref(owner, instruction.name(), desc));

					visitDexClassType(instruction.type(), resultSink, instructionPath);
					super.visitStaticFieldInstruction(instruction);
				}

				@Override
				public void visitCheckCastInstruction(@Nonnull CheckCastInstruction instruction) {
					visitDexClassType(instruction.type(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitCheckCastInstruction(instruction);
				}

				@Override
				public void visitInstanceOfInstruction(@Nonnull InstanceOfInstruction instruction) {
					visitDexClassType(instruction.type(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitInstanceOfInstruction(instruction);
				}

				@Override
				public void visitConstTypeInstruction(@Nonnull ConstTypeInstruction instruction) {
					visitDexClassType(instruction.type(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitConstTypeInstruction(instruction);
				}

				@Override
				public void visitConstMethodTypeInstruction(@Nonnull ConstMethodTypeInstruction instruction) {
					visitDexMethodType(instruction.type(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitConstMethodTypeInstruction(instruction);
				}

				@Override
				public void visitConstMethodHandleInstruction(@Nonnull ConstMethodHandleInstruction instruction) {
					visitDexHandle(instruction.handle(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitConstMethodHandleInstruction(instruction);
				}

				@Override
				public void visitNewInstanceInstruction(@Nonnull NewInstanceInstruction instruction) {
					visitDexClassType(instruction.type(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitNewInstanceInstruction(instruction);
				}

				@Override
				public void visitNewArrayInstruction(@Nonnull NewArrayInstruction instruction) {
					visitDexClassType(instruction.componentType(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitNewArrayInstruction(instruction);
				}

				@Override
				public void visitFilledNewArrayInstruction(@Nonnull FilledNewArrayInstruction instruction) {
					visitDexClassType(instruction.componentType(), resultSink, memberPath.childInsn(instruction, insnIndex));
					super.visitFilledNewArrayInstruction(instruction);
				}

				@Nonnull
				@Override
				public DexConstantVisitor visitBootstrapArgument(@Nonnull InvokeCustomInstruction instruction,
				                                                 int index,
				                                                 @Nonnull Constant argument) {
					DexConstantVisitor dcv = super.visitBootstrapArgument(instruction, index, argument);
					return new DexConstantReferenceVisitor(dcv, resultSink, memberPath.childInsn(instruction, insnIndex));
				}

				@Override
				public void visitTryCatchHandler(@Nonnull TryCatch tryCatch, @Nonnull Handler handler) {
					var exceptionType = handler.exceptionType();
					if (exceptionType != null) {
						String type = exceptionType.internalName();
						if (isClassRefMatch(type))
							resultSink.accept(memberPath.childCatch(type), cref(type));
					}
					super.visitTryCatchHandler(tryCatch, handler);
				}

				@Override
				public void visitLocalVariable(@Nonnull DebugInformation.LocalVariable localVariable) {
					String name = localVariable.name();
					String desc = localVariable.type().descriptor();
					String type = getDexInternalName(localVariable.type());

					if (type == null) {
						super.visitLocalVariable(localVariable);
						return;
					}

					// Skip 'this' variables. Nobody cares that virtual methods have a type reference to themselves...
					if ("this".equals(name) && ownerType.equals(type)) {
						super.visitLocalVariable(localVariable);
						return;
					}

					if (isClassRefMatch(type)) {
						LocalVariable variable = new BasicLocalVariable(localVariable.register(), name, desc, localVariable.signature());
						resultSink.accept(memberPath.childVariable(variable), cref(type));
					}

					super.visitLocalVariable(localVariable);
				}
			};
		}
	}

	/**
	 * Visits references in dex annotations.
	 */
	private class DexAnnotationReferenceVisitor extends DexAnnotationVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentAnnoLocation;
		private final boolean visible;

		public DexAnnotationReferenceVisitor(@Nullable DexAnnotationVisitor delegate,
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
			return new DexConstantReferenceVisitor(dcv, resultSink, currentAnnoLocation);
		}
	}

	/**
	 * Visits references in dex constants.
	 */
	private class DexConstantReferenceVisitor extends DexConstantVisitor {
		private final ResultSink resultSink;
		private final PathNode<?> currentLocation;

		public DexConstantReferenceVisitor(@Nullable DexConstantVisitor delegate,
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
			String annotationType = constant.annotation().type().internalName();
			if (isClassRefMatch(annotationType))
				resultSink.accept(currentLocation, cref(annotationType));

			return new DexAnnotationReferenceVisitor(av, visible, resultSink,
					findAnnotationPath(currentLocation, visible, descriptor));
		}

		@Nullable
		@Override
		public DexConstantVisitor visitArrayConstant(@Nonnull ArrayConstant constant) {
			DexConstantVisitor cv = super.visitArrayConstant(constant);
			return new DexConstantReferenceVisitor(cv, resultSink, currentLocation);
		}

		@Override
		public void visitEnumConstant(@Nonnull EnumConstant constant) {
			super.visitEnumConstant(constant);

			String owner = constant.owner().internalName();
			String name = constant.field().name();
			String desc = constant.field().descriptor();
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(currentLocation, mref(owner, name, desc));
		}

		@Override
		public void visitHandleConstant(@Nonnull HandleConstant constant) {
			super.visitHandleConstant(constant);
			visitDexHandle(constant.handle(), resultSink, currentLocation);
		}

		@Override
		public void visitMemberConstant(@Nonnull MemberConstant constant) {
			super.visitMemberConstant(constant);

			String owner = constant.owner().internalName();
			String name = constant.member().name();
			String desc = constant.member().descriptor();
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(currentLocation, mref(owner, name, desc));
		}

		@Override
		public void visitTypeConstant(@Nonnull TypeConstant constant) {
			super.visitTypeConstant(constant);
			visitDexType(constant.type(), resultSink, currentLocation);
		}
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
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			FieldVisitor fv = super.visitField(access, name, desc, signature, value);

			FieldMember fieldMember = classInfo.getDeclaredField(name, desc);
			if (fieldMember != null) {
				ClassMemberPathNode memberPath = classPath.child(fieldMember);
				visitAsmType(Type.getType(desc), resultSink, memberPath);

				// Visit field
				return new AsmReferenceFieldVisitor(fv, resultSink, memberPath);
			}

			logger.error("Failed to lookup field for query: {}.{}{}", classInfo.getName(), name, desc);
			return fv;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

			MethodMember methodMember = classInfo.getDeclaredMethod(name, desc);
			if (methodMember != null) {
				ClassMemberPathNode memberPath = classPath.child(methodMember);

				if (exceptions != null)
					for (String exception : exceptions)
						if (isClassRefMatch(exception))
							resultSink.accept(memberPath.childThrows(exception), cref(exception));

				visitFirstAsmMethodTypeMatch(Type.getMethodType(desc), resultSink, memberPath);

				// Visit method
				return new AsmReferenceMethodVisitor(mv, methodMember, resultSink, classPath);
			}

			logger.error("Failed to lookup method for query: {}.{}{}", classInfo.getName(), name, desc);
			return mv;
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
			TypeInsnNode insn = new TypeInsnNode(opcode, type);
			visitAsmType(Type.getObjectType(type), resultSink, memberPath.childInsn(insn, index));
			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			FieldInsnNode insn = new FieldInsnNode(opcode, owner, name, desc);
			PathNode<?> instructionPath = memberPath.childInsn(insn, index);
			visitAsmMemberReference(owner, name, desc, resultSink, instructionPath);
			visitAsmType(Type.getType(desc), resultSink, instructionPath);

			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			MethodInsnNode insn = new MethodInsnNode(opcode, owner, name, desc, isInterface);
			PathNode<?> instructionPath = memberPath.childInsn(insn, index);
			visitAsmMemberReference(owner, name, desc, resultSink, instructionPath);
			visitAsmMethodType(Type.getMethodType(desc), resultSink, instructionPath);

			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsmHandle, Object... bsmArgs) {
			InvokeDynamicInsnNode insn = new InvokeDynamicInsnNode(name, desc, bsmHandle, bsmArgs);
			PathNode<?> instructionPath = memberPath.childInsn(insn, index);
			visitAsmMemberReference(ownerType, name, desc, resultSink, instructionPath);
			visitAsmMethodType(Type.getMethodType(desc), resultSink, instructionPath);
			visitAsmBootstrap(bsmHandle, bsmArgs, resultSink, instructionPath);

			super.visitInvokeDynamicInsn(name, desc, bsmHandle, bsmArgs);
		}

		@Override
		public void visitLdcInsn(Object value) {
			LdcInsnNode insn = new LdcInsnNode(value);
			visitAsmConstant(insn.cst, resultSink, memberPath.childInsn(insn, index));

			super.visitLdcInsn(value);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int numDimensions) {
			if (Types.isValidDesc(desc))
				visitAsmType(Type.getType(desc), resultSink,
						memberPath.childInsn(new MultiANewArrayInsnNode(desc, numDimensions), index));
			super.visitMultiANewArrayInsn(desc, numDimensions);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			if (isClassRefMatch(type))
				resultSink.accept(memberPath.childCatch(type), cref(type));
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
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
			AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
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
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
			return new AnnotationReferenceVisitor(av, visible, resultSink, memberPath);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			visitAsmAnnotationDescriptor(desc, resultSink, memberPath);
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
			visitAsmAnnotationDescriptor(descriptor, resultSink, currentAnnoLocation);
			return new AnnotationReferenceVisitor(av, visible, resultSink,
					findAnnotationPath(currentAnnoLocation, visible, descriptor));
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
