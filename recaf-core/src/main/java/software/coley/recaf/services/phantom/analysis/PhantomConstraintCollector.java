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
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;

/**
 * Collects raw phantom constraints from JVM bytecode.
 *
 * @author Matt Coley
 */
public class PhantomConstraintCollector {
	private final PhantomGenerationContext context;
	private final PhantomMethodConstraintAnalysis methodSubtypeAnalyzer;

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
		info.getClassReader().accept(new CollectionVisitor(), ClassReader.SKIP_FRAMES);

		ClassNode node = new ClassNode();
		info.getClassReader().accept(node, ClassReader.SKIP_FRAMES);
		for (MethodNode method : node.methods)
			methodSubtypeAnalyzer.collect(node.name, method.access, method);
	}

	@Nullable
	private PhantomClassConstraint constraint(@Nullable String internalName) {
		if (internalName == null)
			return null;
		return context.getOrCreateConstraint(internalName);
	}

	@Nullable
	private PhantomClassConstraint collectAnnotationDescriptor(@Nonnull String descriptor, boolean visible) {
		context.collectDescriptor(descriptor);
		PhantomClassConstraint constraint = constraint(Type.getType(descriptor).getInternalName());
		if (constraint != null)
			constraint.markAnnotation(visible);
		return constraint;
	}

	private void collectConstant(Object value) {
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

	private void collectHandle(@Nonnull Handle handle) {
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
			case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKESPECIAL -> {
				if (ownerConstraint != null) {
					ownerConstraint.markClass();
					ownerConstraint.addMethod(handle.getName(), handle.getDesc(), false);
				}
				context.collectMethodDescriptor(handle.getDesc());
			}
			default -> {
				// no-op
			}
		}
	}

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

	private AnnotationVisitor annotationCollector(@Nullable AnnotationVisitor delegate,
	                                              @Nullable PhantomClassConstraint annotationConstraint) {
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
				if (annotationConstraint != null && name != null)
					annotationConstraint.addAnnotationElement(name, descriptor);
				super.visitEnum(name, descriptor, value);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String descriptor) {
				PhantomClassConstraint nestedAnnotationConstraint = collectAnnotationDescriptor(descriptor, false);
				if (annotationConstraint != null && name != null)
					annotationConstraint.addAnnotationElement(name, descriptor);
				return annotationCollector(super.visitAnnotation(name, descriptor), nestedAnnotationConstraint);
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
						if (componentDescriptor == null)
							componentDescriptor = descriptor;
						super.visitEnum(ignoredName, descriptor, value);
					}

					@Override
					public AnnotationVisitor visitAnnotation(String ignoredName, String descriptor) {
						PhantomClassConstraint nestedAnnotationConstraint = collectAnnotationDescriptor(descriptor, false);
						if (componentDescriptor == null)
							componentDescriptor = descriptor;
						return annotationCollector(super.visitAnnotation(ignoredName, descriptor), nestedAnnotationConstraint);
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

	private class CollectionVisitor extends ClassVisitor {
		protected CollectionVisitor() {
			super(RecafConstants.getAsmVersion());
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			PhantomClassConstraint superConstraint = constraint(superName);
			if (superConstraint != null)
				superConstraint.markClass();
			if (interfaces != null) {
				for (String interfaceName : interfaces) {
					PhantomClassConstraint interfaceConstraint = constraint(interfaceName);
					if (interfaceConstraint != null)
						interfaceConstraint.markInterface();
				}
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return annotationCollector(super.visitAnnotation(descriptor, visible),
					collectAnnotationDescriptor(descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
					collectAnnotationDescriptor(descriptor, visible));
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			context.collectDescriptor(descriptor);
			collectConstant(value);
			return new FieldVisitor(RecafConstants.getAsmVersion(), super.visitField(access, name, descriptor, signature, value)) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return annotationCollector(super.visitAnnotation(descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			context.collectMethodDescriptor(descriptor);
			if (exceptions != null)
				for (String exception : exceptions)
					context.collectInternalName(exception);

			return new MethodVisitor(RecafConstants.getAsmVersion(), super.visitMethod(access, name, descriptor, signature, exceptions)) {
				@Override
				public AnnotationVisitor visitAnnotationDefault() {
					return annotationCollector(super.visitAnnotationDefault(), null);
				}

				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return annotationCollector(super.visitAnnotation(descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
					return annotationCollector(super.visitParameterAnnotation(parameter, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitInsnAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
					return annotationCollector(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
							collectAnnotationDescriptor(descriptor, visible));
				}

				@Override
				public void visitTypeInsn(int opcode, String type) {
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
					PhantomClassConstraint ownerConstraint = constraint(owner);
					if (ownerConstraint != null) {
						if (isInterface)
							ownerConstraint.markInterface();
						else
							ownerConstraint.markClass();
						if ("<init>".equals(name))
							ownerConstraint.markClass();
						ownerConstraint.addMethod(name, descriptor, opcode == Opcodes.INVOKESTATIC);
					}
					context.collectMethodDescriptor(descriptor);
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
				}

				@Override
				public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
				                                   Object... bootstrapMethodArguments) {
					context.collectMethodDescriptor(descriptor);
					collectHandle(bootstrapMethodHandle);
					for (Object bootstrapMethodArgument : bootstrapMethodArguments)
						collectConstant(bootstrapMethodArgument);
					super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
				}

				@Override
				public void visitLdcInsn(Object value) {
					collectConstant(value);
					super.visitLdcInsn(value);
				}

				@Override
				public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
					context.collectDescriptor(descriptor);
					super.visitMultiANewArrayInsn(descriptor, numDimensions);
				}

				@Override
				public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
					context.collectInternalName(type);
					super.visitTryCatchBlock(start, end, handler, type);
				}
			};
		}
	}
}
