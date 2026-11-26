package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Collects raw phantom constraints from JVM bytecode.
 *
 * @author Matt Coley
 */
public class PhantomConstraintCollector {
	private final PhantomGenerationContext context;
	private final PhantomMethodConstraintAnalysis methodSubtypeAnalyzer;
	private final Set<String> visitedKnownClasses = new HashSet<>();
	private final Set<String> queuedKnownClasses = new HashSet<>();
	private final Deque<JvmClassInfo> pendingKnownClasses = new ArrayDeque<>();

	/**
	 * @param context
	 * 		Phantom analysis context.
	 */
	public PhantomConstraintCollector(@Nonnull PhantomGenerationContext context) {
		this.context = context;
		methodSubtypeAnalyzer = new PhantomMethodConstraintAnalysis(context);
	}

	/**
	 * Collects constraints implied by a class.
	 *
	 * @param info
	 * 		Class to inspect.
	 */
	public void collect(@Nonnull JvmClassInfo info) {
		collectClass(info, true);
		while (!pendingKnownClasses.isEmpty())
			collectClass(pendingKnownClasses.removeFirst(), false);
	}

	private void collectClass(@Nonnull JvmClassInfo info, boolean followReferences) {
		// Skip if the class has already been visited.
		// This is only relevant for known classes, as phantom classes are only visited once.
		if (!followReferences && !visitedKnownClasses.add(info.getName()))
			return;

		// Collect constraints from the class itself.
		int flags = ClassReader.SKIP_FRAMES;
		if (!followReferences)
			flags |= ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG;
		info.getClassReader().accept(new CollectionVisitor(followReferences, followReferences), flags);

		// A second narrow pass that retains debug information collects constraints without following references.
		if (!followReferences)
			info.getClassReader().accept(new CollectionVisitor(false, false), ClassReader.SKIP_FRAMES);

		// Collect constraints from the methods of the class, if requested.
		if (followReferences) {
			ClassNode node = new ClassNode();
			info.getClassReader().accept(node, ClassReader.SKIP_FRAMES);
			for (MethodNode method : node.methods)
				methodSubtypeAnalyzer.collect(node.name, method.access, method);
		}
	}

	private void queueKnownType(@Nullable String internalName) {
		// Skip if the type is null, already visited, or already waiting for inspection.
		if (internalName == null || visitedKnownClasses.contains(internalName) || !queuedKnownClasses.add(internalName))
			return;

		// Add to the queue if the type is known.
		ClassInfo info = context.getLookup().getKnownClassInfo(internalName);
		if (info instanceof JvmClassInfo jvmInfo)
			pendingKnownClasses.addLast(jvmInfo);
	}

	private void queueKnownTypes(@Nonnull Type type) {
		// For any referenced types, queue them for inspection if they are known.
		switch (type.getSort()) {
			case Type.ARRAY -> queueKnownTypes(type.getElementType());
			case Type.OBJECT -> queueKnownType(type.getInternalName());
			case Type.METHOD -> {
				queueKnownTypes(type.getReturnType());
				for (Type argument : type.getArgumentTypes())
					queueKnownTypes(argument);
			}
			default -> {}
		}
	}

	private void collectSignature(@Nullable String signature) {
		// Skip if the signature is null or empty.
		if (signature == null || signature.isEmpty())
			return;

		// Collect generic parameter counts from the signature.
		try {
			new SignatureReader(signature).accept(new GenericSignatureCollector());
		} catch (RuntimeException ignored) {
			// Invalid signatures are handled as erased declarations elsewhere.
		}
	}

	@Nullable
	private PhantomClassConstraint constraint(@Nullable String internalName) {
		if (internalName == null)
			return null;
		return context.getOrCreateConstraint(internalName);
	}

	/**
	 * Collects constraints from the known parents of a type.
	 *
	 * @param internalName
	 * 		Type whose parents should be inspected.
	 * @param visited
	 * 		Types already inspected while walking the hierarchy.
	 */
	private void collectHierarchy(@Nullable String internalName, @Nonnull Set<String> visited) {
		// Skip if already visited.
		if (internalName == null || !visited.add(internalName))
			return;

		// Skip if the type is known, as we only want to collect phantom constraints.
		ClassInfo info = context.getLookup().getKnownClassInfo(internalName);
		if (info == null)
			return;

		// Mark the supertype and collect upward.
		PhantomClassConstraint superConstraint = constraint(info.getSuperName());
		if (superConstraint != null)
			superConstraint.markClass();
		collectHierarchy(info.getSuperName(), visited);

		// Mark all interfaces and collect upward.
		for (String interfaceName : info.getInterfaces()) {
			PhantomClassConstraint interfaceConstraint = constraint(interfaceName);
			if (interfaceConstraint != null)
				interfaceConstraint.markInterface();
			collectHierarchy(interfaceName, visited);
		}
	}

	/**
	 * Collects constraints from a type used in a {@code throws} clause.
	 *
	 * @param internalName
	 * 		Type used in a {@code throws} clause.
	 * @param followReferences
	 *        {@code true} to queue known types for inspection,
	 *        {@code false} to only collect phantom constraints.
	 */
	private void collectExceptionType(@Nullable String internalName, boolean followReferences) {
		if (internalName == null)
			return;

		if (followReferences)
			queueKnownType(internalName);

		context.collectInternalName(internalName);

		PhantomClassConstraint exceptionConstraint = constraint(internalName);
		if (exceptionConstraint != null) {
			exceptionConstraint.markClass();
			exceptionConstraint.addHardSupertype("java/lang/Throwable");
		}
	}

	/**
	 * @param descriptor
	 * 		Annotation descriptor.
	 * @param visible
	 * 		Whether the annotation is visible at runtime.
	 * @param followReferences
	 *        {@code true} to queue known types for inspection,
	 *        {@code false} to only collect phantom constraints.
	 *
	 * @return Constraint for the annotation type, or {@code null} if the type is known.
	 */
	@Nullable
	private PhantomClassConstraint collectAnnotationDescriptor(@Nonnull String descriptor, boolean visible, boolean followReferences) {
		if (followReferences)
			queueKnownType(Type.getType(descriptor).getInternalName());

		context.collectDescriptor(descriptor);

		PhantomClassConstraint constraint = constraint(Type.getType(descriptor).getInternalName());
		if (constraint != null)
			constraint.markAnnotation(visible);
		return constraint;
	}

	/**
	 * @param value
	 * 		Constant value.
	 */
	private void collectConstant(@Nullable Object value) {
		switch (value) {
			case null -> {
				// no-op
			}
			case Type type -> context.collectType(type);
			case Handle handle -> collectHandle(handle);
			case ConstantDynamic dynamic -> {
				context.collectDescriptor(dynamic.getDescriptor());
				collectHandle(dynamic.getBootstrapMethod());
				for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++)
					collectConstant(dynamic.getBootstrapMethodArgument(i));
			}
			default -> {
				// Primitive/string constants do not contribute missing type constraints.
			}
		}
	}

	/**
	 * @param handle
	 * 		Method handle.
	 */
	private void collectHandle(@Nonnull Handle handle) {
		// Known owners can carry the only copy of a required hierarchy or annotation declaration.
		queueKnownType(handle.getOwner());
		PhantomClassConstraint ownerConstraint = constraint(handle.getOwner());
		switch (handle.getTag()) {
			case Opcodes.H_GETFIELD, Opcodes.H_PUTFIELD -> {
				if (ownerConstraint != null) {
					ownerConstraint.markClass();
					ownerConstraint.addField(handle.getName(), handle.getDesc(), false);
				}
				context.collectDescriptor(handle.getDesc());
			}
			case Opcodes.H_GETSTATIC, Opcodes.H_PUTSTATIC -> {
				if (ownerConstraint != null)
					ownerConstraint.addField(handle.getName(), handle.getDesc(), true);
				context.collectDescriptor(handle.getDesc());
			}
			case Opcodes.H_INVOKEINTERFACE -> {
				if (ownerConstraint != null) {
					ownerConstraint.markInterface();
					if (!isFinalObjectInstanceMethod(handle.getName(), handle.getDesc()))
						ownerConstraint.addMethod(handle.getName(), handle.getDesc(), false);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			case Opcodes.H_INVOKESTATIC -> {
				if (ownerConstraint != null) {
					if (handle.isInterface())
						ownerConstraint.markInterface();
					else
						ownerConstraint.markClass();
					if (!isFinalObjectInstanceMethod(handle.getName(), handle.getDesc()))
						ownerConstraint.addMethod(handle.getName(), handle.getDesc(), true);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			case Opcodes.H_NEWINVOKESPECIAL -> {
				if (ownerConstraint != null) {
					ownerConstraint.markClass();
					ownerConstraint.addMethod(handle.getName(), handle.getDesc(), false);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			case Opcodes.H_INVOKEVIRTUAL -> {
				if (ownerConstraint != null) {
					ownerConstraint.markClass();
					if (!isFinalObjectInstanceMethod(handle.getName(), handle.getDesc()))
						ownerConstraint.addMethod(handle.getName(), handle.getDesc(), false);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			case Opcodes.H_INVOKESPECIAL -> {
				if (ownerConstraint != null) {
					if (handle.isInterface())
						ownerConstraint.markInterface();
					else
						ownerConstraint.markClass();
					if (!isFinalObjectInstanceMethod(handle.getName(), handle.getDesc()))
						ownerConstraint.addMethod(handle.getName(), handle.getDesc(), false);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			default -> {
				// no-op
			}
		}
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return {@code true} when the method is a final public instance method declared by {@link Object}.
	 *			Such a reference resolves to {@link Object}, so it must not become a declaration requirement on the owner.
	 */
	private static boolean isFinalObjectInstanceMethod(@Nonnull String name, @Nonnull String descriptor) {
		return switch (name) {
			case "getClass" -> "()Ljava/lang/Class;".equals(descriptor);
			case "notify", "notifyAll" -> "()V".equals(descriptor);
			case "wait" -> "()V".equals(descriptor) || "(J)V".equals(descriptor) || "(JI)V".equals(descriptor);
			default -> false;
		};
	}

	/**
	 * @param value
	 * 		Annotation element value.
	 *
	 * @return Descriptor of the annotation element value,
	 * or {@code null} if the value is not a primitive, string, or type.
	 */
	@Nullable
	private static String annotationValueDescriptor(@Nonnull Object value) {
		return switch (value) {
			case Boolean ignored -> "Z";
			case Byte ignored -> "B";
			case Character ignored -> "C";
			case Short ignored -> "S";
			case Integer ignored -> "I";
			case Long ignored -> "J";
			case Float ignored -> "F";
			case Double ignored -> "D";
			case String ignored -> "Ljava/lang/String;";
			case Type ignored -> "Ljava/lang/Class;";
			default -> null;
		};
	}

	/**
	 * @param delegate
	 * 		Delegate visitor to forward to.
	 * @param annotationConstraint
	 * 		Constraint for the annotation type, or {@code null} if the type is known.
	 * @param followReferences
	 *        {@code true} to queue known types for inspection,
	 *        {@code false} to only collect phantom constraints.
	 *
	 * @return Visitor that collects constraints from the annotation.
	 */
	@Nonnull
	private AnnotationVisitor annotationCollector(@Nullable AnnotationVisitor delegate,
	                                              @Nullable PhantomClassConstraint annotationConstraint,
	                                              boolean followReferences) {
		return new AnnotationVisitor(RecafConstants.getAsmVersion(), delegate) {
			@Override
			public void visit(String name, Object value) {
				collectConstant(value);
				if (annotationConstraint != null && name != null) {
					String descriptor = annotationValueDescriptor(value);
					if (descriptor != null)
						annotationConstraint.addAnnotationElement(name, descriptor);
				}
				super.visit(name, value);
			}

			@Override
			public void visitEnum(String name, String descriptor, String value) {
				context.collectDescriptor(descriptor);
				PhantomClassConstraint enumConstraint = constraint(Type.getType(descriptor).getInternalName());
				if (enumConstraint != null)
					enumConstraint.addEnumConstant(value);
				if (annotationConstraint != null && name != null)
					annotationConstraint.addAnnotationElement(name, descriptor);
				super.visitEnum(name, descriptor, value);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String descriptor) {
				PhantomClassConstraint nestedAnnotationConstraint = collectAnnotationDescriptor(descriptor, false, followReferences);
				if (annotationConstraint != null && name != null)
					annotationConstraint.addAnnotationElement(name, descriptor);
				return annotationCollector(super.visitAnnotation(name, descriptor), nestedAnnotationConstraint, followReferences);
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				AnnotationVisitor arrayDelegate = super.visitArray(name);
				return new AnnotationVisitor(RecafConstants.getAsmVersion(), arrayDelegate) {
					private String componentDescriptor;

					@Override
					public void visit(String ignoredName, Object value) {
						collectConstant(value);
						if (componentDescriptor == null)
							componentDescriptor = annotationValueDescriptor(value);
						super.visit(ignoredName, value);
					}

					@Override
					public void visitEnum(String ignoredName, String descriptor, String value) {
						context.collectDescriptor(descriptor);
						PhantomClassConstraint enumConstraint = constraint(Type.getType(descriptor).getInternalName());
						if (enumConstraint != null)
							enumConstraint.addEnumConstant(value);
						if (componentDescriptor == null)
							componentDescriptor = descriptor;
						super.visitEnum(ignoredName, descriptor, value);
					}

					@Override
					public AnnotationVisitor visitAnnotation(String ignoredName, String descriptor) {
						PhantomClassConstraint nestedAnnotationConstraint = collectAnnotationDescriptor(descriptor, false, followReferences);
						if (componentDescriptor == null)
							componentDescriptor = descriptor;
						return annotationCollector(super.visitAnnotation(ignoredName, descriptor), nestedAnnotationConstraint, followReferences);
					}

					@Override
					public void visitEnd() {
						// Array-valued annotation elements need their descriptor synthesized from the first observed value.
						if (annotationConstraint != null && name != null && componentDescriptor != null)
							annotationConstraint.addAnnotationElement(name, "[" + componentDescriptor);
						super.visitEnd();
					}
				};
			}
		};
	}

	/**
	 * Visitor that collects constraints from a class.
	 */
	private class CollectionVisitor extends ClassVisitor {
		private final boolean followReferences;
		private final boolean collectCodeReferences;
		private String currentClassName;

		protected CollectionVisitor(boolean followReferences, boolean collectCodeReferences) {
			super(RecafConstants.getAsmVersion());
			this.followReferences = followReferences;
			this.collectCodeReferences = collectCodeReferences;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			currentClassName = name;
			collectSignature(signature);
			queueKnownType(superName);

			// Mark the supertype as a phantom candidate.
			PhantomClassConstraint superConstraint = constraint(superName);
			if (superConstraint != null)
				superConstraint.markClass();

			// Walk the supertype hierarchy to mark any supertypes as phantom candidates.
			Set<String> visited = new HashSet<>();
			collectHierarchy(superName, visited);

			// Mark the interfaces as phantom candidates.
			if (interfaces != null) {
				for (String interfaceName : interfaces) {
					queueKnownType(interfaceName);
					PhantomClassConstraint interfaceConstraint = constraint(interfaceName);
					if (interfaceConstraint != null)
						interfaceConstraint.markInterface();

					// Also walk the interface hierarchy to mark any superinterfaces as phantom candidates.
					collectHierarchy(interfaceName, visited);
				}
			}
		}

		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			queueKnownType(name);
			queueKnownType(outerName);

			// Record inner/outer relationships for both the inner and outer class constraints.
			PhantomClassConstraint innerConstraint = constraint(name);
			PhantomClassConstraint outerConstraint = constraint(outerName);
			if (innerConstraint != null) {
				if ((access & Opcodes.ACC_ANNOTATION) != 0)
					innerConstraint.markAnnotation(false);
				else if ((access & Opcodes.ACC_ENUM) != 0)
					innerConstraint.markEnum();
				else if ((access & Opcodes.ACC_INTERFACE) != 0)
					innerConstraint.markInterface();
				else if (access != 0)
					innerConstraint.markClass();
			}
			if (innerConstraint != null && outerName != null && innerName != null)
				innerConstraint.markInnerClassOf(outerName, innerName, access);
			if (outerConstraint != null && name != null && innerName != null)
				outerConstraint.addDeclaredInner(name, innerName, access);

			super.visitInnerClass(name, outerName, innerName, access);
		}

		@Override
		public void visitOuterClass(String owner, String name, String descriptor) {
			queueKnownType(owner);

			if (descriptor != null)
				context.collectMethodDescriptor(descriptor);

			if (owner != null && currentClassName != null) {
				int separator = currentClassName.lastIndexOf('$');
				if (separator > 0) {
					String innerName = currentClassName.substring(separator + 1);

					// Record inner/outer relationships for both the inner and outer class constraints.
					PhantomClassConstraint innerConstraint = constraint(currentClassName);
					PhantomClassConstraint outerConstraint = constraint(owner);
					if (innerConstraint != null)
						innerConstraint.markInnerClassOf(owner, innerName);
					if (outerConstraint != null)
						outerConstraint.addDeclaredInner(currentClassName, innerName);
				}
			}

			super.visitOuterClass(owner, name, descriptor);
		}

		@Override
		public void visitNestHost(String nestHost) {
			queueKnownType(nestHost);
			context.collectInternalName(nestHost);
			super.visitNestHost(nestHost);
		}

		@Override
		public void visitNestMember(String nestMember) {
			queueKnownType(nestMember);
			context.collectInternalName(nestMember);
			super.visitNestMember(nestMember);
		}

		@Override
		public void visitPermittedSubclass(String permittedSubclass) {
			queueKnownType(permittedSubclass);
			context.collectInternalName(permittedSubclass);
			// A permitted subclass must directly extend or implement the sealed type that permits it.
			PhantomClassConstraint permittedConstraint = constraint(permittedSubclass);
			if (permittedConstraint != null && currentClassName != null)
				permittedConstraint.addRequiredDirectSupertype(currentClassName);
			super.visitPermittedSubclass(permittedSubclass);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return annotationCollector(super.visitAnnotation(descriptor, visible),
					collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
					collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
		}

		@Override
		public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
			collectSignature(signature);
			if (followReferences)
				queueKnownTypes(Type.getType(descriptor));
			context.collectDescriptor(descriptor);
			return new RecordComponentVisitor(RecafConstants.getAsmVersion(), super.visitRecordComponent(name, descriptor, signature)) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return annotationCollector(super.visitAnnotation(descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
				                                             String descriptor, boolean visible) {
					return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}
			};
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			collectSignature(signature);
			if (followReferences)
				queueKnownTypes(Type.getType(descriptor));
			context.collectDescriptor(descriptor);
			collectConstant(value);
			return new FieldVisitor(RecafConstants.getAsmVersion(), super.visitField(access, name, descriptor, signature, value)) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return annotationCollector(super.visitAnnotation(descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			collectSignature(signature);
			if (followReferences)
				queueKnownTypes(Type.getMethodType(descriptor));
			context.collectMethodDescriptor(descriptor);
			if (exceptions != null)
				for (String exception : exceptions)
					collectExceptionType(exception, followReferences);

			return new MethodVisitor(RecafConstants.getAsmVersion(), super.visitMethod(access, name, descriptor, signature, exceptions)) {
				@Override
				public AnnotationVisitor visitAnnotationDefault() {
					return annotationCollector(super.visitAnnotationDefault(), null, followReferences);
				}

				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return annotationCollector(super.visitAnnotation(descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
					return annotationCollector(super.visitParameterAnnotation(parameter, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitInsnAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public void visitLocalVariable(String name, String descriptor, String signature,
				                               Label start, Label end, int index) {
					collectSignature(signature);
					if (followReferences)
						queueKnownTypes(Type.getType(descriptor));
					context.collectDescriptor(descriptor);
					super.visitLocalVariable(name, descriptor, signature, start, end, index);
				}

				@Override
				public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
				                                                      Label[] start, Label[] end, int[] index,
				                                                      String descriptor, boolean visible) {
					return annotationCollector(super.visitLocalVariableAnnotation(typeRef, typePath, start, end,
							index, descriptor, visible), collectAnnotationDescriptor(descriptor, visible, followReferences), followReferences);
				}

				@Override
				public void visitTypeInsn(int opcode, String type) {
					if (!collectCodeReferences) {
						super.visitTypeInsn(opcode, type);
						return;
					}
					queueKnownType(type);
					if (opcode == Opcodes.NEW) {
						PhantomClassConstraint ownerConstraint = constraint(type);
						if (ownerConstraint != null)
							ownerConstraint.markClass();
					} else {
						if (type.indexOf('[') == 0 || type.indexOf(';') > 0)
							context.collectDescriptor(type);
						else
							context.collectInternalName(type);
					}
					super.visitTypeInsn(opcode, type);
				}

				@Override
				public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
					if (!collectCodeReferences) {
						super.visitFieldInsn(opcode, owner, name, descriptor);
						return;
					}
					queueKnownType(owner);
					PhantomClassConstraint ownerConstraint = constraint(owner);
					if (ownerConstraint != null) {
						if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD)
							ownerConstraint.markClass();
						ownerConstraint.addField(name, descriptor,
								opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
					}
					context.collectDescriptor(descriptor);
					super.visitFieldInsn(opcode, owner, name, descriptor);
				}

				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
					if (!collectCodeReferences) {
						super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
						return;
					}
					queueKnownType(owner);
					PhantomClassConstraint ownerConstraint = constraint(owner);
					if (ownerConstraint != null) {
						if (isInterface)
							ownerConstraint.markInterface();
						else
							ownerConstraint.markClass();
						if ("<init>".equals(name))
							ownerConstraint.markClass();
						if (!isFinalObjectInstanceMethod(name, descriptor))
							ownerConstraint.addMethod(name, descriptor, opcode == Opcodes.INVOKESTATIC);
					}
					context.collectMethodDescriptor(descriptor);
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
				}

				@Override
				public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
				                                   Object... bootstrapMethodArguments) {
					if (!collectCodeReferences) {
						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
						return;
					}
					context.collectMethodDescriptor(descriptor);
					collectHandle(bootstrapMethodHandle);
					for (Object bootstrapMethodArgument : bootstrapMethodArguments)
						collectConstant(bootstrapMethodArgument);
					super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
				}

				@Override
				public void visitLdcInsn(Object value) {
					if (!collectCodeReferences) {
						super.visitLdcInsn(value);
						return;
					}
					collectConstant(value);
					super.visitLdcInsn(value);
				}

				@Override
				public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
					if (!collectCodeReferences) {
						super.visitMultiANewArrayInsn(descriptor, numDimensions);
						return;
					}
					context.collectDescriptor(descriptor);
					super.visitMultiANewArrayInsn(descriptor, numDimensions);
				}

				@Override
				public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
					collectExceptionType(type, followReferences);
					super.visitTryCatchBlock(start, end, handler, type);
				}
			};
		}
	}

	/**
	 * Visitor that collects generic parameter counts from a signature.
	 */
	private class GenericSignatureCollector extends SignatureVisitor {
		private String className;
		private int argumentCount;

		private GenericSignatureCollector() {
			super(RecafConstants.getAsmVersion());
		}

		@Override
		public void visitClassType(String name) {
			context.collectInternalName(name);
			className = name;
			argumentCount = 0;
		}

		@Override
		public void visitInnerClassType(String name) {
			if (className != null) {
				className += '$' + name;
				context.collectInternalName(className);
			}
			argumentCount = 0;
		}

		@Override
		public void visitTypeArgument() {
			argumentCount++;
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			argumentCount++;
			return new GenericSignatureCollector();
		}

		@Override
		public void visitEnd() {
			if (className != null && argumentCount > 0) {
				PhantomClassConstraint constraint = context.getOrCreateConstraint(className);
				if (constraint != null)
					constraint.markGenericParameterCount(argumentCount);
			}
			className = null;
			argumentCount = 0;
		}
	}
}
